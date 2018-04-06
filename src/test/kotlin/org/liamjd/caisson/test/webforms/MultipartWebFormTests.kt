package org.liamjd.caisson.test.webforms

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.models.CaissonMultipartContent
import org.liamjd.caisson.webforms.Form
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part
import kotlin.test.assertEquals

class FileUpload(val upload: CaissonMultipartContent)

class MultipartWebFormTests : Spek({

	describe("Should handle MultiPart web for uploads through the normal conversion mechanism") {
		val mServletRequest: HttpServletRequest = mockk<HttpServletRequest>()
		val mUploadPart: Part = mockk<Part>()
		val bytes: ByteArray = ByteArray(10)
		val htmlUploadThing = "htmlUploadThing"
		bytes.fill(Byte.MIN_VALUE,0)

		every { mServletRequest.parts} returns arrayListOf(mUploadPart)
		every { mServletRequest.getPart(htmlUploadThing) } returns mUploadPart
		every { mServletRequest.getAttribute(any())} returns null // ignore org.eclipse.jetty.multipartConfig stuff
		every { mServletRequest.setAttribute(any(),any())} just Runs // don't bother setting org.eclipse.jetty.multipartConfig
		every { mUploadPart.contentType} returns "text/text"
		every { mUploadPart.name} returns htmlUploadThing
		every { mUploadPart.size} returns 123L
		every { mUploadPart.submittedFileName} returns "/c/folder/file.txt"
		every { mUploadPart.inputStream} returns bytes.inputStream()


		it("should convert the simplest file upload") {

			val uploadForm = Form(mServletRequest,FileUpload::class, listOf(htmlUploadThing)).get() as FileUpload
			assertEquals("text/text",uploadForm.upload.contentType)
			assertEquals("/c/folder/file.txt",uploadForm.upload.originalFileName)
			assertEquals(123L,uploadForm.upload.size)
			assertEquals(10,uploadForm.upload.stream.readBytes().size)
		}
	}
})