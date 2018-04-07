package org.liamjd.caisson.models

import java.io.InputStream

data class CaissonMultipartContent(val contentType: String, val size: Long, val stream: InputStream, val originalFileName: String)