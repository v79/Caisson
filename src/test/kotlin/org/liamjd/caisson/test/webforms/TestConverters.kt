package org.liamjd.caisson.test.webforms

import org.liamjd.caisson.convertors.Converter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

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

/**
 * Let's move with the future and use LocalDates instead of Dates.
 */
class LocalDateConverter : Converter {
	override fun convert(from: String): LocalDate? {
		val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
		try {
			return LocalDate.parse(from,dateTimeFormat)
		} catch(e: java.time.format.DateTimeParseException) {
			return LocalDate.now()
		}
	}
}