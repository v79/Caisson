package org.liamjd.caisson.test.webforms

fun main(args: Array<String>) {
	val longString: String = "1551441414479"
	try {
		val asLong: Long = longString.toLong()

		println(asLong)
	} catch (nfe: NumberFormatException) {
		println("sad")
	}
}