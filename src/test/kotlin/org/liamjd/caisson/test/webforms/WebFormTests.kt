package org.liamjd.caisson.test.webforms

import io.mockk.every
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.convertors.Converter
import org.liamjd.caisson.extensions.bind
import org.liamjd.caisson.webforms.WebForm
import spark.QueryParamsMap
import spark.Request
import java.util.*
import kotlin.test.assertEquals

data class SimpleString(val myString: String)
data class SimpleInt(val myNumber: Int)
data class SimpleBool(val myBoolean: Boolean)
data class SimpleDouble(val myDouble: Double)
data class SimpleLong(val myLong: Long)
data class SimpleFloat(val myFloat: Float)
enum class Gender(val gender: String) {
	male("male"),
	female("female"),
	other("other")
}

class BadIntConverter : Converter {
	override fun convert(from: String): Int? {
		// this bad implementation returns 666 regardless of the input
		return 666
	}
}

class GenderConverter : Converter {
	override fun convert(from: String): Gender? {
		return Gender.valueOf(from)
	}
}

data class MySimpleDate(@CConverter(converterClass = SimpleDateConverter::class) val myDate: Date)
data class UnexpectedInteger(@CConverter(converterClass = BadIntConverter::class) val myInt: Int)
data class GenderForm(@CConverter(converterClass = GenderConverter::class) val gender: Gender)
data class ColourListForm(val colour: List<String>)
data class APerson(val name: String, val age: Int)
data class BirthdayPerson(val name: String, @CConverter(SimpleDateConverter::class) val dob: Date)

/**
 * Basic tests for parsing spark requests and building models using default and custom annotated converts
 */
class WebFormTests : Spek({

	val mSparkRequest = mockk<Request>()
	val mSparkQueryMap = mockk<QueryParamsMap>()
	val requestMap = mutableMapOf<String, Array<String>>()

	every { mSparkRequest.queryMap() } returns mSparkQueryMap
	every { mSparkQueryMap.toMap()} returns requestMap
	every { mSparkRequest.raw()} returns null

	beforeEachTest {
		println("88888888888888888888888888 Spek Test 8888888888888888888888888888")
		requestMap.clear()
	}

	describe("no conversions when working with strings") {

		val myName = arrayOf("Caisson")
		it("creates a SimpleString with myName as its value") {
			requestMap.put("myString", myName)
			val form = WebForm(mSparkRequest, SimpleString::class)
			val result: SimpleString? = form.get<SimpleString>()
			assertEquals(myName.first(), result?.myString)
		}
	}

	describe("Using the extension function request.bind() to return the correct object") {
		val myName = arrayOf("Caisson")
		it("creates a SimpleString with myName as its value") {
			requestMap.put("myString", myName)
			val result = mSparkRequest.bind<SimpleString>()
			assertEquals(myName.first(), result?.myString)
		}
	}

	describe("converting the basic Kotlin types") {

		it("conversion when working with Integers") {
			val myNumber = arrayOf("669")
			val myExpectedResult = myNumber.first().toInt()
			requestMap.put("myNumber", myNumber)
			val result = mSparkRequest.bind<SimpleInt>()
			assertEquals(myExpectedResult, result?.myNumber)
		}

		it("conversion when working with Longs") {
			val myLongNumber = arrayOf("1551441414479")
			val myExpectedResult: Long = 1551441414479L
			requestMap.put("myLong", myLongNumber)
			val result = mSparkRequest.bind<SimpleLong>()
			assertEquals(myExpectedResult, result?.myLong)
		}

		it("conversion when working with Booleans") {
			val myBoolean = arrayOf("true")
			val myExpectedResult = true
			requestMap.put("myBoolean", myBoolean)
			val result = mSparkRequest.bind<SimpleBool>()
			assertEquals(myExpectedResult, result?.myBoolean)
		}

		it("conversion when working with Doubles") {
			val myDouble = arrayOf("5.5")
			val myExpectedResult = 5.5
			requestMap.put("myDouble", myDouble)
			val result = mSparkRequest.bind<SimpleDouble>()
			assertEquals(myExpectedResult, result?.myDouble)
		}
		it("conversion when working with Floats") {
			val myFloat = arrayOf("23.64")
			val myExpectedResult: Float = 23.64F
			requestMap.put("myFloat", myFloat)
			val result = mSparkRequest.bind<SimpleFloat>()
			assertEquals(myExpectedResult, result?.myFloat)
		}
	}

	describe("conversion with an annotated conversion class") {
		it("should convert a date with the dd/MM/yyyy format") {
			val myDate = arrayOf("06/04/2002")
			val cal = Calendar.getInstance()
			cal.clear()
			cal.set(Calendar.YEAR,2002)
			cal.set(Calendar.MONTH,Calendar.APRIL)
			cal.set(Calendar.DAY_OF_MONTH,6)
			val myExpectedDate: Date = cal.time
			requestMap.put("myDate",myDate)
			val result = mSparkRequest.bind<MySimpleDate>()
			assertEquals(myExpectedDate,result?.myDate)
		}
		it("should use the annotated converter in preference to the default converter") {
			val myNumber = arrayOf("1")
			val myExpectedResult = 666
			requestMap.put("myInt", myNumber)
			val result = mSparkRequest.bind<UnexpectedInteger>()
			assertEquals(myExpectedResult, result?.myInt)
		}
	}

	describe("conversion multiple params") {
		it("should create a Person given a name and an age") {
			val name = "Liam"
			val age = "18"
			requestMap.put("name", arrayOf(name))
			requestMap.put("age", arrayOf(age))
			val result= mSparkRequest.bind<APerson>()
			assertEquals(name,result?.name)
			assertEquals(age.toInt(),result?.age)
		}
		it("should convert a basic value and an annotated converter value") {
			val name = "Liam"
			val dob = "06/04/2002"
			val cal = Calendar.getInstance()
			cal.clear()
			cal.set(Calendar.YEAR,2002)
			cal.set(Calendar.MONTH,Calendar.APRIL)
			cal.set(Calendar.DAY_OF_MONTH,6)
			val myExpectedDate: Date = cal.time
			requestMap.put("name", arrayOf(name))
			requestMap.put("dob", arrayOf(dob))
			val result = mSparkRequest.bind<BirthdayPerson>()
			assertEquals(name,result?.name)
			assertEquals(myExpectedDate,result?.dob)
		}
	}

	describe("conversion with enums") {
		it("should use an annotated converter with an enum") {
			val myGender = arrayOf("other")
			val expectedResult = Gender.other
			requestMap.put("gender",myGender)
			val result = mSparkRequest.bind<GenderForm>()
			assertEquals(expectedResult,result?.gender)
		}
		/*it("might be able to work with a reflection-based approach?") {
			val myGender = "other"
			val expectedResult = Gender.other
			request.put("gender",myGender)
			val form = WebForm.kt(request,UnconvertedGenderForm::class)
			val result = form.get() as UnconvertedGenderForm
			assertEquals(expectedResult,result.gender)
		}*/
	}

	describe("conversion with lists") {
		it("should populate a list of strings") {
			val myColours = arrayOf("red","green")
			requestMap.put("colour",myColours)
			val result = mSparkRequest.bind<ColourListForm>()
			// not going to make an assumption based on order
			assertEquals(myColours.size,result?.colour?.size)
		}

	}
})
