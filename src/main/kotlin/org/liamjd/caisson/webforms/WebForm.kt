package org.liamjd.caisson.webforms

import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.convertors.*
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

	private val params: RequestParams
	private val raw: HttpServletRequest?
	private val modelClass: KClass<*>
	private var valid: Boolean = true
	private lateinit var modelObject: Any

	constructor(params: RequestParams, modelClass: KClass<*>) {
		this.params = params
		this.modelClass = modelClass
		this.raw = null
	}

	constructor(servletRequest: HttpServletRequest, modelClass: KClass<*>) {
		this.params = mapOf()
		this.modelClass = modelClass
		this.raw = servletRequest
	}

	/**
	 * Build and return the generated model object of the form data, or null. This is the primary entry point for
	 * building the model object
	 */
	fun get(): Any? {
		val primaryConstructor = modelClass.primaryConstructor

		val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()
		if (primaryConstructor != null) {

			for (constructorKParam in primaryConstructor.parameters) {
				if (params.isNotEmpty()) {
					val requestParam = params.get(constructorKParam.name)
					val inputValue: String
					val inputList: List<String>
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

					var finalValue: Any? = null
					// 1 - check for a converter
					val converterAnnotation = constructorKParam.findAnnotation<CConverter>()
					if (converterAnnotation != null) {
						// convert it using the annotated converter
						finalValue = getAnnotatedConverterValue(converterAnnotation, inputValue)
					} else {
						// attempt to convert from a basic internal type
						finalValue = getBasicValue(constructorKParam, inputValue, inputList)
					}
					constructorParams.put(constructorKParam, finalValue)
				} else {
					// params is null here, could just be a multipart form only
				}
			} // end for
			// finally, construct the object
			modelObject = primaryConstructor.callBy(constructorParams)
		} else {
			// if primary constructor is null?
		}

		if (!::modelObject.isInitialized || modelObject == null) {
			return null
		}
		return modelObject
	}

	/**
	 * If no annotated converter class is provided for the parameter, attempt to convert it though basic in-built
	 * classes. This handles Strings, Ints, Doubles, Longs, Floats, Booleans, Lists. Returns null if no conversion is possible.
	 */
	private fun getBasicValue(constructorKParam: KParameter, inputValue: String, inputList: List<String>): Any? {
		val erasure = constructorKParam.type.jvmErasure
		var converter: Converter? = null
		var finalValue: Any? = null
		when (erasure) {
			String::class -> {
				finalValue = inputValue
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
			else -> {
				// TODO: can I find a generic way of handling enums?
				println("I can't handle it (it's a ${erasure}, maybe even an Enum ${erasure.isSubclassOf(Enum::class)}?; is final: ${erasure.isFinal})")
				if (erasure.isSubclassOf(Enum::class)) {
					println(erasure.primaryConstructor?.parameters?.first())
				}
			}
		}
		if (converter != null) {
			finalValue = getConverterValue(converter, inputValue)
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