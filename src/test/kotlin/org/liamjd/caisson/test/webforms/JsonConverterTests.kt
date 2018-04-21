package org.liamjd.caisson.test.webforms

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.api.dsl.xon
import org.liamjd.caisson.extensions.bindJson
import spark.Request
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals


data class JsonPerson(val fname: String, val sname: String)
data class PersonWithAge(val name: String, val age: Int)
data class PersonWithCustomNames(val fname: String, @Json(name = "surname") val sname: String)
data class JsonBirthdayPerson(val name: String, @LocalDateFromJson val dob: LocalDate)


@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class LocalDateFromJson

class LocalDataAdaptor() {
	@FromJson @LocalDateFromJson fun fromJson(	dob: String): LocalDate {
		val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
		try {
			return LocalDate.parse(dob,dateTimeFormat)
		} catch(e: java.time.format.DateTimeParseException) {
			return LocalDate.now()
		}
	}
}

class JsonConverterTests : Spek({

	val mSparkRequest = mockk<Request>()

	describe("Using moshi to build objects from a Json string") {
		val jasonStratham = """
			{ "fname" : "Jason", "sname" : "Stratham" }
			"""
		val jasonDonovan = """
			{ "fname" : "Jason", "surname" : "Donovan" }
			"""
		val jasonIsaacs = """
			{ "name" : "Jason Issacs", "age" : 55 }
			"""
		val jasonMomoa = """
			{ "name" : "Jason Momoa", "dob" : "01/08/1979" }
			"""

		on("json stratham") {
			every { mSparkRequest.body() } returns jasonStratham

			it("should create a JsonPerson with two string properties") {
				val person = mSparkRequest.bindJson<JsonPerson>()
				assertEquals("Jason", person?.fname)
				assertEquals("Stratham", person?.sname)
			}
		}

		on("Jason Donovan") {
			every { mSparkRequest.body() } returns jasonDonovan
			it("should create a JsonPerson with two string properties with the annotated json property name") {
				val person = mSparkRequest.bindJson<PersonWithCustomNames>()
				assertEquals("Jason", person?.fname)
				assertEquals("Donovan", person?.sname)
			}
		}

		on("Jason Issacs") {
			every { mSparkRequest.body() } returns jasonIsaacs
			it("should create a JsonPerson with a string and an int") {
				val person = mSparkRequest.bindJson<PersonWithAge>()
				assertEquals("Jason Issacs", person?.name)
				assertEquals(55, person?.age)
			}
		}

		xon("Jason Momoa") {
			every { mSparkRequest.body() } returns jasonMomoa
			it("should create a JsonPerson with a string and a custom converter for date of birth") {
				val dob: LocalDate = LocalDate.of(1979, Month.AUGUST, 1)
				val person = mSparkRequest.bindJson<JsonBirthdayPerson>()
				assertEquals("Jason Momoa", person?.name)
				assertEquals(dob, person?.dob)
			}
		}

	}
})