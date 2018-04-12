package org.liamjd.caisson.webforms

import org.liamjd.caisson.Exceptions.CaissonBindException
import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.convertors.*
import org.liamjd.caisson.models.CaissonMultipartContent
import org.slf4j.LoggerFactory
import spark.Request
import java.lang.reflect.InvocationTargetException
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

typealias RequestParams = Map<String, Array<String>>

class WebForm(sparkRequest: Request, modelClass: KClass<*>) : Form {

	val logger = LoggerFactory.getLogger(WebForm::class.java)

	private val paramsMap: RequestParams
	private val raw: HttpServletRequest?
	private val modelClass: KClass<*>
	private var multiPartUploadNames: List<String>? = null
	private lateinit var modelObject: Any

	constructor(sparkRequest: Request, modelClass: KClass<*>, partNames: List<String>) : this(sparkRequest, modelClass) {
		this.multiPartUploadNames = partNames
	}

	constructor(sparkRequest: Request, modelClass: KClass<*>, partName: String) : this(sparkRequest, modelClass) {
		this.multiPartUploadNames = listOf(partName)
	}

	init {
		this.modelClass = modelClass
		this.paramsMap = sparkRequest.queryMap().toMap()
		this.raw = sparkRequest.raw()
	}

	/**
	 * Generate an object with the given `KClass`, populating all of its primary constructor parameters
	 */
	override fun <T> get(): T? {
		val primaryConstructor = modelClass.primaryConstructor
		val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()

		if (primaryConstructor == null) {
			logger.error("Can't create a model which doesn't have a primary constructor!")
			throw Exception("Can't create a model which doesn't have a primary constructor!")
		}

		val constructorKParams: List<KParameter> = primaryConstructor.parameters
		for (kParam in constructorKParams) {
			logger.info("${modelClass.simpleName} constructor parameter ${kParam.name} value " + paramsMap?.get(kParam.name))

			/* ************************************* GET THE KCLASS OF THE PARAMETER VIA THE JAVA ERASURE ******************/
			// TODO: do I need to consider kParam.isOptional and kParam.type.isMarkedNullable?
			val erasure = kParam.type.jvmErasure
			val requestParamValues = paramsMap.get(kParam.name)
			val inputValue: String
			val inputList: List<String>
			val converter: Converter?

			if (requestParamValues == null) {
				inputValue = ""
				inputList = emptyList()
			} else if (requestParamValues.size == 1) {
				inputValue = requestParamValues[0]
				inputList = emptyList()
			} else {
				inputValue = ""
				inputList = requestParamValues.toList()
			}

			// building the constructorParams map
			// if there is an annotated Converter, use it
			val converterAnnotation = kParam.findAnnotation<CConverter>()
			if (converterAnnotation != null) {
				constructorParams.put(kParam, getAnnotatedConverterValue(converterAnnotation, inputValue))
			} else {
				when (erasure) {
					CaissonMultipartContent::class -> {
						// if CaissonMultipartContent, handle file uploads
						if (raw == null) {
							throw Exception("Caisson cannot parse this CaissonMultipartContent as the raw servlet request was null")
						}
						if (multiPartUploadNames == null) {
							throw Exception("Caisson cannot parse this CaissonMultipartContent the input part names are null")
						}
						logger.info("Extracting file information from Multipart request for ${kParam.name}")
						val multiPartFiles = getMultiPartFile(raw, multiPartUploadNames!!)
						if (multiPartFiles.size > 0 && multiPartFiles.size < 2) {
							constructorParams.put(kParam, multiPartFiles[0])
						} else {
							constructorParams.put(kParam, multiPartFiles)
						}
					}
					List::class -> {
						// if a List<*>, get the type of the list element
						// deal with lists

						logger.info("We have a list. We need to extract the generic type and call the appropriate converter.")
						if (kParam.type.arguments.size > 1) {
							throw Exception("How could a List<*> ever have more than on argument?")
						}
						// Now this starts to get a bit recursive...
						when (kParam.type.arguments.first().type?.jvmErasure) {
							// list of strings for checkboxes, etc
							String::class -> {
								constructorParams.put(kParam,inputList)
							}
						// If it's a list of files, there's nothing we need to do. Our function can return a list of files already
							CaissonMultipartContent::class -> {
								val multiPartFiles = getMultiPartFile(servletRequest = raw!!, partNames = multiPartUploadNames!!)
								constructorParams.put(kParam, multiPartFiles)
							}
							else -> {
								logger.error("I don't know how to process a List of ${kParam.type.arguments.first().type}")
							}
						}


					}
					Enum::class -> {
						// deal with enums?
						logger.error("Caisson cannot yet parse Enums as parameters")
						throw Exception("Caisson cannot yet parse Enums as parameters")
					}
				// if String, Int, Long, Float, Boolean, Double do the simple conversion from the queryParamMap, calling either the internal or the Annotated converter class
					String::class -> {
						constructorParams.put(kParam, inputValue)
					}
					Int::class -> {
						if(!kParam.type.isMarkedNullable && inputValue.isNullOrEmpty()) {
							logger.error("Parameter ${kParam.name} has not been marked nullable but input value is empty")
						}
						converter = DefaultIntConverter()
						constructorParams.put(kParam, getConverterValue(converter, inputValue))
					}
					Long::class -> {
						converter = DefaultLongConverter()
						constructorParams.put(kParam, getConverterValue(converter, inputValue))
					}
					Double::class -> {
						converter = DefaultDoubleConverter()
						constructorParams.put(kParam, getConverterValue(converter, inputValue))
					}
					Float::class -> {
						converter = DefaultFloatConverter()
						constructorParams.put(kParam, getConverterValue(converter, inputValue))
					}
					Boolean::class -> {
						converter = DefaultBooleanConverter()
						constructorParams.put(kParam, getConverterValue(converter, inputValue))
					}
				}

			}

		} // end for loop


		// Finally, construct our model by passing in our constructor parameter map
		primaryConstructor?.let {
			logger.info("Finally construct our object with paramsMap:")
			constructorParams.forEach {
				logger.info("..... ${it.key} -> ${it.value}")
			}
			logger.info("expecting ${primaryConstructor.parameters}")
			try {
				modelObject = it.callBy(constructorParams)
			} catch (ite: InvocationTargetException) {
				logger.error("Invocation Target Exception while building ${modelClass.simpleName}")
				logger.error(ite.targetException.message)
				throw CaissonBindException(ite.targetException.message)
			}
		}

		if (!::modelObject.isInitialized || modelObject == null) {
			return null
		}
		return modelObject as T
	}

