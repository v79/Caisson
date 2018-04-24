package org.liamjd.caisson.webforms

import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.annotations.Compound
import org.liamjd.caisson.convertors.*
import org.liamjd.caisson.exceptions.CaissonBindException
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

class Prefix(val prefix: String?, val separator: String? = ".") {
	constructor() : this("")

	fun isNull(): Boolean = prefix.isNullOrEmpty()
	override fun toString(): String = if(isNull()) { "" } else { prefix + separator }
}

class WebForm(sparkRequest: Request, modelClass: KClass<*>) : Form {

	companion object {
		var counter: Int = 0
	}

	val logger = LoggerFactory.getLogger(WebForm::class.java)

	private val paramsMap: RequestParams
	private val raw: HttpServletRequest?
	private val modelClass: KClass<*>
	private var multiPartUploadNames: List<String>? = null
	private lateinit var modelObject: Any
	private val request: Request
	private var modelPrefix: Prefix = Prefix()

	constructor(sparkRequest: Request, modelClass: KClass<*>, partNames: List<String>) : this(sparkRequest, modelClass) {
		this.multiPartUploadNames = partNames
//		this.modelPrefix = Prefix()
	}

	constructor(sparkRequest: Request, modelClass: KClass<*>, partName: String) : this(sparkRequest, modelClass) {
		this.multiPartUploadNames = listOf(partName)
//		this.modelPrefix = Prefix()
	}

	constructor(sparkRequest: Request, modelClass: KClass<*>, prefix: Prefix = Prefix()): this(sparkRequest, modelClass) {
		this.modelPrefix = prefix
		counter++
		println("--${counter} ---------- a prefix has been provided and set to '${modelPrefix}' for ${modelClass.simpleName}")
	}

	//, modelPrefix: Prefix = Prefix()

	init {
		this.modelClass = modelClass
		this.paramsMap = sparkRequest.queryMap().toMap()
		this.raw = sparkRequest.raw()
		this.request = sparkRequest
	}

	/**
	 * Generate an object with the given `KClass`, populating all of its primary constructor parameters
	 */
	override fun <T> get(): T? {

		println("¬¬¬¬¬¬¬¬¬¬¬¬¬ WebForm ${this} with prefix ${this.modelPrefix}¬¬¬¬¬¬¬¬¬¬¬¬¬¬ get()")

		val primaryConstructor = modelClass.primaryConstructor
		val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()

		if (primaryConstructor == null) {
			logger.error("Can't create a model which doesn't have a primary constructor!")
			throw Exception("Can't create a model which doesn't have a primary constructor!")
		}

		val constructorKParams: List<KParameter> = primaryConstructor.parameters
		for (kParam in constructorKParams) {

			var fieldName: String

			val compoundConverter = kParam.findAnnotation<Compound>()

			// SCENARIO 1: no prefix is provided
			if(modelPrefix.isNull()) {
				// just use the kParam.name
				fieldName = kParam.name!!
			} else {
				// SCENARIO 2: a prefix has been provided
				fieldName = modelPrefix.toString() + kParam.name
			}

			logger.info("$this with prefix ${this.modelPrefix} - ${modelClass.simpleName} constructor parameter ${fieldName} value " + paramsMap.get(fieldName) + " (optional ${kParam.isOptional})")

			/* ************************************* GET THE KCLASS OF THE PARAMETER VIA THE JAVA ERASURE ******************/
			// TODO: do I need to consider kParam.type.isMarkedNullable?
			val erasure = kParam.type.jvmErasure
			val requestParamValues = paramsMap.get(fieldName)

			// TODO: turn these two into a sealed class? Wish I had a struct!
			val inputValue: String
			val inputList: List<String>

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

			// if the parameter is optional (it has a default value) and there is no input value, don't create a constructorParam entry
			if (kParam.isOptional && inputValue.isEmpty() && inputList.isEmpty()) {
				// skip this parameter and use its default value
			} else {

				// building the constructorParams map
				// if there is an annotated Converter, use it

				// look for a compound model class annotation

				if(compoundConverter != null) {
					val compoundErasure = kParam.type.jvmErasure
					val compoundPrefixString = if(compoundConverter.prefix.isNullOrEmpty()) {
						fieldName
					} else {
						compoundConverter.prefix
					}
					val compoundPrefix = Prefix(compoundPrefixString, compoundConverter.separator)
					val compoundModel = WebForm(request,compoundErasure,compoundPrefix)

					constructorParams.put(kParam,compoundModel.get())
				} else {

					val converterAnnotation = kParam.findAnnotation<CConverter>()
					if (converterAnnotation != null) {
						constructorParams.put(kParam, getAnnotatedConverterValue(converterAnnotation, inputValue))
					} else {
						val result = buildModelObject(erasure, kParam, inputValue, inputList)
						constructorParams.put(kParam, result)
					}
				}
			} // end for loop
		}

		// Finally, construct our model by passing in our constructor parameter map
		primaryConstructor.let {
			logger.info("Finally construct our ${modelClass.simpleName} object with paramsMap:")
			constructorParams.forEach {
				logger.info("..... ${it.key.name} -> ${it.value}")
			}
			logger.debug("expecting ${primaryConstructor.parameters}")
			try {
				modelObject = it.callBy(constructorParams)
			} catch (ite: InvocationTargetException) {
				logger.error("Invocation Target Exception while building ${modelClass.simpleName}")
				logger.error(ite.targetException.message)
				throw CaissonBindException(ite.targetException.message)
			}
		}

		if (!::modelObject.isInitialized || modelObject == null) {
			logger.error("Could not bind model class ${modelClass.simpleName} with request")
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

	private fun buildModelObject(erasure: KClass<*>, kParam: KParameter, inputValue: String, inputList: List<String>) : Any? {
		val converter: Converter
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
					return  multiPartFiles[0]
//					constructorParams.put(kParam, multiPartFiles[0])
				} else {
					return multiPartFiles
//					constructorParams.put(kParam, multiPartFiles)
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
						return inputList
//						constructorParams.put(kParam, inputList)
					}
				// If it's a list of files, there's nothing we need to do. Our function can return a list of files already
					CaissonMultipartContent::class -> {
						val multiPartFiles = getMultiPartFile(servletRequest = raw!!, partNames = multiPartUploadNames!!)
						return multiPartFiles
//						constructorParams.put(kParam, multiPartFiles)
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
				return inputValue
//				constructorParams.put(kParam, inputValue)
			}
			Int::class -> {
				if (!kParam.type.isMarkedNullable && inputValue.isNullOrEmpty()) {
					logger.error("Parameter ${kParam.name} has not been marked nullable but input value is empty")
				}
				converter = DefaultIntConverter()
				return getConverterValue(converter, inputValue)
//				constructorParams.put(kParam, getConverterValue(converter, inputValue))
			}
			Long::class -> {
				converter = DefaultLongConverter()
				return getConverterValue(converter, inputValue)
//				constructorParams.put(kParam, getConverterValue(converter, inputValue))
			}
			Double::class -> {
				converter = DefaultDoubleConverter()
				return getConverterValue(converter, inputValue)
//				constructorParams.put(kParam, getConverterValue(converter, inputValue))
			}
			Float::class -> {
				converter = DefaultFloatConverter()
				return getConverterValue(converter, inputValue)
//				constructorParams.put(kParam, getConverterValue(converter, inputValue))
			}
			Boolean::class -> {
				converter = DefaultBooleanConverter()
				return getConverterValue(converter, inputValue)
//				constructorParams.put(kParam, getConverterValue(converter, inputValue))
			}
		}
		// failure point! maybe better to throw an exception...
		return null
	}

}