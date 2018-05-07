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
import javax.servlet.http.HttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InitializerBlockTests: Spek({
	val mSparkRequest = mockk<Request>()
	val mRaw = mockk<HttpServletRequest>()
	val mSparkQueryMap = mockk<QueryParamsMap>()
	val map = mutableMapOf<String, Array<String>>()

	every { mSparkRequest.queryMap().toMap()} returns map
	every { mSparkRequest.raw() } returns mRaw
	every { mRaw.getAttribute(any()) } returns null
	every { mRaw.setAttribute(any(), any()) } just Runs

	afterEachTest { map.clear() }

	describe("Should call the initializer function on our newly created object") {
		it("Will have a name, a date of birth, an age, and a hash value") {
			val dob = "01/01/1970"
			val name = "Wolfgang"
			map.put("dob",arrayOf(dob))
			map.put("name",arrayOf(name))

			val wolfgang = mSparkRequest.bind<PersonWithInitBlock>()
			assertNotNull(wolfgang) {
				assertEquals(name,it.name)
				assertTrue(it.age > 30)
				assertTrue(it.toString().startsWith(name))
				assertTrue(it.toString().length > name.length)
			}
		}
	}
})