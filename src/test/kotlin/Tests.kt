import io.github.com6235.lang
import io.github.com6235.main
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.test.expect

class Tests {
    @Test
    fun testMain() {
//        val stream = ByteArrayOutputStream()
//        System.setOut(PrintStream(stream))
        main(arrayOf("./src/test/resources/Selected Packs1.txt"))
    }

    @Test
    fun testMainWithBadPacks() {
        val stream = ByteArrayOutputStream()
        System.setOut(PrintStream(stream))
        main(arrayOf("./src/test/resources/Selected Packs2.txt"))
        expect(true) { stream.toString(Charset.defaultCharset()).contains("Fullbright") }
    }
}