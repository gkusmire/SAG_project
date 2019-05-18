package pl.sag

import com.google.gson.Gson
import java.io.File
import java.nio.file.Files

val gson = Gson()

inline fun <reified T> parseJsonFile(filename: String): T? {
    val resource = object {}.javaClass.classLoader.getResource(filename)
    return resource?.let {
        val json = String(Files.readAllBytes(File(it.file).toPath()))

        gson.fromJson(json, T::class.java)
    }
}