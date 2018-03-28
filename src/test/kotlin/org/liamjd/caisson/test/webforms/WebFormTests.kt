package org.liamjd.caisson.test.webforms

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.org.liamjd.caisson.convertors.Converter
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
data class JavaString(val myJavaString: java.lang.String)

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

data class MySimpleDate(@CConverter(converterClass = SimpleDateConverter::class) val myDate: Date)

class WebFormTests : Spek({
	describe("when the param map is empty, do nothing and return null") {
		val emptyRequest = mutableMapOf<String, String>()
		val form = Form(emptyRequest, TestPerson::class)

		it("returns null with empty map") {
			val generatedModel = form.get()
			assertNull(generatedModel)
		}
	}

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
	}
})
