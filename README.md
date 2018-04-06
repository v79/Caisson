# Caisson
An MVC Framework for the Spark-Kotlin project

**Caisson** is an attempt introduce some basic MVC functionality to the Spark-Kotlin project. It is inspired by SpringMVC, but with a much smaller and simpler scope. Step one is to introduce *model binding* and *converters* to Spark-Kotlin, using annotations and reflection to construct instances of Kotlin classes from a Spark-Kotlin request parameter map.

Users of the library will be able to write code something like this:

```kotlin
data class Person(val name:String, @CConverter(converterClass = SimpleDateConverter::class) val date: Date)

class SimpleDateConverter : Converter {
	override fun convert(from: String): Date? {
		val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
		try {
			return sdf.parse(from)
		} catch (e: ParseException) {
			return Date()
		}
	}
}
```

And use these classes with Spark-Kotlin's normal request object:

```kotlin
post("/addPerson") {
  val person = Form(request, Person::class).get() as Person
  println("person is ${person.name}, born on ${person.date}")
  // no longer do I need to parse the request.params map
}
```

This project is in the very earliest stages, and I have a lot to learn about Kotlin, HTTP requests, Reflection and more besides. So don't even think of using it :)
