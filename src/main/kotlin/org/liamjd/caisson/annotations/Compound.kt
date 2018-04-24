package org.liamjd.caisson.annotations


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Compound(val prefix: String = "", val separator: String = ".")