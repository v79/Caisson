package org.liamjd.caisson.extensions

import org.liamjd.caisson.webforms.WebForm
import spark.Request
import kotlin.reflect.KClass

/**
 * Extension function to bind the model object (as Any?) from the Spark request
 */
fun <T> Request.bind(modelClass: KClass<*>): T? {
//	return modelClass.cast(WebForm(this, modelClass).get())
	return WebForm(this,modelClass).get()
}

/**
 * Extension function to bind the model object (as Any?) from the Spark request, assuming a CaissonMultipartContent
 * parameter for file uploads
 */
fun <T> Request.bind(modelClass: KClass<*>, partNames: List<String>): T? {
	return WebForm(this,modelClass,partNames).get()
}