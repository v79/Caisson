package org.liamjd.caisson.webforms

import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.convertors.*
import org.liamjd.caisson.models.CaissonMultipartContent
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

typealias RequestParams = Map<String, Array<String>>

/**
 * Represents the internal data state of an HTML form element. It is generated from a Spark HTML request parameter map.
 * To use, create a `Form` instance and call its `get()` method, casting the result to your model class.
 * To construct, use
 *
 * `val yourModel = Form(requestParamsMap, YourModel::class).get() as YourModel`
 */
class Form {

	private val paramsMap: RequestParams
	private val raw: HttpServletRequest?
	private val modelClass: KClass<*>
	private val multiPartUploadNames: List<String>?
	private lateinit var modelObject: Any

	constructor(params: RequestParams, modelClass: KClass<*>) {
		this.paramsMap = params
		this.modelClass = modelClass
		this.raw = null
		this.multiPartUploadNames = null
	}

	constructor(servletRequest: HttpServletRequest, modelClass: KClass<*>, partName: String) : this(servletRequest, modelClass,listOf(partName))

	constructor(servletRequest: HttpServletRequest, modelClass: KClass<*>, partNames: List<String>) {
		this.paramsMap = mapOf()
		this.modelClass = modelClass
		this.raw = servletRequest
		this.multiPartUploadNames = partNames
	}

	/**
	 * Build and return the generated model object of the form data, or null. This is the primary entry point for
	 * building the model object
	 */

	fun get(): Any? {
		val primaryConstructor = modelClass.primaryConstructor


		if(primaryConstructor == null) {
			throw Exception("Can't create a model which doesn't have a primary constructor!")
		}

		// for each parameter in the constructor, determine its final value
		val finalValueList: MutableList<Any> = mutableListOf()
		for(constructorKParam in primaryConstructor.parameters) {
			val parameterName: String = constructorKParam.name.toString()
			var finalValue: Any? = null
			val inputValue: String
			val inputList: List<String>

			// 1 --------------- We have a parameter map
			if(paramsMap.isNotEmpty()) {
				finalValue = getValueFromParameterMap(constructorKParam, parameterName)
			} else {
				// 2 ---------------- No parameter map
			}





			if(finalValue == null) {
				throw Exception("Could not determine the final value for parameter ${constructorKParam}")
			}
			finalValueList.add(finalValue)
		}


		return 2
	}


	private fun getValueFromParameterMap(constructorKParam: KParameter, paramName: String): Any {
		println("+++++++++++++++++ getValueFromParameterMap() paramsMap ${paramsMap}")
		// this will mostly be a string; may be an array of strings for checkboxes, etc
		val valueResult: MutableList<Any?> = mutableListOf()
		val inputList = paramsMap.get(paramName)

		val converterAnnotation = constructorKParam.findAnnotation<CConverter>()




		return 1

	}



