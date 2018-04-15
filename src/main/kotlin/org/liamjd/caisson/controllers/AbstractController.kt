package org.liamjd.caisson.controllers

import org.liamjd.spark.templates.thymeleaf.ThymeleafTemplateEngine
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response
import spark.Session
import spark.kotlin.after
import spark.kotlin.before
import spark.kotlin.notFound

/**
 * TODO: decouple the thymeleaf engine
 * Base class for all controllers. It defines a logger and the rendering engine, plus before and after filters,
 * notFound routes, and other common routes
 */
abstract class AbstractController(path: String) : CaissonController {

	override val FLASH_COUNT_MAX = 3

	internal open val logger = LoggerFactory.getLogger(AbstractController::class.java)
	// TODO: make this engine a bit more generic
	protected val engine: ThymeleafTemplateEngine = ThymeleafTemplateEngine()

	var session: Session? = null
	open lateinit var path: String
	val controllerHome: String = path + "/"
	val model: MutableMap<String, Any> = hashMapOf<String, Any>()

	init {
		this.path = path
		// put before and after filters here
		before {
			session = request.session(true)
			logger.info(request.pathInfo())

			// update all flash item counts
			val flashAttr: MutableMap<String, Any>? = request.session().attribute("flash")
			if (flashAttr != null && flashAttr.isNotEmpty()) {
				flashAttr.forEach {
					model.put(it.key, it.value) // store the flashed thing on the model
					val flashKeyCount: Int? = request.session().attribute(getFlashKeyCountName(it.key))
					if (flashKeyCount == null) {
						request.session().attribute(getFlashKeyCountName(it.key), 1)
					} else {
						request.session().attribute(getFlashKeyCountName(it.key), (flashKeyCount + 1))
					}
				}
			}
		}

		after {
			val flashAttr: MutableMap<String, Any>? = request.session().attribute("flash")
			if (flashAttr != null && flashAttr.isNotEmpty()) {
				flashAttr.forEach {
					val flashKeyCount: Int? = request.session().attribute(getFlashKeyCountName(it.key))
					if (flashKeyCount != null && flashKeyCount > FLASH_COUNT_MAX) {
						clearFlashForKey(request, it.key)
						model.remove(it.key)
					}
				}
			}
		}

		notFound { "404 not found?" }

	}

	override fun flash(request: Request, response: Response, key: String, value: Any) {
		val keyValueMap: MutableMap<String, Any> = mutableMapOf<String, Any>()
		keyValueMap.put(key, value)
		response.body("")
		response.status(200) // success
		request.session().attribute("flash", keyValueMap)
		request.session().attribute(getFlashKeyCountName(key), 1)
	}

	private fun clearFlashForKey(request: Request, key: String) {
		val flashAttr: MutableMap<String, Any>? = request.session().attribute("flash")
		if (flashAttr != null) {
			if (flashAttr.containsKey(key)) {
				flashAttr.remove(key)
			}
			request.session().attribute(getFlashKeyCountName(key), null)
		}
	}

	override fun emptyFlash(request: Request) {
		request.session().attribute("flash", null)
	}

	fun getFlashKeyCountName(key: String): String {
		return "flash" + key + "Count"
	}

	fun debugParams(request: Request) {
		request.params().forEach {
			logger.debug("Param ${it.key} -> ${it.value}")
		}
	}

	fun debugQueryParams(request: Request) {
		request.queryParams().forEach {
			logger.debug("QueryParam ${it}")
		}
	}

	fun debugSplat(request: Request) {
		for (s in request.splat()) {
			logger.info("Splat -> $s")
		}
	}

	fun debugModel() {
		logger.info("MODEL: " + model.toString())
	}

	fun debugFlash(request: Request) {
		logger.info("FLASH: " + request.session().attribute("flash"))
	}

	fun debugRequestMap(request: Request) {
		request.toMap.forEach {
			println("\t$it")
		}
	}

	fun debugQueryMap(request: Request) {
		request.queryMap().toMap().forEach {
			print("\tqueryMap to map ${it.key} -> ${it.value}")
			it.value.forEach {
				print("\t\t ${it}")
			}
			println()
		}
	}

}