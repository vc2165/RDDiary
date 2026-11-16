package com.vinay.rddiary

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vinay.rddiary.data.Book
import com.vinay.rddiary.data.parseBook
import com.vinay.rddiary.data.toJson
import com.vinay.rddiary.ui.RdApp
import com.vinay.rddiary.ui.RdTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

enum class Tab { HOME, ACCOUNTS, TODAY, SETTINGS }

/** Which filtered list to show (from tapping a pie slice or a card). */
enum class Filter { ALL, PENDING, ADVANCE, UP_TO_DATE, MATURING, PAID_THIS_MONTH, NOT_PAID_THIS_MONTH, DONE }

sealed interface Route {
    data class Detail(val id: String) : Route
    data class Pay(val accountId: String, val paymentId: String? = null) : Route
    data class EditAccount(val id: String?) : Route
    data class Filtered(val filter: Filter) : Route
    data object Import : Route
}

class AppState(private val ctx: Context, private val activity: ComponentActivity) {
    private val file = File(ctx.filesDir, "rddiary.json")
    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var book by mutableStateOf(load())
        private set
    var tab by mutableStateOf(Tab.HOME)
    val stack = mutableStateListOf<Route>()
    val snackbar = SnackbarHostState()
    var bigText by mutableStateOf(prefs.getBoolean("bigText", false))
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init { T.te = prefs.getBoolean("telugu", true) }

    private fun load(): Book = try {
        if (file.exists()) parseBook(JSONObject(file.readText())) else Book()
    } catch (e: Exception) {
        try { file.copyTo(File(ctx.filesDir, "rddiary-broken-${System.currentTimeMillis()}.json")) } catch (_: Exception) {}
        Book()
    }

    fun update(f: (Book) -> Book) {
        book = f(book)
        val tmp = File(ctx.filesDir, "rddiary.json.tmp")
        tmp.writeText(book.toJson().toString())
        if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
    }

    fun replaceAll(b: Book) = update { b }

    fun setTelugu(on: Boolean) { T.te = on; prefs.edit().putBoolean("telugu", on).apply() }
    fun setBig(on: Boolean) { bigText = on; prefs.edit().putBoolean("bigText", on).apply() }

    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1) }

    fun toast(msg: String, action: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(msg, action, duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun tick(confirm: Boolean = false) {
        val c = if (confirm && Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
        activity.window.decorView.performHapticFeedback(c)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext, this)
        setContent { RdTheme(big = state.bigText) { RdApp(state) } }
    }
}
