package org.liamjd.caisson.views.templates

import spark.ModelAndView
import java.util.*

/**
 * Any HTML engine must implement all of these methods
 */
interface CaissonEngine {

	fun render(modelAndView: ModelAndView) :String

	fun render(modelAndView: ModelAndView, fragment: String) :String

	fun render(modelAndView: ModelAndView, locale: Locale) :String

	fun render(modelAndView: ModelAndView, fragments: Set<String>, locale: Locale) :String
}