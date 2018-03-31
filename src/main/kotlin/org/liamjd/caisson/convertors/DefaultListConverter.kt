package org.liamjd.caisson.convertors

/**
 * The default list converter does nothing other than return the input. But it's a start?
 */
class DefaultListConverter {
	fun convert(from: List<String>): List<String>? {
		return from
	}
}