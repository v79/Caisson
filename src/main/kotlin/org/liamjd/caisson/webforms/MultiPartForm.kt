package org.liamjd.caisson.webforms

import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.superclasses

@Deprecated("This is obsolete and doesn't work",ReplaceWith("org.liamjd.caisson.webforms.WebForm"))
class MultiPartForm(raw: HttpServletRequest, multiPartModel: KClass<*>) {

	init {

		val caissonSuperclass: KClass<*>? = multiPartModel.superclasses.find { it.qualifiedName == "org.liamjd.spark.caisson.controllers.CaissonMultipartForm" }
		if (caissonSuperclass == null) {
			// abort! throw exception
			throw Exception("${multiPartModel.qualifiedName} does not extend org.liamjd.spark.caisson.controllers.CaissonMultipartForm")
		}

		if (raw.getAttribute("org.eclipse.jetty.multipartConfig") == null) {
			val multipartConfigElement = MultipartConfigElement(System.getProperty("java.io.tmpdir"))
			raw.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)
		}

		val primaryConstructor = multiPartModel.primaryConstructor


		val partNames = raw.parts.map { it.name }

		// need to map the part names with the parameter names
		// but what about the implicit values such as contentType, name, size, submittedFileName
		// and headers such as content-disposition and content-type

		val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()
		if (primaryConstructor != null) {
			for (constructorKParam in primaryConstructor.parameters) {
				println("MultiPartForm: $constructorKParam")


			}
		}
		println()

		println("raw part names: $partNames")

//		raw.parts.forEach {
//			println("raw part: ${it.name}")
//		}


	}
}