	/**
	 * Convert the string input for the current parameter into an object of the correct class
	 * It will use the annotated @CConverter class.
	 */
	@Deprecated("Don't think we want to use this?")
	private fun getConvertedValue(kParameter: KParameter, input: String): Any? {
		val converterAnnotation = kParameter.findAnnotation<CConverter>()
		var finalValue: Any? = null
		if (converterAnnotation != null) {
			val converterClass = Class.forName(converterAnnotation.converterClass.qualifiedName).newInstance().javaClass.kotlin
			if (converterClass != null) {
				val converterFunction = converterClass.declaredMemberFunctions.find { it.name.equals("convert") }
				if (converterFunction != null) {
					logger.info("Using annotated converter ${converterAnnotation} to convert value ${input}")
					finalValue = converterFunction.call(converterClass.createInstance(), input)
				}
			} else {
				logger.error("Could not constructor a converter of class ${converterClass}")
				return null
			}
		} else {
			logger.error("Somehow we're trying to call an annotated Converter on a parameter which hasn't been annotated!")
		}

		return finalValue
	}


	/**
	 * Call the conversion function defined by the Default converter for the given class
	 */
	private fun getConverterValue(converter: Converter, inputValue: String): Any? {
		logger.info("Calling basic converter ${converter} for value ${inputValue}")
		return converter.convert(inputValue)
	}

	/**
	 * Call the conversion function defined by the CConverter provided in the annotation
	 */
	private fun getAnnotatedConverterValue(cConverter: CConverter, inputValue: String): Any? {
		var finalValue: Any? = null

		val converterClass = Class.forName(cConverter.converterClass.qualifiedName).newInstance().javaClass.kotlin
		if (converterClass != null) {
			val converterFunction = converterClass.declaredMemberFunctions.find { it.name.equals("convert") }
			if (converterFunction != null) {
				finalValue = converterFunction.call(converterClass.createInstance(), inputValue)
			}
		}
		return finalValue
	}

	/**
	 * Extract each file and store them in a list of CaissonMultipartContent
	 */
	private fun getMultiPartFile(servletRequest: HttpServletRequest, partNames: List<String>): List<CaissonMultipartContent> {
		val multiPartFormConverter = MultipartUploadConverter()
		val fileList: MutableList<CaissonMultipartContent> = mutableListOf()

		if (servletRequest.getAttribute("org.eclipse.jetty.multipartConfig") == null) {
			val multipartConfigElement = MultipartConfigElement(System.getProperty("java.io.tmpdir"))
			servletRequest.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)
		}

		// it's possible to have mulitple parts with the same name
		// instead, loop round servletRequest.parts and check each name in turn
		for (part in servletRequest.parts) {
			logger.info("Checking part $part to see if it matches one of the part names $partNames")
			if (part.name in partNames) {
				fileList.add(multiPartFormConverter.convert(part))
			}
		}

		return fileList
	}

}