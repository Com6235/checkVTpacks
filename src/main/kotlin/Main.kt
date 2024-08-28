package io.github.com6235

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
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
        "txt" -> parseTxt(path)
        // TODO fix zip archives
        "zip" -> {
            val pwd = Path(System.getProperty("user.dir"))
            unzip(path.toFile(), pwd)

            val file = Path(pwd.absolutePathString(), "Selected Packs.txt")
            parseTxt(file)

        }
        else -> {
            println(translated("file-not-understood"))
            null
        }
    }

    if (!out.isNullOrEmpty()) {
        println(translated("bad-packs", out.joinToString { " - $it \n" }))
    } else if (out != null && out.isEmpty()) {
        println(translated("no-bad-packs"))
    }
}

fun parseTxt(path: Path): List<String>? {
    val lines = path.readLines()
    if (lines[0] != "Vanilla Tweaks Resource Pack") {
        println(translated("bad-txt", path.absolutePathString()))
        return listOf()
    }

    val yaml = Yaml(configuration = YamlConfiguration(strictMode = false)).decodeFromString(SelectedPacks.serializer(), lines.subList(1, lines.size).joinToString("\n").replace("\t", "  - "))
    return yaml.Packs?.filter { it in badSelected.badpacks }
}

fun unzip(zipFile: File, output: Path) {
    val buffer = ByteArray(16384)
    val zis = ZipInputStream(zipFile.inputStream())
    var zipEntry = zis.nextEntry
    while (zipEntry != null) {
        if (zipEntry.name != "Selected Packs.txt") return
        val newFile = File(output.toFile(), zipEntry.name)
        if (zipEntry.isDirectory) {
            continue
        } else {
            val fos = FileOutputStream(newFile)
            var len: Int
            while ((zis.read(buffer).also { len = it }) > 0) {
                fos.write(buffer, 0, len)
            }
            fos.close()
        }
        zipEntry = zis.nextEntry
    }

    zis.closeEntry()
    zis.close()
}
