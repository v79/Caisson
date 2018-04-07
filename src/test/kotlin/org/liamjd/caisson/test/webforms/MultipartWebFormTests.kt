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
class FileUploads(val uploads: List<CaissonMultipartContent>)

class MultipartWebFormTests : Spek({

	describe("Should handle MultiPart web for uploads through the normal conversion mechanism") {
		val mServletRequest: HttpServletRequest = mockk<HttpServletRequest>()
		val mUploadPart: Part = mockk<Part>()
		val mSecondPart: Part = mockk<Part>()
		val bytes: ByteArray = ByteArray(10)
		val htmlUploadThing = "htmlUploadThing"
		val secondUpload = "secondUpload"
		bytes.fill(Byte.MIN_VALUE, 0)

		every { mServletRequest.parts } returns arrayListOf(mUploadPart)
		every { mServletRequest.getPart(htmlUploadThing) } returns mUploadPart
		every { mServletRequest.getAttribute(any()) } returns null // ignore org.eclipse.jetty.multipartConfig stuff
		every { mServletRequest.setAttribute(any(), any()) } just Runs // don't bother setting org.eclipse.jetty.multipartConfig

		every { mUploadPart.contentType } returns "text/text"
		every { mUploadPart.name } returns htmlUploadThing
		every { mUploadPart.size } returns 123L
		every { mUploadPart.submittedFileName } returns "/c/folder/file.txt"
		every { mUploadPart.inputStream } returns bytes.inputStream()

		every { mSecondPart.contentType } returns "img/png"
		every { mSecondPart.name } returns secondUpload
		every { mSecondPart.size } returns 456L
		every { mSecondPart.submittedFileName } returns "/c/folder/picture.png"
		every { mSecondPart.inputStream } returns bytes.inputStream()

		it("should convert the simplest single file upload") {
			every { mServletRequest.parts } returns listOf(mUploadPart)

			val uploadForm = Form(mServletRequest, FileUpload::class, listOf(htmlUploadThing)).get() as FileUpload
			assertEquals("text/text", uploadForm.upload.contentType)
			assertEquals("/c/folder/file.txt", uploadForm.upload.originalFileName)
			assertEquals(123L, uploadForm.upload.size)
			assertEquals(10, uploadForm.upload.stream.readBytes().size)
		}

		it("should allow us to upload two files in a single action") {
			every { mServletRequest.parts } returns mutableListOf(mUploadPart, secondUpload) as Collection<Part>

			val uploadForm = Form(mServletRequest, FileUploads::class, listOf(htmlUploadThing, secondUpload)).get() as FileUploads
			assertEquals("img/png", uploadForm.uploads[1].contentType)
			assertEquals("/c/folder/picture.png", uploadForm.uploads[1].originalFileName)
			assertEquals(456L, uploadForm.uploads[1].size)
			assertEquals(10, uploadForm.uploads[1].stream.readBytes().size)
		}
	}
})