	fun get2(): Any? {
		val primaryConstructor = modelClass.primaryConstructor

		val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()
		if (primaryConstructor != null) {
			var finalValue: Any? = null

			for (constructorKParam in primaryConstructor.parameters) {
				// reset finalValue
				finalValue = null
				val inputValue: String
				val inputList: List<String>
				if (paramsMap.isNotEmpty()) {
					val name = constructorKParam.name
					val requestParam = paramsMap.get(constructorKParam.name)

					// determine what our input value(s) are

					if (requestParam != null) {
						if (requestParam.size == 1) {
							inputValue = requestParam.get(0)
							inputList = emptyList()
						} else {
							// now we have an array of values, perhaps from a checkbox?
							inputList = requestParam.toList()
							inputValue = ""
						}
					} else {
						inputValue = ""
						inputList = emptyList()
					}
				} else {
					// paramsMap is empty but we could have a CaissonMultipartContent?
					inputValue = ""
					inputList = emptyList()

					// this is now List<CaissonMultipartContent>

					val erasure = constructorKParam.type.jvmErasure
					when(erasure) {
						CaissonMultipartContent::class -> {
							println("We've got a multi part document uploady thingy")
							if(raw != null && multiPartUploadNames != null) {

								if (raw.getAttribute("org.eclipse.jetty.multipartConfig") == null) {
									val multipartConfigElement = MultipartConfigElement(System.getProperty("java.io.tmpdir"))
									raw.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)
								}


								// TODO: handle multiple file uploads
								val multiPartFormConverter: MultipartUploadConverter = MultipartUploadConverter()

								val fileList: MutableList<CaissonMultipartContent> = mutableListOf()
								for(part in raw.parts) {
									println("\tconverting part $part (${part.name})")
									fileList.add(multiPartFormConverter.convert(part))
								}

								finalValue = fileList

								/*if(multiPartUploadNames.size == 1) {
									finalValue = multiPartFormConverter.convert(raw,multiPartUploadNames.first())
								}
								else {
									// how do I handle multiple files in a single upload
									println("We need to handle ${multiPartUploadNames.size} file uploads. What even is the return type?")
									val fileList: MutableList<CaissonMultipartContent> = mutableListOf()
									for(file in multiPartUploadNames) {
										println("converting $file")
										fileList.add(multiPartFormConverter.convert(raw,file))
									}
									finalValue = fileList
								}*/
							} else {
								// throw exception here?
								finalValue = null
								println("We've got a multiPartFormConverter but no HttpServletRequest!")
							}
						}
						else -> {
							// throw an exception?
							println("Don't know what to do here")
						}
					}

				}


				println("___ after all that, our finalValue is currently... ${finalValue}. If it's still null, try to convert it___ ")

				// will be null unless the multipart converter has kicked in
				if(finalValue == null) {
					val converterAnnotation = constructorKParam.findAnnotation<CConverter>()
					if (converterAnnotation != null) {
						// convert it using the annotated converter
						finalValue = getAnnotatedConverterValue(converterAnnotation, inputValue)
					} else {
						// attempt to convert from a basic internal type
						finalValue = getBasicValue(constructorKParam, inputList)
					}
				}
				constructorParams.put(constructorKParam, finalValue)
			}
		} // end constructor param for loop

		// finally, construct the object
		primaryConstructor?.let {
			println("\t**** finally construct our object with paramsMap: ${constructorParams}, expecting ${primaryConstructor.parameters} ****")
			modelObject = it.callBy(constructorParams)
		}
//		if(primaryConstructor != null) {
//			modelObject = primaryConstructor.
//		}

		if (!::modelObject.isInitialized || modelObject == null) {
			return null
		}
		return modelObject
	}

	/**
	 * If no annotated converter class is provided for the parameter, attempt to convert it though basic in-built
	 * classes. This handles Strings, Ints, Doubles, Longs, Floats, Booleans, Lists. Returns null if no conversion is possible.
	 */
	private fun getBasicValue(constructorKParam: KParameter, inputList: List<String>, servletRequest: HttpServletRequest? = null): Any? {
		val erasure = constructorKParam.type.jvmErasure
		var converter: Converter? = null
		var finalValue: Any? = null
		when (erasure) {
			String::class -> {
				finalValue = inputList[0]
			}
			Int::class -> {
				converter = DefaultIntConverter()
			}
			Long::class -> {
				converter = DefaultLongConverter()
			}
			Double::class -> {
				converter = DefaultDoubleConverter()
			}
			Float::class -> {
				converter = DefaultFloatConverter()
			}
			Boolean::class -> {
				converter = DefaultBooleanConverter()
			}
			List::class -> {
				val listConverter = DefaultListConverter()
				finalValue = listConverter.convert(inputList)
			}
			/*CaissonMultipartContent::class -> {
				println("We've got a multi part document uploady thingy")
				if(servletRequest != null) {
					val multiPartFormConverter: MultipartUploadConverter = MultipartUploadConverter()
					finalValue = multiPartFormConverter.convert(servletRequest,"upload")
				} else {
					// throw exception here?
					finalValue = null
				}

			}*/
			else -> {
				// TODO: can I find a generic way of handling enums?
				println("I can't handle it (it's a ${erasure}, maybe even an Enum ${erasure.isSubclassOf(Enum::class)}?; is final: ${erasure.isFinal})")
				if (erasure.isSubclassOf(Enum::class)) {
					println(erasure.primaryConstructor?.parameters?.first())
				}
			}
		}
		if (converter != null) {
			finalValue = getConverterValue(converter, inputList[0])
		}
		return finalValue
	}

	/**
	 * Call the conversion function defined by the Default converter for the given class
	 */
	private fun getConverterValue(converter: Converter, inputValue: String): Any? {
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

}