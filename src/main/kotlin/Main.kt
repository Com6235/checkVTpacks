package io.github.com6235

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.readLines

@Serializable
data class SelectedPacks(
    val Version: String,
    val Packs: List<String>? = listOf(),
    val `Combined packs`: List<String>? = listOf()
)

@Serializable
data class BadPacks(val badpacks: List<String>)

val lang = ResourceBundle.getBundle("lang")

val badSelected by lazy {
    Json.decodeFromString<BadPacks>(
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                    URI.create("https://raw.githubusercontent.com/Com6235/checkVTpacks/meta/badpacks.json")
                ).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            ).body()
    )

}

fun translated(key: String, vararg args: Any): String = lang.getString(key).format(*args)

fun main(args: Array<String>) {
    if (args.isEmpty()) return println(translated("no-args"))
    val path = try { Path(args[0]) } catch (e: Exception) { return println(translated("arg-not-path", args[0])) }

    val out = when (path.extension.lowercase()) {
        "txt" -> parseTxt(path.readLines())
        "zip" -> parseTxt(unzip(path.toFile()))
        else -> null
    }?.distinct()

    if (!out.isNullOrEmpty()) {
        println(translated("bad-packs", out.joinToString("") { " - $it \n" }))
    } else if (out != null && out.isEmpty()) {
        println(translated("no-bad-packs"))
    } else if (out == null) {
        println(translated("file-not-understood"))
    }
}

fun parseTxt(lines: List<String>): List<String>? {
    if (lines[0] != "Vanilla Tweaks Resource Pack") {
        println(translated("bad-txt"))
        return listOf()
    }

    val yaml = Yaml(configuration = YamlConfiguration(strictMode = false)).decodeFromString(SelectedPacks.serializer(), lines.subList(1, lines.size).joinToString("\n").replace("\t", "  - "))

    val badCombined = yaml.`Combined packs`?.map { it.split("+") }
        ?.map { it.filter { it in badSelected.badpacks } }
        ?.filter { it.isNotEmpty() }
        ?.reduceOrNull { a, b -> a + b }
        ?: listOf()

    return yaml.Packs?.filter { it in badSelected.badpacks }?.add(badCombined)
}

fun <T> List<T>.add(other: List<T>): List<T> {
    val list = this.toMutableList()
    return list + other
}

fun unzip(zipFile: File): List<String> {
    val zip = ZipFile(zipFile)
    val packs = zip.getEntry("Selected Packs.txt").let { zip.getInputStream(it) }.reader().use { it.readLines() }
    zip.close()
    return packs
}
