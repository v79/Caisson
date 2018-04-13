package org.liamjd.caisson.test.webforms

import org.liamjd.caisson.annotations.CConverter
import org.liamjd.caisson.convertors.Converter
import org.liamjd.caisson.models.CaissonMultipartContent
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * A bunch of model classes used in testing
 */

class SimpleDateConverter : Converter {
	override fun convert(from: String): Date? {
		val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
		try {
			return sdf.parse(from)
		} catch (e: ParseException) {
			return null
		}
	}
}

/**
 * This will return a date even if no value is passed
 */
class AssumingDateConverter : Converter {
	override fun convert(from: String): Date? {
		if(from.isNullOrEmpty()) {
			return Date()
		}
		val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
		try {
			return sdf.parse(from)
		} catch (e: ParseException) {
			return null
		}
	}
}

data class Person(val name: String, @CConverter(converterClass = SimpleDateConverter::class) val dob: Date)
data class PersonWithDefaultBirthday(val name: String, @CConverter(converterClass = AssumingDateConverter::class) val dob: Date)

data class Photograph(val picture: CaissonMultipartContent)

data class LegalDocuments(val docs: List<CaissonMultipartContent>)

data class UnusedFieldsTest(val used: String, val usedNumber: Int, val unusedInt: Int)