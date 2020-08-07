package bmw.ximalaya.test

import android.util.Log

object LijhLog {
    var TAG = "lijh>"
    var cause: String? = null
    private fun getStackTraceString(cause: String?, trace: Boolean = true): String {
        return cause ?: Thread.currentThread().stackTrace
            .filterIndexed { index, stackTraceElement -> stackTraceElement.className != javaClass.name && index > 2 }
            .filterIndexed { index, _ -> trace || index == 0 }
            .map { stackTraceElement -> stackTraceElement.toString() }
            .reduce { acc, s -> "${acc}\n\tat $s" }
    }

    fun d(tag: String? = null, msg: String?, trace: Boolean = false) {
        Log.d(
            tag ?: TAG,
            "${if (msg?.length ?: 0 > 0) "$msg\n" else ""}${getStackTraceString(cause, trace)}"
        )
    }

    fun d(msg: String? = null, trace: Boolean = false) {
        d(null, msg, trace)
    }

    fun i(tag: String? = null, msg: String?, trace: Boolean = false) {
        Log.i(
            tag ?: TAG,
            "${if (msg?.length ?: 0 > 0) "$msg\n" else ""}${getStackTraceString(cause, trace)}"
        )
    }

    fun i(msg: String? = null, trace: Boolean = false) {
        i(null, msg, trace)
    }

    fun w(tag: String? = null, msg: String?, trace: Boolean = false) {
        Log.w(
            tag ?: TAG,
            "${if (msg?.length ?: 0 > 0) "$msg\n" else ""}${getStackTraceString(cause, trace)}"
        )
    }

    fun w(msg: String? = null, trace: Boolean = false) {
        w(null, msg, trace)
    }

    fun v(tag: String? = null, msg: String?, trace: Boolean = false) {
        Log.v(
            tag ?: TAG,
            "${if (msg?.length ?: 0 > 0) "$msg\n" else ""}${getStackTraceString(cause, trace)}"
        )
    }

    fun v(msg: String? = null, trace: Boolean = false) {
        v(null, msg, trace)
    }

    fun e(tag: String? = null, msg: String?, trace: Boolean = false) {
        Log.e(
            tag ?: TAG,
            "${if (msg?.length ?: 0 > 0) "$msg\n" else ""}${getStackTraceString(cause, trace)}"
        )
    }

    fun e(msg: String? = null, trace: Boolean = false) {
        e(null, msg, trace)
    }

}