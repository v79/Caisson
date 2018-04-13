package org.liamjd.caisson.test.webforms

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.Exceptions.CaissonBindException
import org.liamjd.caisson.extensions.bind
import org.liamjd.caisson.webforms.WebForm
import spark.QueryParamsMap
import spark.Request
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Testing when the request does not contain all the files identified in the model
 */
class IncompleteModelTests: Spek( {

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

	describe("Model class has constructor fields which are not in the request params") {

		it("Should create a UnusedFieldsTest given only the first String and Int") {
			val used = "I am here"
			val usedNumber = "999";
			map.put("used", arrayOf(used))
			map.put("usedNumber", arrayOf(usedNumber))
			val unusedFieldsTest: UnusedFieldsTest? = mSparkRequest.bind<UnusedFieldsTest>()

			assertNotNull(unusedFieldsTest)
			assertEquals(used,unusedFieldsTest?.used)
			assertEquals(usedNumber.toInt(), unusedFieldsTest?.usedNumber)
			assertNotNull(unusedFieldsTest?.unusedInt)
		}

		it("Should fail when not providing a value which should be converted by an annotated converter") {
			val name = "Liam"
			// not providing a dob value here
			map.put("name", arrayOf(name))

			assertFailsWith<CaissonBindException> {
				val incompletePerson = mSparkRequest.bind<BirthdayPerson>()
			}
		}

		it("Should construct default values when request is empty with simple class fields") {
			val emptyRequest = mSparkRequest.bind(UnusedFieldsTest::class) as UnusedFieldsTest
			assertNotNull(emptyRequest)
			assertTrue { emptyRequest.used.isBlank() }
			assertEquals(0,emptyRequest.usedNumber)
			assertEquals(0,emptyRequest.unusedInt)
		}

		it("Should not throw exception if the converter can handle empty values") {
			val emptyPersonRequest = mSparkRequest.bind(PersonWithDefaultBirthday::class) as PersonWithDefaultBirthday
			assertNotNull(emptyPersonRequest)
			assertNotNull(emptyPersonRequest.dob)
			assert(emptyPersonRequest.dob.before(Date()))
		}
	}

})