package org.liamjd.caisson.test.webforms

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.extensions.bind
import spark.QueryParamsMap
import spark.Request
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testing file uploads which use the MultipartUploadConverter class
 */
class FileUploadTests : Spek({

	val mSparkRequest = mockk<Request>()
	val mRaw = mockk<HttpServletRequest>()
	val paramsMap: Map<String, List<String>>
	val mSparkQueryMap = mockk<QueryParamsMap>()
	val map = mutableMapOf<String, Array<String>>()

	every { mSparkRequest.queryMap() } returns mSparkQueryMap
	every { mSparkRequest.raw() } returns mRaw
	every { mSparkQueryMap.toMap() } returns map
	every { mRaw.getAttribute(any()) } returns null
	every { mRaw.setAttribute(any(), any()) } just Runs

	afterEachTest { map.clear() }

	describe("Test conversion of the Person class") {
		val name: String = "Logan"
		val dob: String = "01/01/1970"
		val calendar: Calendar = Calendar.getInstance()
		calendar.clear()
		calendar.set(Calendar.YEAR, 1970)
		calendar.set(Calendar.MONTH, Calendar.JANUARY)
		calendar.set(Calendar.DAY_OF_MONTH, 1)

		it("Person has a string and an annotated converter") {
			map.put("name", arrayOf(name))
			map.put("dob", arrayOf(dob))
			val person = mSparkRequest.bind<Person>()

			assertEquals(name, person?.name)
			assertEquals(calendar.time, person?.dob)
			println(person)
		}
	}

	describe("Uploading files") {
		val uploadPhoto = "uploadPhoto"
		val uploadDoc1 = "uploadDoc1"
		val uploadDoc2 = "uploadDoc2"
		val uploadManyDocs = "uploadManyDocs"
		val photoPart = mockk<Part>()
		val doc1Part = mockk<Part>()
		val doc2Part = mockk<Part>()

		val bytes: ByteArray = ByteArray(10)
		bytes.fill(Byte.MIN_VALUE, 0)

		every { mRaw.getPart(uploadPhoto) } returns photoPart
		every { photoPart.name } returns uploadPhoto
		every { photoPart.contentType } returns "img/jpg"
		every { photoPart.size } returns 123L
		every { photoPart.submittedFileName } returns "/c/folder/photo.jpg"
		every { photoPart.inputStream } returns bytes.inputStream()

		it("Uploading a single named file to a basic class") {
			every { mRaw.getPart(uploadPhoto) } returns photoPart
			every { mRaw.parts } returns arrayListOf(photoPart)

			val photograph = mSparkRequest.bind<Photograph>(arrayListOf(uploadPhoto))
			assertEquals(photoPart.submittedFileName, photograph?.picture?.originalFileName)
			assertEquals(photoPart.contentType, photograph?.picture?.contentType)
			assertEquals(photoPart.size, photograph?.picture?.size)
			assertEquals(10, photograph?.picture?.stream?.readBytes()?.size)
			photograph?.picture?.stream?.reset()
		}

		it("Uploading a single named file to a basic clas using the request.bind() extension method") {
			every { mRaw.getPart(uploadPhoto) } returns photoPart
			every { mRaw.parts } returns arrayListOf(photoPart)

			val photograph = mSparkRequest.bind<Photograph>(arrayListOf(uploadPhoto))
			assertEquals(photoPart.submittedFileName, photograph?.picture?.originalFileName)
			assertEquals(photoPart.contentType, photograph?.picture?.contentType)
			assertEquals(photoPart.size, photograph?.picture?.size)
			assertEquals(10, photograph?.picture?.stream?.readBytes()?.size)
			photograph?.picture?.stream?.reset()
		}

		it("Uploading two documents, each with their own input name") {
			val uploadDoc = "uploadDoc"
			every { mRaw.getPart(uploadDoc1) } returns doc1Part
			every { mRaw.getPart(uploadDoc2) } returns doc2Part
			every { mRaw.parts } returns arrayListOf(doc1Part, doc2Part)

			every { doc1Part.name } returns uploadDoc
			every { doc1Part.contentType } returns "text/pdf"
			every { doc1Part.size } returns 456L
			every { doc1Part.submittedFileName } returns "/c/folder/lawyer.pdf"
			every { doc1Part.inputStream } returns bytes.inputStream()
			every { doc2Part.name } returns uploadDoc
			every { doc2Part.contentType } returns "text/doc"
			every { doc2Part.size } returns 7896L
			every { doc2Part.submittedFileName } returns "/c/folder/solictor.pdf"
			every { doc2Part.inputStream } returns bytes.inputStream()

			val legalDocuments = mSparkRequest.bind<LegalDocuments>(arrayListOf(uploadDoc))
			legalDocuments?.docs?.forEach {
				// not a great test but I can't assume an order to this list
				assertTrue(it.originalFileName.equals(doc1Part.submittedFileName) || it.originalFileName.equals(doc2Part.submittedFileName))
			}

			assertEquals(2, legalDocuments?.docs?.size)
		}

		it("Uploading two documents, sharing the same input name") {
			every { mRaw.getPart(uploadDoc1) } returns doc1Part
			every { mRaw.getPart(uploadDoc2) } returns doc2Part
			every { mRaw.parts } returns arrayListOf(doc1Part, doc2Part)

			every { doc1Part.name } returns uploadDoc1
			every { doc1Part.contentType } returns "text/pdf"
			every { doc1Part.size } returns 456L
			every { doc1Part.submittedFileName } returns "/c/folder/lawyer.pdf"
			every { doc1Part.inputStream } returns bytes.inputStream()
			every { doc2Part.name } returns uploadDoc2
			every { doc2Part.contentType } returns "text/doc"
			every { doc2Part.size } returns 7896L
			every { doc2Part.submittedFileName } returns "/c/folder/solictor.pdf"
			every { doc2Part.inputStream } returns bytes.inputStream()

			val legalDocuments2 = mSparkRequest.bind<LegalDocuments>(arrayListOf(uploadManyDocs))
//			assertEquals(2, legalDocuments2?.docs?.size)
		}
	}


})