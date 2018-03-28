package org.liamjd.caisson.convertors

class DefaultLongConverter : Converter {
	override fun convert(from: String): Long {
		try {
			return from.toLong()
		} catch (nfe: NumberFormatException) {
			return 0L
		}
	}
}
