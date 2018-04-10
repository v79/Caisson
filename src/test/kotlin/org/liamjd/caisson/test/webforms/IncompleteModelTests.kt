package org.liamjd.caisson.test.webforms

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.webforms.WebForm
import spark.QueryParamsMap
import spark.Request
import javax.servlet.http.HttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
		val used = "I am here"
		val usedNumber = "999";

		it("Should create a UnusedFieldsTest given only the first String and Int") {
			map.put("used", arrayOf(used))
			map.put("usedNumber", arrayOf(usedNumber))
			val unusedFieldsTest: UnusedFieldsTest = WebForm(mSparkRequest, UnusedFieldsTest::class).get() as UnusedFieldsTest

			assertNotNull(unusedFieldsTest)
			assertEquals(used,unusedFieldsTest.used)
			assertEquals(usedNumber.toInt(), unusedFieldsTest.usedNumber)
			assertNotNull(unusedFieldsTest.unusedInt)
		}
	}

})