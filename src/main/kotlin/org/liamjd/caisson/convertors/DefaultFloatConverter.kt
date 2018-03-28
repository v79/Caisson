package org.liamjd.caisson.convertors

class DefaultFloatConverter : Converter {
	override fun convert(from: String): Float {
		try {
			return from.toFloat()
		} catch (nfe: NumberFormatException) {
			return 0.0F
		}
	}
}