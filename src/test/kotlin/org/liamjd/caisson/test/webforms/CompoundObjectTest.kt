package org.liamjd.caisson.test.webforms

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.extensions.bind
import org.liamjd.caisson.webforms.Prefix
import spark.QueryParamsMap
import spark.Request
import javax.servlet.http.HttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

object Business {
	val name = "Liam's Software Inc"
	val address = Address("10 High Street", "Royal Mile","", "Edinburgh", "EH1 1AB")
	val email = "liam@software.inc"
}

class CompoundObjectTest : Spek({
	val mSparkRequest = mockk<Request>()
	val mRaw = mockk<HttpServletRequest>()
	val mSparkQueryMap = mockk<QueryParamsMap>()
	val map = mutableMapOf<String, Array<String>>()

	every { mSparkRequest.queryMap() } returns mSparkQueryMap
	every { mSparkRequest.raw() } returns mRaw
	every { mSparkQueryMap.toMap() } returns map
	every { mRaw.getAttribute(any()) } returns null
	every { mRaw.setAttribute(any(), any()) } just Runs

	beforeEachTest { map.clear() }
	afterEachTest { map.clear() }

	describe("binding a business with a @Compound Address class") {
		it("will build a complete business with the address") {
			map.put("businessName",arrayOf(Business.name))
			map.put("address.addr1",arrayOf(Business.address.addr1))
			map.put("address.addr2",arrayOf(Business.address.addr2))
			map.put("address.addr3",arrayOf(Business.address.addr3))
			map.put("address.town",arrayOf(Business.address.town))
			map.put("address.postcode",arrayOf(Business.address.postcode))
			map.put("email",arrayOf(Business.email))
			val myBusiness = mSparkRequest.bind<BusinessDetails>()

			assertNotNull(myBusiness) {
				assertEquals(Business.name, it.businessName)
				assertEquals(Business.email, it.email)
				assertEquals(Business.address.addr1, it.address.addr1)
				assertEquals(Business.address.town, it.address.town)
			}
		}
	}

	describe("binding a business with a @Compound Address class with custom prefix and separators") {
		val prefix = Prefix("wibble","-").toString()

		it("will build a complete business with the address") {
			map.put("businessName",arrayOf(Business.name))
			map.put("${prefix}addr1",arrayOf(Business.address.addr1))
			map.put("${prefix}addr2",arrayOf(Business.address.addr2))
			map.put("${prefix}addr3",arrayOf(Business.address.addr3))
			map.put("${prefix}town",arrayOf(Business.address.town))
			map.put("${prefix}postcode",arrayOf(Business.address.postcode))
			map.put("email",arrayOf(Business.email))

			val myBusiness = mSparkRequest.bind<BusinessWithPrefix>()

			assertNotNull(myBusiness) {
				assertEquals(Business.name, it.businessName)
				assertEquals(Business.email, it.email)
				assertEquals(Business.address.addr1, it.address.addr1)
				assertEquals(Business.address.town, it.address.town)
			}
		}
	}
})