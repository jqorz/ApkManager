import java.io.DataOutputStream
import java.lang.Exception
import java.lang.StringBuilder

object ShellUtils {

    fun sh(cmd: String) = execCmd("sh", cmd)

    fun su(cmd: String) = execCmd("su", cmd)

    fun isRootAccess() = execCmd("su", "echo test", false).status == 0

    @JvmOverloads
    fun execCmd(
        program: String,
        input: String,
        needResult: Boolean = true
    ): ExecResult {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(program)
            DataOutputStream(process.outputStream)
                .use {
                    it.write(input.toByteArray())
                    it.writeBytes("\n")
                    it.flush()
                    it.writeBytes("exit\n")
                    it.flush()
                    if (!needResult) {
                        return NO_RESULT
                    }
                    val resultCode = process.waitFor()
                    val output = process.inputStream
                        .bufferedReader()
                        .useLines { lines ->
                            val results = StringBuilder()
                            lines.forEach { results.append(it) }
                            results.toString()
                        }
                    val error = process.errorStream
                        .bufferedReader()
                        .useLines { lines ->
                            val results = StringBuilder()
                            lines.forEach { results.append(it) }
                            results.toString()
                        }
                    return ExecResult(output, error, resultCode)

                }

        } catch (e: Exception) {
            e.printStackTrace()
            return ExecResult("", e.toString(), -1)
        } finally {
            process?.destroy()
        }
    }

    val NO_RESULT = ExecResult("", "", -1)

    class ExecResult(val output: String?, val error: String?, val status: Int) {
        override fun toString(): String {
            val stringBuilder = StringBuilder()
            stringBuilder.append("ret:").append(status).append('\n')
            stringBuilder.append("out{\n").append(output).append("}\n")
            stringBuilder.append("err{\n").append(error).append("}\n")
            return stringBuilder.toString()
        }
    }
}
