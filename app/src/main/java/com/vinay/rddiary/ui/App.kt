@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.vinay.rddiary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinay.rddiary.AppState
import com.vinay.rddiary.Filter
import com.vinay.rddiary.Route
import com.vinay.rddiary.T
import com.vinay.rddiary.Tab
import com.vinay.rddiary.data.AccState
import com.vinay.rddiary.data.Status
import com.vinay.rddiary.data.inr
import com.vinay.rddiary.data.states
import com.vinay.rddiary.data.summary
import com.vinay.rddiary.data.thisMonth
import java.time.format.TextStyle
import java.util.Locale

var focusSearch by mutableStateOf(false)

@Composable
fun RdApp(s: AppState) {
    BackHandler(enabled = s.stack.isNotEmpty()) { s.pop() }
    BackHandler(enabled = s.stack.isEmpty() && s.tab != Tab.HOME) { s.tab = Tab.HOME }
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(Modifier.fillMaxSize()) {
            Tabs(s)
            AnimatedContent(
                targetState = s.stack.size to s.stack.lastOrNull(),
                transitionSpec = {
                    if (targetState.first > initialState.first)
                        ((slideInVertically(tween(260)) { it / 8 } + fadeIn(tween(200))) togetherWith ExitTransition.KeepUntilTransitionsFinished)
                            .apply { targetContentZIndex = 1f }
                    else
                        (EnterTransition.None togetherWith (slideOutVertically(tween(200)) { it / 8 } + fadeOut(tween(160))))
                            .apply { targetContentZIndex = -1f }
                },
                contentKey = { it.first },
                label = "routes",
            ) { (_, r) ->
                if (r != null) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (r) {
                        is Route.Detail -> DetailScreen(s, r.id)
                        is Route.Pay -> PayScreen(s, r.accountId, r.paymentId)
                        is Route.EditAccount -> EditAccountScreen(s, r.id)
                        is Route.Filtered -> FilteredScreen(s, r.filter)
                        Route.Import -> ImportScreen(s)
                    }
                } else Box(Modifier)
            }
        }
    }
}

@Composable
private fun Tabs(s: AppState) {
    Scaffold(
        snackbarHost = { SnackbarHost(s.snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                listOf(
                    Triple(Tab.HOME, T["home"], Icons.Outlined.Home), Triple(Tab.ACCOUNTS, T["accounts"], Icons.Outlined.People),
                    Triple(Tab.TODAY, T["today"], Icons.Outlined.Today), Triple(Tab.SETTINGS, T["more"], Icons.Outlined.Settings),
                ).forEach { (t, l, ic) ->
                    NavigationBarItem(selected = s.tab == t, onClick = { s.tab = t }, icon = { Icon(ic, null) }, label = { Text(l) })
                }
            }
        },
        floatingActionButton = {
            if (s.tab == Tab.ACCOUNTS) ExtendedFloatingActionButton(
                text = { Text(T["addAccount"]) }, icon = { Icon(Icons.Filled.Add, null) },
                onClick = { s.push(Route.EditAccount(null)) }, containerColor = Brand, contentColor = Color.White,
            )
        },
    ) { pad ->
        Box(Modifier.padding(bottom = pad.calculateBottomPadding())) {
            AnimatedContent(s.tab, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "tabs") { t ->
                when (t) {
                    Tab.HOME -> HomeScreen(s)
                    Tab.ACCOUNTS -> AccountsScreen(s)
                    Tab.TODAY -> TodayScreen(s)
                    Tab.SETTINGS -> SettingsScreen(s)
                }
            }
        }
    }
}

/* ---------------- Home ---------------- */

