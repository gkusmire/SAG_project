package pl.sag

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.nio.file.Files

val gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd HH:mm")
    .setPrettyPrinting()
    .create()

inline fun <reified T> parseJsonFile(filename: String): T {
    val resource = object {}.javaClass.classLoader.getResource(filename)
    return resource.let {
        val json = String(Files.readAllBytes(File(it.file).toPath()))

        gson.fromJson(json, T::class.java)
    }
}

inline fun <reified T> saveToJsonFile(filename: String, obj: T) {
    val json = gson.toJson(obj)
    val file = File(filename)
    file.writeText(json)
}

inline fun <reified T> fromJSON(json: String): T =
    gson.fromJson(json, T::class.java)

inline fun <reified T> toJSON(value: T) = gson.toJson(value)