package org.liamjd.caisson.extensions

import org.liamjd.caisson.json.JsonBinder
import org.liamjd.caisson.webforms.WebForm
import spark.Request

/**
 * Extension function to bind the model object (as T?) from the Spark request
 */

inline fun <reified T> Request.bind(): T? {
	return WebForm(this,T::class).get()
}

/**
 * Extension function to bind the model object (as T?) from the Spark request, assuming a CaissonMultipartContent
 * parameter for file uploads
 */
inline fun <reified T> Request.bind(partNames: List<String>): T? {
	return WebForm(this,T::class,partNames).get()
}

inline fun <reified T> Request.bindJson(): T? {
	return JsonBinder(this.body(),T::class).get()
}