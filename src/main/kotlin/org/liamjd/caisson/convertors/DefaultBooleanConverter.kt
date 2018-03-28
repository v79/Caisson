package org.liamjd.caisson.convertors

class DefaultBooleanConverter : Converter {
	override fun convert(from: String): Boolean {
		return from.toBoolean()
	}
}