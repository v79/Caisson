package org.liamjd.caisson.server

import org.liamjd.caisson.annotations.CController
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.slf4j.LoggerFactory
import spark.Spark
import spark.kotlin.port
import spark.kotlin.staticFiles
import spark.servlet.SparkApplication
import java.time.LocalDate

class CaissonServer(val packageName: String, val portNumber: Int?) : SparkApplication {
	private val logger = LoggerFactory.getLogger(CaissonServer::class.java)
	private val systemPort: String? = System.getProperty("server.port")

	override fun init() {

		if(portNumber != null) {
			port(number = portNumber)
		} else {
			port(number = systemPort?.toInt() ?: 4569)
		}
		// this part doesn't work here - needs to be in the primary applicaiton
		staticFiles.location("/public")

	}

	fun start() {
		val reflections = Reflections(packageName, MethodAnnotationsScanner(), TypeAnnotationsScanner(), SubTypesScanner())
		val controllers = reflections.getTypesAnnotatedWith(CController::class.java)
		controllers.forEach {
			logger.info("Instantiating controller " + it.simpleName)
			it.newInstance()
		}

		displayStartupMessage()
	}


	private fun displayStartupMessage() {
		logger.info("=============================================================")
		logger.info("Kotlin Spark Route Tester Started")
		logger.info("Date: " + LocalDate.now().toString())
		logger.info("OS: " + System.getProperty("os.name"))
		logger.info("Port: " + Spark.port())
		logger.info("JDBC URL: " + System.getenv("JDBC_DATABASE_URL"))
		logger.info("=============================================================")
	}
}