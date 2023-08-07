# Caisson
An MVC library for the Spark-Kotlin project

# PROJECT CLOSED

**Caisson** is an attempt introduce some basic MVC functionality to the Spark-Kotlin project. It is inspired by SpringMVC, but with a much smaller and simpler scope. Step one is to introduce *model binding* and *converters* to Spark-Kotlin, using annotations and reflection to construct instances of Kotlin classes from a Spark-Kotlin request parameter map.

Users of the library will be able to bind any model class from a spark-kotlin request, mapping the request param names to the constructor parameters for the model. For instance, to bind the `Person` model from the spark-kotlin request, simply call the `bind<>()` function on the `request`, supplying the model class in the generics diamonds.

```kotlin
post("/addPerson") {
  val person = request.bind<Person>()
  println("person is ${person?.name}, born on ${person?.date}")
}
```

This assumes the following `Person` model and a custom converter class (annotated with `@CConverter`) to parse the request parameter string. Note that only fields defined in the class's primary constructor can be bound; you are welcome to add additional fields or calculated values in the class's `init { }` block. A `data class` is an obvious choice for a model, but ordinary classes work too.

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

## File uploads

To implement file uploading, your model class must contain a field of type `CaissonMultipartContent` (or a List of these). **Caisson** will store each of the uploaded files' bytestreams in the `CaissonMultipartContent` object:

```kotlin
data class MyFiles(val upload: List<MultipartCaissonContent>)
```

And use these classes with Spark-Kotlin's normal request object. You must specify the names of the HTML input components used.

```HTML
<form name="fileUpload">
  <input type="file" name="upload">
  <input type="file" name="upload">
</form>
```

```kotlin
val myFiles = request.bind<MyFiles>(arrayListOf("upload"))
```

Alternatively, if each input component has a different name, supply a List of their names as the third parameter.

```HTML
<form name="fileUpload">
  <input type="file" name="drivingLicense">
  <input type="file" name="passport">
</form>
```

```kotlin
val myFiles = request.bind<MyFiles>(arrayListOf("drivingLicense","passport"))
```

This project is in the very earliest stages, and I have a lot to learn about Kotlin, HTTP requests, Reflection and more besides. So don't even think of using it :). There is a corresponding demo project hosted on github called [https://github.com/v79/spark-caisson-integration](spark-caisson-integration) which implements all the features of Caisson and may prove useful while I'm still working on the documentation.
