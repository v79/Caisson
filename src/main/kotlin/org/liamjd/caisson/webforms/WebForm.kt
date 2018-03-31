package org.liamjd.caisson.webforms

import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.convertors.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

typealias RequestParams = Map<String, Array<String>>

class FormField(val key: String, val value: String?)


/**
 * Represents the internal data state of an HTML form element. It is generated from a Spark HTML request parameter map
 */
class Form(val params: RequestParams, val modelClass: KClass<*>) {
	private val fields = mutableSetOf<FormField>()
	private var valid: Boolean = true
	private lateinit var modelObject: Any

	init {
		if (params != null) {
			val primaryConstructor = modelClass.primaryConstructor

			val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()
			if (primaryConstructor != null) {
				for (constructorKParam in primaryConstructor.parameters) {

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

//					val inputValue: String = params.get(constructorKParam.name) ?: "" // Could be null when it's an unset checkbox
					var finalValue: Any? = null
					// 1 - check for a converter
					val converterAnnotation = constructorKParam.findAnnotation<CConverter>()
					if (converterAnnotation != null) {
						// convert it
						finalValue = getConvertedValue(converterAnnotation, inputValue)
					} else {
						val erasure = constructorKParam.type.jvmErasure
						var converter: Converter? = null
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
								if(erasure.isSubclassOf(Enum::class)) {
									println(erasure.primaryConstructor?.parameters?.first())
								}
							}
						}
						if (converter != null) {
							finalValue = getConvertedValue(converter, inputValue)
						}
					}

					constructorParams.put(constructorKParam, finalValue)

				}
				modelObject = primaryConstructor.callBy(constructorParams)

			}
		} else {
			// params is empty, which can happen when a checkbox is unticked
		}

	}

	/**
	 * Call the conversion function defined by the Default converter for the given class
	 */
	private fun getConvertedValue(converter: Converter, inputValue: String): Any? {
		return converter.convert(inputValue)
	}

	/**
	 * Call the conversion function defined by the CConverter provided in the annotation
	 */
	private fun getConvertedValue(cConverter: CConverter, inputValue: String): Any? {
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
	 * Useful debugging toString method
	 */
	override fun toString(): String {
		val sb: StringBuilder = StringBuilder()
		for (f in fields) {
			sb.append(f.key).append(" -> ").append(f.value).append(", ")
		}
		sb.append(" (valid $valid)")
		return sb.toString()
	}

	/**
	 * Return the generated model object of the form data, or null
	 */
	fun get(): Any? {
		if (!::modelObject.isInitialized || modelObject == null) {
			return null
		}
		return modelObject
	}
}