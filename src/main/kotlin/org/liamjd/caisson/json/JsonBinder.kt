package org.liamjd.caisson.json

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import org.liamjd.caisson.webforms.Form
import kotlin.reflect.KClass


/**
 * Return an object with class `modelClass` from a Json string
 */
class JsonBinder(val body: String, modelClass: KClass<*>) : Form {

	private val modelClass: KClass<*>

	init {
		this.modelClass = modelClass
	}

	override fun <T> get(): T? {
		val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
		val jsonAdapter = moshi.adapter<Any>(modelClass.java)

		val modelObject = jsonAdapter.fromJson(body)

		return modelObject as T
	}
}