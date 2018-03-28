import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class SparkConverter(val converterClass: KClass<out Converter>)

typealias IsValid = Pair<Boolean, Map<String, String>?>

interface Valid {
	fun valid(): IsValid
}

class FakeRequest(val params: Map<String, String>) {
	override fun toString(): String {
		val sBuilder: StringBuilder = StringBuilder()
		for (k in params) {
			sBuilder.append("[" + k.key + "->" + k.value + "] ")
		}
		return sBuilder.toString()
	}
}

interface Converter {
	fun convert(from: String): Any?
}

class MyTimeIntConverter : Converter {
	override fun convert(hoursString: String): Int {
		return hoursString.toInt()
	}
}

class LocalDateConverter : Converter {
	override fun convert(from: String): LocalDate {
		val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
		return LocalDate.parse(from, dateTimeFormatter)
	}
}

data class PersonBirthday(val name: String, @SparkConverter(converterClass = LocalDateConverter::class) val dob: LocalDate)

data class MyTime(@SparkConverter(converterClass = MyTimeIntConverter::class) val hours: Int,
				  @SparkConverter(converterClass = MyTimeIntConverter::class) val minutes: Int) : Valid {

	override fun valid(): IsValid {
		var errors = mutableMapOf<String, String>()
		var valid = true
		if (!(hours in 0..23)) {
			errors.put("hours", "hours must be in range 0 to 23")
			valid = false
		}
		if (!(minutes in 0..59)) {
			errors.put("minutes", "minutes must be in range 0 to 59")
			valid = false
		}
		return IsValid(valid, errors)
	}
}

data class MyUser(val name: String, val email: String) : Valid {
	constructor(n: String) : this(n, n + "@" + n + ".com")

	override fun valid(): IsValid {
		val errors = mutableMapOf<String, String>()

		if (name.isEmpty() || email.isEmpty()) {
			errors.put("name", "must not be empty")
			errors.put("email", "must not be empty")
			return IsValid(false, errors)
		}
		return IsValid(true, null)

	}
}

class FormField(val key: String, val value: String?)

class Form(val req: FakeRequest, val modelClass: KClass<*>) {
	val fields = mutableSetOf<FormField>()
	var valid: Boolean = true
	lateinit var modelObject: Any

	init {

//		println("All about my class ${modelClass.simpleName}")
//		val isData = modelClass.isData
//		val numOfConstructors = modelClass.constructors.size
		val primaryConstructor = modelClass.primaryConstructor
//		val declaredMemberProperties = modelClass.declaredMemberProperties

		val consParams: MutableMap<KParameter, Any?> = mutableMapOf()
		if (primaryConstructor != null) {
			for (constructorKParam in primaryConstructor.parameters) {
				val inputValue: String = req.params.get(constructorKParam.name) ?: "" // I don't ever expect a null here
//				println("\t primaryConstructor parameter: ${constructorKParam} with type jvmErasure ${constructorKParam.type.jvmErasure}")
//				println("\t annotations are: " + constructorKParam.annotations)

				var finalValue: Any? = null

				val sparkConverterAnnotation = constructorKParam.findAnnotation<SparkConverter>()
				if (sparkConverterAnnotation != null) {
					println("\t\t my converter class: " + sparkConverterAnnotation.converterClass)
					val converterClass = Class.forName(sparkConverterAnnotation.converterClass.qualifiedName).newInstance().javaClass.kotlin
					if (converterClass != null) {
						val converterFunction = converterClass.declaredMemberFunctions.find { it.name.equals("convert") }
						if (converterFunction != null) {
//							println("\t\t converterClass ${converterClass}")
//							println("\t\t converter function ${converterFunction}")
//							println("\t\t function parameters; ${converterFunction.parameters}")

							/*val converterParams: MutableMap<KParameter, String> = mutableMapOf()
							converterFunction.parameters.forEach {
								println("\t\t\tconverterFunction param: $it")
							}*/
							finalValue = converterFunction.call(converterClass.createInstance(), inputValue)
						}
					}
				} else {
					// we don't have a converter, so let's squash the String into whatever we've got
					// but better to provide some default converters for the basic types
					finalValue = inputValue
				}

//				println("our final value is $finalValue")
				consParams.put(constructorKParam, finalValue)

			}
			modelObject = primaryConstructor.callBy(consParams)
		}

	}

	fun get() = modelObject

	override fun toString(): String {
		val sb: StringBuilder = StringBuilder()
		for (f in fields) {
			sb.append(f.key).append(" -> ").append(f.value).append(", ")
		}
		sb.append(" (valid $valid)")
		return sb.toString()
	}

}


fun main(args: Array<String>) {

	val stringOnly = false

	if (!stringOnly) {
		val nameParam = "hours" to "24"
		val ageParam = "minutes" to "60"
		val paramMap = mutableMapOf<String, String>()
		paramMap.put(nameParam.first, nameParam.second)
		paramMap.put(ageParam.first, ageParam.second)
		println("Constructing myFakeRequest with ${paramMap}")
		val myFakeRequest = FakeRequest(params = paramMap)

		val myTimeForm = Form(myFakeRequest, MyTime::class)

		println("myTimeForm is valid: ${myTimeForm.valid}")
		val time: MyTime = myTimeForm.get() as MyTime
		println("MyTime is $time")
		println("-------------------------------------------")

		val birthdayParam = "name" to "Liam"
		val birthdayDateParam = "dob" to "06/04/1979"
		paramMap.clear()
		paramMap.put(birthdayParam.first, birthdayParam.second)
		paramMap.put(birthdayDateParam.first, birthdayDateParam.second)
		println("Constructing my fake birthday request with ${paramMap}")
		val myFakeBirthdayRequest = FakeRequest(paramMap)
		val myBirthdayForm = Form(myFakeBirthdayRequest, PersonBirthday::class)
		val birthday = myBirthdayForm.get() as PersonBirthday
		println("Birthday is $birthday")

		println("-------------------------------------------")
	}

	val userParamMap = mutableMapOf(Pair("name", "liam"), Pair("email", "liam@davison.com"))
	val fakeUserRequest = FakeRequest(params = userParamMap)
	val myUserForm = Form(fakeUserRequest, MyUser::class)
	val user = myUserForm.get() as MyUser
	println("User is $user")




	val a: Int = 10000
	val d: Double = 100.00
	val f: Float = 100.00f
	val l: Long = 1000000004
	val s: Short = 10
	val b: Byte = 1
	val c: Char = 'c'
	val bool: Boolean = true
	val string: String ="string"
	// arrays
	// collections
	// ranges


	class Something(string: String, integer: Int, list: List<String>, other: Long)
	fun wibble(args: Array<String>) {
		Something::class.primaryConstructor?.parameters?.forEach {
			when (it.type.jvmErasure) {
				String::class -> println("I'm a String")
				Int::class -> println("I'm an Int")
				List::class -> println("I'm a List")
				else -> println("I don't what I am")
			}
		}
	}


}
