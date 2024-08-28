package io.github.com6235

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
data class SelectedPacks(val Version: String, val Packs: List<String>, val `Combined packs`: List<String>)
@Serializable
data class BadPacks(val badPacks: List<String>)

val lang = ResourceBundle.getBundle("lang")
val badSelected by lazy {
    Json.decodeFromString<BadPacks>(
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("https://github.com/com6235/checkVTpacks/"))
                    .GET()
                    .build(),
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
        "zip" -> {
            val pwd = Path(System.getenv("user.dir"))
            unzip(pwd.toFile(), Path(System.getenv("user.dir")))

            val file = Path(pwd.absolutePathString(), "Selected Packs.txt")
            parseTxt(file)

        }
        else -> {
            println(translated("file-not-understood"))
            null
        }
    }

    if (out != null) {
        println(translated("bad-packs", out.joinToString { " - $it \n" }))
    }
}

fun parseTxt(path: Path): List<String> {
    val lines = path.readLines()
    if (lines[0] != "Vanilla Tweaks Resource Pack") {
        println(translated("bad-txt", path.absolutePathString()))
        return listOf()
    }

    val yaml = Yaml.default.decodeFromString(SelectedPacks.serializer(), lines.subList(1, lines.size).joinToString("\n").replace("\t", "  - "))
    return yaml.Packs.filter { it in badSelected.badPacks }
}

fun unzip(zipFile: File, output: Path) {
    val buffer = ByteArray(1024)
    val zis = ZipInputStream(zipFile.inputStream())
    var zipEntry = zis.nextEntry
    while (zipEntry != null) {
        val newFile = File(output.toFile(), zipEntry.name)
        if (zipEntry.isDirectory) {
            if (!newFile.isDirectory && !newFile.mkdirs()) {
                throw IOException("Failed to create directory $newFile")
            }
        } else {
            val parent = newFile.parentFile
            if (!parent.isDirectory && !parent.mkdirs()) {
                throw IOException("Failed to create directory $parent")
            }

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
