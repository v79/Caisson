package org.liamjd.caisson.convertors

import org.liamjd.caisson.models.CaissonMultipartContent
import java.io.InputStream
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest

class MultipartUploadConverter {

	fun convert(servletRequest: HttpServletRequest, uploadParameterName: String): CaissonMultipartContent {
		println("in MultipartUploadConverter with request $servletRequest looking for $uploadParameterName")

		// this is critical and must come first!
		if (servletRequest.getAttribute("org.eclipse.jetty.multipartConfig") == null) {
			val multipartConfigElement = MultipartConfigElement(System.getProperty("java.io.tmpdir"))
			servletRequest.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement)
		}

		debugServletRequest(servletRequest, uploadParameterName)
		val file = servletRequest.getPart(uploadParameterName)

		file.run {
			println("ContentType: " + this.contentType)
			println("Name: " + this.name)
			println("Size: " + this.size)
			println("submittedFileName: " + this.submittedFileName)

			val stream: InputStream = servletRequest.getPart(uploadParameterName).inputStream
			return CaissonMultipartContent(this.contentType,this.size, stream, this.submittedFileName)

		}
	}

	private fun debugServletRequest(servletRequest: HttpServletRequest, uploadParameterName: String) {
		println("DebugServletRequest: parts.size: ${servletRequest.parts.size}")
		println("DebugServletRequest: looking for upload named: ${uploadParameterName}")
		println("DebugServletRequest: found: ${servletRequest.getPart(uploadParameterName).name}")
	}
}