package org.liamjd.caisson.test.webforms

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.exceptions.CaissonBindException
import org.liamjd.caisson.extensions.bind
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
	val mSparkQueryMap = mockk<QueryParamsMap>()
	val map = mutableMapOf<String, Array<String>>()

	every { mSparkRequest.queryMap().toMap()} returns map
	every { mSparkRequest.raw() } returns mRaw
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

			assertNotNull(unusedFieldsTest) {
				assertEquals(used,it.used)
				assertEquals(usedNumber.toInt(), it.usedNumber)
				assertNotNull(it.unusedInt)
			}
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
			val emptyRequest = mSparkRequest.bind<UnusedFieldsTest>()
			assertNotNull(emptyRequest)
			assertTrue { emptyRequest?.used!!.isBlank() }
			assertEquals(0,emptyRequest?.usedNumber)
			assertEquals(0,emptyRequest?.unusedInt)
		}

		it("Should not throw exception if the converter can handle empty values") {
			val name = "Bob"
			map.put("name",arrayOf(name))
			val emptyPersonRequest = mSparkRequest.bind<PersonWithDefaultBirthday>()
			assertNotNull(emptyPersonRequest) {
				assertEquals(name, it.name)
				assertNotNull(it.dob)
				assert(it.dob.before(Date()))
			}
		}

		it("Should use default values to construct the object when not provided by params") {
			val colour = "Blue"
			map.put("colour",arrayOf(colour))
			val carnation = mSparkRequest.bind<DefaultValueTest>()
			assertNotNull(carnation) {
				assertEquals(colour,it.colour)
				assertEquals("Carnation", it.flower)
			}
		}

		it("Should not use default value when one is provided in params") {
			val colour = "Red"
			val flower = "Tulip"
			map.put("colour",arrayOf(colour))
			map.put("flower",arrayOf(flower))
			val tulip = mSparkRequest.bind<DefaultValueTest>()
			assertNotNull(tulip) {
				assertEquals(colour,it.colour)
				assertEquals(flower,it.flower)
			}

		}
	}

})