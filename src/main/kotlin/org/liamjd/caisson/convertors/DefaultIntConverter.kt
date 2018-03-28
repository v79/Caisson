package org.liamjd.caisson.convertors

class DefaultIntConverter : Converter {
	override fun convert(from: String): Int {
		try {
			return from.toInt()
		} catch (nfe: NumberFormatException) {
			return 0
		}
	}
}