@Composable
fun HomeScreen(s: AppState) {
    val b = s.book
    val sm = remember(b) { b.summary() }
    val sts = remember(b) { b.states() }
    val now = thisMonth()
    val maturing = remember(sts) { sts.filter { it.status != Status.CLOSED && it.status != Status.COMPLETED && it.monthsLeft <= 3 } }
    val todayTotal = remember(b) { b.payments.filter { it.date == com.vinay.rddiary.data.today() }.let { it.sumOf { p -> p.amount } to it.size } }
    Scaffold(topBar = {
        Bar(T["app"], null)
    }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                // Big search box: opens the Accounts tab ready to type
                Surface(
                    onClick = { focusSearch = true; s.tab = Tab.ACCOUNTS },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(60.dp),
                    shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, null, tint = Brand)
                        Spacer(Modifier.width(12.dp))
                        Text(T["search"], style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Card(onClick = { s.tab = Tab.TODAY }) {
                    Text(T["todayCollection"], style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(inr(todayTotal.first), style = MaterialTheme.typography.headlineLarge, color = Brand)
                    Muted("${todayTotal.second} ${T["entries"]}")
                }
            }
            if (b.accounts.isEmpty()) {
                item { Card { Muted(T["noAccounts"]) } }
            } else {
                item {
                    Card(title = "${T["thisMonth"]} · ${now.month.getDisplayName(TextStyle.FULL, if (T.te) Locale("te") else Locale.ENGLISH)} — ${T["byAccounts"]}") {
                        Donut(
                            listOf(sm.upToDate.toDouble() to CUpToDate, sm.pending.toDouble() to CPending, sm.advance.toDouble() to CAdvance),
                            center = {
                                Text("${sm.active}", style = MaterialTheme.typography.headlineMedium)
                                Text(T["accountsWord"], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(10.dp))
                        LegendRow(CUpToDate, T["upToDate"], "${sm.upToDate}") { s.push(Route.Filtered(Filter.UP_TO_DATE)) }
                        LegendRow(CPending, T["pending"], "${sm.pending}") { s.push(Route.Filtered(Filter.PENDING)) }
                        LegendRow(CAdvance, T["advance"], "${sm.advance}") { s.push(Route.Filtered(Filter.ADVANCE)) }
                    }
                }
                item {
                    val left = (sm.expected - sm.collected).coerceAtLeast(0.0)
                    Card(title = "${T["thisMonth"]} — ${T["byAmount"]}") {
                        Donut(
                            listOf(sm.collected to CUpToDate, left to CPending),
                            center = {
                                Text(if (sm.expected > 0) "${Math.round(sm.collected / sm.expected * 100).coerceAtMost(999)}%" else "—", style = MaterialTheme.typography.headlineMedium)
                                Text(T["collected"], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(10.dp))
                        LegendRow(CUpToDate, T["collected"], inr(sm.collected)) { s.push(Route.Filtered(Filter.PAID_THIS_MONTH)) }
                        LegendRow(CPending, T["toCollect"], inr(left)) { s.push(Route.Filtered(Filter.NOT_PAID_THIS_MONTH)) }
                        Kv(T["expected"], inr(sm.expected))
                        Kv(T["totalPending"], inr(sm.pendingTotal), color = CPending) { s.push(Route.Filtered(Filter.PENDING)) }
                    }
                }
                item {
                    Card(title = T["maturing"], onClick = { s.push(Route.Filtered(Filter.MATURING)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${maturing.size}", style = MaterialTheme.typography.headlineLarge, color = if (maturing.isNotEmpty()) CAdvance else MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(12.dp))
                            Muted(T["withinMonths"])
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- Account lists ---------------- */

fun applyFilter(list: List<AccState>, f: Filter): List<AccState> = when (f) {
    Filter.ALL -> list.sortedBy { it.acc.name.lowercase() }
    Filter.PENDING -> list.filter { it.status == Status.PENDING }.sortedByDescending { it.pendingAmount }
    Filter.ADVANCE -> list.filter { it.status == Status.ADVANCE }.sortedByDescending { it.advanceAmount }
    Filter.UP_TO_DATE -> list.filter { it.status == Status.UP_TO_DATE }.sortedBy { it.acc.name.lowercase() }
    Filter.MATURING -> list.filter { it.status != Status.CLOSED && it.status != Status.COMPLETED && it.monthsLeft <= 3 }.sortedBy { it.monthsLeft }
    Filter.PAID_THIS_MONTH -> list.filter { it.paidThisMonth > 0 }.sortedBy { it.acc.name.lowercase() }
    Filter.NOT_PAID_THIS_MONTH -> list.filter { it.paidThisMonth == 0.0 && (it.status == Status.PENDING || it.status == Status.UP_TO_DATE) }
        .sortedByDescending { it.pendingAmount }
    Filter.DONE -> list.filter { it.status == Status.CLOSED || it.status == Status.COMPLETED }.sortedBy { it.acc.name.lowercase() }
}

fun filterName(f: Filter) = when (f) {
    Filter.ALL -> T["all"]; Filter.PENDING -> T["pending"]; Filter.ADVANCE -> T["advance"]; Filter.UP_TO_DATE -> T["upToDate"]
    Filter.MATURING -> T["maturing"]; Filter.PAID_THIS_MONTH -> T["paidThisMonth"]; Filter.NOT_PAID_THIS_MONTH -> T["toCollect"]
    Filter.DONE -> T["completed"] + " / " + T["closed"]
}

fun matches(st: AccState, q: String): Boolean {
    val t = q.trim().lowercase()
    if (t.isEmpty()) return true
    return st.acc.name.lowercase().contains(t) || st.acc.accNo.lowercase().contains(t) || st.acc.phone.contains(t)
}

@Composable
fun AccountsScreen(s: AppState) {
    var q by rememberSaveable { mutableStateOf("") }
    var f by rememberSaveable { mutableStateOf(Filter.ALL) }
    val sts = remember(s.book) { s.book.states() }
    val list = remember(sts, q, f) { applyFilter(sts, f).filter { matches(it, q) } }
    val fr = remember { FocusRequester() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) { kotlinx.coroutines.delay(150); try { fr.requestFocus() } catch (_: Exception) {}; focusSearch = false }
    }
    Scaffold(topBar = { Bar(T["accounts"], null) }) { pad ->
        Column(Modifier.padding(pad)) {
            TextField(
                q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 14.dp).focusRequester(fr),
                placeholder = { Text(T["searchHint"]) }, singleLine = true, shape = RoundedCornerShape(30.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Brand) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Filled.Close, null) } },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp)) {
                listOf(Filter.ALL, Filter.PENDING, Filter.ADVANCE, Filter.UP_TO_DATE, Filter.MATURING, Filter.DONE).forEach { x ->
                    FilterChip(f == x, { f = x }, { Text(filterName(x), style = MaterialTheme.typography.labelLarge) }, Modifier.padding(end = 8.dp))
                }
            }
            Muted("${list.size} ${T["accountsWord"]}", Modifier.padding(horizontal = 20.dp))
            LazyColumn(flingBehavior = rememberCalmFling(), contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)) {
                if (list.isEmpty()) item { Muted(if (s.book.accounts.isEmpty()) T["noAccounts"] else T["noResults"], Modifier.padding(24.dp)) }
                items(list, key = { it.acc.id }) { AccountRow(s, it) }
            }
        }
    }
}

@Composable
fun FilteredScreen(s: AppState, f: Filter) {
    val sts = remember(s.book) { s.book.states() }
    val list = remember(sts) { applyFilter(sts, f) }
    Scaffold(topBar = { Bar(filterName(f), { s.pop() }) }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                val total = when (f) {
                    Filter.PENDING, Filter.NOT_PAID_THIS_MONTH -> list.sumOf { it.pendingAmount }
                    Filter.ADVANCE -> list.sumOf { it.advanceAmount }
                    Filter.PAID_THIS_MONTH -> list.sumOf { it.paidThisMonth }
                    else -> null
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text("${list.size} ${T["accountsWord"]}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (total != null) Text("${T["total"]} ${inr(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (list.isEmpty()) item { Muted(T["noResults"], Modifier.padding(24.dp)) }
            items(list, key = { it.acc.id }) { AccountRow(s, it) }
        }
    }
}
