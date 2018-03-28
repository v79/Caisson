package org.liamjd.caisson.webforms

import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.org.liamjd.caisson.convertors.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

typealias RequestParams = Map<String, String>

class FormField(val key: String, val value: String?)


/**
 * Represents the internal data state of an HTML form element. It is generated from a Spark HTML request parameter map
 */
class Form(val params: RequestParams, val modelClass: KClass<*>) {
	val fields = mutableSetOf<FormField>()
	var valid: Boolean = true
	lateinit var modelObject: Any

	init {
		if (params.isNotEmpty()) {
			val primaryConstructor = modelClass.primaryConstructor

			val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()
			if (primaryConstructor != null) {
				for (constructorKParam in primaryConstructor.parameters) {
					val inputValue: String = params.get(constructorKParam.name) ?: "" // I don't ever expect a null here
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
								println("I've found a string")
								finalValue = inputValue
							}
							Int::class -> {
								println("I've found an Int")
								converter = DefaultIntConverter()
							}
							Long::class -> {
								println("I've found a Long")
								converter = DefaultLongConverter()
							}
							Double::class -> {
								println("I've found a Double")
								converter = DefaultDoubleConverter()
							}
							Float::class -> {
								println("I've found a Float")
								converter = DefaultFloatConverter()
							}
							Boolean::class -> {
								println("I've found a Boolean")
								converter = DefaultBooleanConverter()
							}
							else -> println("I don't know what I have, but I can't handle it")
						}
						if (converter != null) {
							finalValue = getConvertedValue(converter, inputValue)
						}
					}

					constructorParams.put(constructorKParam, finalValue)

				}
				modelObject = primaryConstructor.callBy(constructorParams)

			}
		}

	}

	private fun getConvertedValue(converter: Converter, inputValue: String): Any? {
		return converter.convert(inputValue)
	}

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

	override fun toString(): String {
		val sb: StringBuilder = StringBuilder()
		for (f in fields) {
			sb.append(f.key).append(" -> ").append(f.value).append(", ")
		}
		sb.append(" (valid $valid)")
		return sb.toString()
	}

	/**
	 * Return the generated model object of the form data
	 */
	fun get(): Any? {
		if (!::modelObject.isInitialized || modelObject == null) {
			return null
		}
		return modelObject
	}
}