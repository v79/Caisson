package org.liamjd.caisson.convertors

import org.liamjd.caisson.models.CaissonMultipartContent
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part

/**
 * Converts an HttpServletRequest.part into a CaissonMultipartContent object
 */
class MultipartUploadConverter {

	fun convert(part: Part): CaissonMultipartContent {
		return CaissonMultipartContent(part.contentType, part.size, part.inputStream, part.submittedFileName)
	}

	private fun debugServletRequest(servletRequest: HttpServletRequest, uploadParameterName: String) {
		println("DebugServletRequest: parts.size: ${servletRequest.parts.size}")
		println("DebugServletRequest: looking for upload named: ${uploadParameterName}")
		println("DebugServletRequest: found: ${servletRequest.getPart(uploadParameterName).name}")
	}
}