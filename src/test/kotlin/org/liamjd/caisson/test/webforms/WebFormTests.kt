package org.liamjd.caisson.test.webforms

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.convertors.Converter
import org.liamjd.caisson.webforms.Form
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

data class TestPerson(val name: String, val age: Int)
data class SimpleString(val myString: String)
data class SimpleInt(val myNumber: Int)
data class SimpleBool(val myBoolean: Boolean)
data class SimpleDouble(val myDouble: Double)
data class SimpleLong(val myLong: Long)
data class SimpleFloat(val myFloat: Float)
data class JavaString(val myJavaString: java.lang.String)
enum class Gender(val gender: String) {
	male("male"),
	female("female"),
	other("other")
}

class SimpleDateConverter : Converter {
	override fun convert(from: String): Date? {
		val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
		try {
			return sdf.parse(from)
		} catch (e: ParseException) {
			return Date()
		}
	}
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
data class UnconvertedGenderForm(val gender: Gender)

class WebFormTests : Spek({
	// not sure about this test scenario
	/*describe("when the param map is empty, do nothing and return null") {
		val emptyRequest = mutableMapOf<String, String>()
		val form = Form(emptyRequest, TestPerson::class)

		it("returns null with empty map") {
			val generatedModel = form.get()
			assertNull(generatedModel)
		}
	}*/

	describe("no conversions when working with strings") {
		val request = mutableMapOf<String, String>()
		val myName = "Caisson"
		request.put("myString", myName)
		it("creates a SimpleString with myName as its value") {
			val form = Form(request, SimpleString::class)
			val result: SimpleString = form.get() as SimpleString
			assertEquals(myName, result.myString)
		}
	}

	describe("converting the basic Kotlin types") {
		val request = mutableMapOf<String, String>()

		beforeEachTest { request.clear() }

		it("conversion when working with Integers") {
			val myNumber = "669"
			val myExpectedResult = myNumber.toInt()
			request.put("myNumber", myNumber)
			val form = Form(request, SimpleInt::class)
			val result: SimpleInt = form.get() as SimpleInt
			assertEquals(myExpectedResult, result.myNumber)
		}

		it("conversion when working with Longs") {
			val myLongNumber = "1551441414479"
			val myExpectedResult: Long = 1551441414479L
			request.put("myLong", myLongNumber)
			val form = Form(request, SimpleLong::class)
			val result: SimpleLong = form.get() as SimpleLong
			assertEquals(myExpectedResult, result.myLong)
		}

		it("conversion when working with Booleans") {
			val myBoolean = "true"
			val myExpectedResult = true
			request.put("myBoolean", myBoolean)
			val form = Form(request, SimpleBool::class)
			val result: SimpleBool = form.get() as SimpleBool
			assertEquals(myExpectedResult, result.myBoolean)
		}

		it("conversion when working with Doubles") {
			val myDouble = "5.5"
			val myExpectedResult = 5.5
			request.put("myDouble", myDouble)
			val form = Form(request, SimpleDouble::class)
			val result: SimpleDouble = form.get() as SimpleDouble
			assertEquals(myExpectedResult, result.myDouble)
		}
		it("conversion when working with Floats") {
			val myFloat = "23.64"
			val myExpectedResult: Float = 23.64F
			request.put("myFloat", myFloat)
			val form = Form(request, SimpleFloat::class)
			val result: SimpleFloat = form.get() as SimpleFloat
			assertEquals(myExpectedResult, result.myFloat)
		}
	}

	describe("conversion with an annotated conversion class") {
		val request = mutableMapOf<String, String>()
		it("should convert a date with the dd/MM/yyyy format") {
			val myDate = "06/04/2002"
			val cal = Calendar.getInstance()
			cal.clear()
			cal.set(Calendar.YEAR,2002)
			cal.set(Calendar.MONTH,Calendar.APRIL)
			cal.set(Calendar.DAY_OF_MONTH,6)
			val myExpectedDate: Date = cal.time
			request.put("myDate",myDate)
			val form = Form(request,MySimpleDate::class)
			val result: MySimpleDate = form.get() as MySimpleDate
			assertEquals(myExpectedDate,result.myDate)
		}
		it("should use the annotated converter in preference to the default converter") {
			val myNumber = "1"
			val myExpectedResult = 666
			request.put("myInt", myNumber)
			val form = Form(request, UnexpectedInteger::class)
			val result: UnexpectedInteger = form.get() as UnexpectedInteger
			assertEquals(myExpectedResult, result.myInt)
		}
	}

	describe("conversion with enums") {
		val request = mutableMapOf<String, String>()
		it("should use an annotated converter with an enum") {
			val myGender = "other"
			val expectedResult = Gender.other
			request.put("gender",myGender)
			val form = Form(request,GenderForm::class)
			val result = form.get() as GenderForm
			assertEquals(expectedResult,result.gender)
		}
		/*it("might be able to work with a reflection-based approach?") {
			val myGender = "other"
			val expectedResult = Gender.other
			request.put("gender",myGender)
			val form = Form(request,UnconvertedGenderForm::class)
			val result = form.get() as UnconvertedGenderForm
			assertEquals(expectedResult,result.gender)
		}*/
	}
})
