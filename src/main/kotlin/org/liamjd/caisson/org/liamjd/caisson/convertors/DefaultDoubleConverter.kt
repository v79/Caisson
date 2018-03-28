package org.liamjd.caisson.org.liamjd.caisson.convertors

class DefaultDoubleConverter : Converter {
	override fun convert(from: String): Double {
		try {
			return from.toDouble()
		} catch (nfe: NumberFormatException) {
			return 0.0
		}
	}
}
