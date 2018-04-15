package org.liamjd.caisson.controllers

import org.liamjd.caisson.views.templates.CaissonEngine
import org.liamjd.caisson.webforms.RequestParams
import spark.Request
import spark.Response
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

interface CaissonController {

	val FLASH_COUNT_MAX: Int
	var engine: CaissonEngine

	fun flash(request: Request, response: Response, key: String, value: Any)
	fun emptyFlash(request: Request)

	/**
	 * Extension function to convert request.queryParams into a nice map
	 */
	val Request.toMap: RequestParams
		get() {
			val map = mutableMapOf<String,String>()
			this.queryParams().forEach {
				if(map.contains(it)) {
					map.get(it)
				} else {
					map.put(it, decode(this.queryParams(it)))
				}
			}
			return map as RequestParams
		}

	/**
	 * URLDecode a given string
	 */
	private fun decode(s: String, enc: String = "UTF-8"): String {
		try {
			return URLDecoder.decode(s, enc)
		} catch (e: UnsupportedEncodingException) {
			e.printStackTrace()
		}

		return ""
	}

}

