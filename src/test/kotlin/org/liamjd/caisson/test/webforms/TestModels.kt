package org.liamjd.caisson.test.webforms

import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.models.CaissonMultipartContent
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.*

/**
 * A bunch of model classes used in testing
 */

data class Person(val name: String, @CConverter(converterClass = SimpleDateConverter::class) val dob: Date)
data class PersonWithDefaultBirthday(val name: String, @CConverter(converterClass = AssumingDateConverter::class) val dob: Date)

data class Photograph(val picture: CaissonMultipartContent)

data class LegalDocuments(val docs: List<CaissonMultipartContent>)

data class UnusedFieldsTest(val used: String, val usedNumber: Int, val unusedInt: Int)

data class DefaultValueTest(val colour: String, val flower: String = "Carnation")

class PersonWithInitBlock(val name: String, @CConverter(LocalDateConverter::class) val dob: LocalDate) {

	private val hash: Long
	val age: Int
		get() {
			val now = LocalDate.now()
			val between = Period.between(dob, now)
			return between.years
		}
	init {
		hash = name.length + age + Instant.now().toEpochMilli();
	}

	override fun toString(): String =
		"${name} is ${age} years old and has private hash ${hash}"
}