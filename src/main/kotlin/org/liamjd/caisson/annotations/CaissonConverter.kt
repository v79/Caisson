package org.liamjd.caisson.annotations

import kotlin.reflect.KClass
import org.liamjd.caisson.convertors.Converter

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CConverter(val converterClass: KClass<out Converter>)