@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.rddiary.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinay.rddiary.AppState
import com.vinay.rddiary.Route
import com.vinay.rddiary.T
import com.vinay.rddiary.data.Account
import com.vinay.rddiary.data.Book
import com.vinay.rddiary.data.Payment
import com.vinay.rddiary.data.Status
import com.vinay.rddiary.data.amt
import com.vinay.rddiary.data.inr
import com.vinay.rddiary.data.num
import com.vinay.rddiary.data.parseAccounts
import com.vinay.rddiary.data.parseBook
import com.vinay.rddiary.data.paymentsOf
import com.vinay.rddiary.data.state
import com.vinay.rddiary.data.thisMonth
import com.vinay.rddiary.data.toJson
import com.vinay.rddiary.data.today
import com.vinay.rddiary.data.uid
import com.vinay.rddiary.data.ym
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private fun loc() = if (T.te) Locale("te") else Locale.ENGLISH
fun monthLabel(m: YearMonth) = m.month.getDisplayName(JTextStyle.SHORT, loc()) + " " + m.year
fun dayLabel(d: String) = LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d MMM yyyy", loc()))

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, numeric: Boolean = false, modifier: Modifier = Modifier.fillMaxWidth(), minLines: Int = 1) {
    TextField(
        value, onChange, modifier, label = { Text(label) }, singleLine = minLines == 1, minLines = minLines,
        textStyle = MaterialTheme.typography.bodyLarge, shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(unfocusedIndicatorColor = Color.Transparent),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
    )
}

@Composable
private fun BottomButtons(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(14.dp, 8.dp, 14.dp, 12.dp)) { content() }
    }
}

/* ---------------- One account ---------------- */

@Composable
fun DetailScreen(s: AppState, id: String) {
    val b = s.book
    val a = b.accounts.firstOrNull { it.id == id }
    if (a == null) { LaunchedEffect(Unit) { s.pop() }; return }
    val st = remember(b, id) { b.state(a) }
    val pays = remember(b, id) { b.paymentsOf(id).sortedWith(compareByDescending<Payment> { it.date }.thenByDescending { it.created }) }
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            Bar(a.name, { s.pop() }) {
                IconButton(onClick = { s.push(Route.EditAccount(id)) }) { Icon(Icons.Outlined.Edit, T["edit"]) }
            }
        },
        bottomBar = {
            if (st.status != Status.CLOSED && st.status != Status.COMPLETED)
                BottomButtons { BigButton("₹  " + T["enterPayment"]) { s.push(Route.Pay(id)) } }
        },
    ) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Card {
                    StatusPill(st)
                    Spacer(Modifier.height(8.dp))
                    when (st.status) {
                        Status.PENDING -> Text(inr(st.pendingAmount), style = MaterialTheme.typography.displaySmall, color = CPending)
                        Status.ADVANCE -> Text(inr(st.advanceAmount), style = MaterialTheme.typography.displaySmall, color = CAdvance)
                        else -> {}
                    }
                    if (a.accNo.isNotBlank()) Kv(T["accNo"], a.accNo)
                    Kv(T["monthly"], inr(a.monthly))
                    Kv(T["opened"], monthLabel(ym(a.start)))
                    Kv(T["installments"], "${st.monthsPaid} / ${a.tenure}")
                    Kv(T["dueTillNow"], inr(st.due))
                    Kv(T["paidTillNow"], inr(st.paid))
                    Kv(T["paidThisMonth"], inr(st.paidThisMonth))
                    Kv(T["maturity"], monthLabel(st.maturity) + when {
                        st.monthsLeft > 0 -> "  (${st.monthsLeft} ${T["monthsLeft"]})"
                        st.monthsLeft == 0 -> "  (${T["thisMonthEnds"]})"
                        else -> "  (${T["matured"]})"
                    }, color = if (st.monthsLeft in 0..3) CAdvance else Color.Unspecified)
                    if (a.phone.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + a.phone))) }, Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Filled.Call, null); Spacer(Modifier.width(8.dp)); Text("${T["call"]}  ${a.phone}")
                        }
                    }
                    // 60-month accounts close to the end can be extended to 120 months
                    if (a.tenure < 120 && st.monthsLeft <= 6 && st.status != Status.CLOSED) {
                        Spacer(Modifier.height(10.dp))
                        BigButton(T["extend"], color = CAdvance) {
                            s.update { bk -> bk.copy(accounts = bk.accounts.map { if (it.id == id) it.copy(tenure = 120) else it }) }
                            s.tick(true); s.toast(T["extended"], T["undo"]) {
                                s.update { bk -> bk.copy(accounts = bk.accounts.map { if (it.id == id) it.copy(tenure = a.tenure) else it }) }
                            }
                        }
                    }
                }
            }
            item { Text(T["history"], style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)) }
            if (a.openingPaid > 0) item {
                Card { Kv(T["openingPaid"], inr(a.openingPaid)) }
            }
            if (pays.isEmpty()) item { Muted(T["noPayments"], Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            items(pays, key = { it.id }) { p ->
                Card(onClick = { s.push(Route.Pay(id, p.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(dayLabel(p.date), style = MaterialTheme.typography.bodyLarge)
                            if (p.note.isNotBlank()) Muted(p.note)
                        }
                        Text(inr(p.amount), style = MaterialTheme.typography.titleMedium, color = CUpToDate)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

/* ---------------- Enter or edit a payment ---------------- */

@Composable
fun PayScreen(s: AppState, accountId: String, paymentId: String?) {
    val b = s.book
    val a = b.accounts.firstOrNull { it.id == accountId }
    if (a == null) { LaunchedEffect(Unit) { s.pop() }; return }
    val existing = paymentId?.let { pid -> b.payments.firstOrNull { it.id == pid } }
    // state as if this payment didn't exist (so editing shows the right preview)
    val base = remember(b) { Book(b.accounts, b.payments.filter { it.id != paymentId }) }
    val st = remember(base) { base.state(a) }
    var amount by remember { mutableStateOf(existing?.let { amt(it.amount) } ?: amt(a.monthly)) }
    var date by remember { mutableStateOf(existing?.date ?: today()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(150); try { fr.requestFocus() } catch (_: Exception) {} }

    val value = num(amount) ?: 0.0
    val after = remember(base, value, date) {
        if (value <= 0) null
        else base.copy(payments = base.payments + Payment("preview", accountId, date, value)).state(a)
    }

    fun save() {
        if (value <= 0) { s.toast(T["enterAmount"]); return }
        val p = Payment(existing?.id ?: uid(), accountId, date, value, note.trim(), existing?.created ?: System.currentTimeMillis())
        val before = s.book
        s.update { bk -> bk.copy(payments = if (existing == null) bk.payments + p else bk.payments.map { if (it.id == p.id) p else it }) }
        s.tick(true)
        s.pop()
        s.toast("${T["saved"]}: ${a.name} ${inr(value)}", T["undo"]) { s.replaceAll(before) }
    }

    Scaffold(
        topBar = { Bar(a.name, { s.pop() }) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, T["delete"], tint = CPending) }
        } },
        bottomBar = { BottomButtons { BigButton(T["save"]) { save() } } },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState(), flingBehavior = rememberCalmFling()).padding(horizontal = 18.dp)) {
            StatusPill(st)
            Muted("${T["monthly"]} ${inr(a.monthly)}" + if (a.accNo.isNotBlank()) " · ${a.accNo}" else "", Modifier.padding(top = 6.dp))
            Label(T["amount"])
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹", fontSize = 36.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    amount, { amount = it.replace(Regex("[^\\d.]"), "") },
                    Modifier.fillMaxWidth().focusRequester(fr),
                    textStyle = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true, cursorBrush = SolidColor(Brand),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            HorizontalDivider(color = Brand, thickness = 2.dp)
            // quick amounts
            FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quick = ArrayList<Pair<String, Double>>()
                (1..3).forEach { n -> quick += "$n ${if (n == 1) T["month"] else T["months"]} · ${inr(a.monthly * n)}" to a.monthly * n }
                if (st.status == Status.PENDING && st.pendingAmount != a.monthly) quick += "${T["pendingAmt"]} · ${inr(st.pendingAmount)}" to st.pendingAmount
                quick.forEach { (l, v) ->
                    FilterChip(value == v, { amount = amt(v); s.tick() }, { Text(l, style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = if (value == v) ({ Icon(Icons.Filled.Check, null) }) else null)
                }
            }
            // date
            Label(T["date"])
            val t = today()
            val y = LocalDate.now().minusDays(1).toString()
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(date == t, { date = t }, { Text(T["today"], style = MaterialTheme.typography.labelLarge) })
                FilterChip(date == y, { date = y }, { Text(if (T.te) "నిన్న" else "Yesterday", style = MaterialTheme.typography.labelLarge) })
                FilterChip(date != t && date != y, { picking = true }, { Text(if (date != t && date != y) dayLabel(date) else T["pickDay"], style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) })
            }
            Label(T["note"])
            Field(note, { note = it }, T["note"])
            // what happens after this payment
            if (after != null) {
                Surface(Modifier.fillMaxWidth().padding(top = 18.dp), shape = RoundedCornerShape(16.dp), color = statusColor(after.status).copy(alpha = .1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Muted(T["afterThis"])
                        Text(statusText(after), style = MaterialTheme.typography.titleMedium, color = statusColor(after.status))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (picking) {
        val ps = rememberDatePickerState(LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { ps.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; picking = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(T["cancel"]) } },
        ) { DatePicker(ps) }
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("${T["delete"]} ${inr(existing.amount)}?") },
            text = { Text("${a.name} · ${dayLabel(existing.date)}") },
            confirmButton = { TextButton(onClick = {
                val before = s.book
                s.update { bk -> bk.copy(payments = bk.payments.filter { it.id != existing.id }) }
                confirmDelete = false; s.pop(); s.toast(T["deleted"], T["undo"]) { s.replaceAll(before) }
            }) { Text(T["delete"], color = CPending) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(T["cancel"]) } },
        )
    }
}

/* ---------------- Add / edit an account ---------------- */

@Composable
private fun MonthPicker(value: YearMonth, onChange: (YearMonth) -> Unit) {
    var mOpen by remember { mutableStateOf(false) }
    var yOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { mOpen = true }, Modifier.fillMaxWidth().height(56.dp)) {
                Text(value.month.getDisplayName(JTextStyle.FULL, loc()), style = MaterialTheme.typography.bodyLarge)
            }
            DropdownMenu(mOpen, { mOpen = false }) {
                (1..12).forEach { m ->
                    DropdownMenuItem(text = { Text(java.time.Month.of(m).getDisplayName(JTextStyle.FULL, loc())) }, onClick = { onChange(value.withMonth(m)); mOpen = false })
                }
            }
        }
        Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { yOpen = true }, Modifier.fillMaxWidth().height(56.dp)) { Text("${value.year}", style = MaterialTheme.typography.bodyLarge) }
            DropdownMenu(yOpen, { yOpen = false }) {
                (YearMonth.now().year + 1 downTo 2005).forEach { y ->
                    DropdownMenuItem(text = { Text("$y") }, onClick = { onChange(value.withYear(y)); yOpen = false })
                }
            }
        }
    }
}

@Composable
fun EditAccountScreen(s: AppState, id: String?) {
    val a = id?.let { i -> s.book.accounts.firstOrNull { it.id == i } }
    var name by remember { mutableStateOf(a?.name ?: "") }
    var accNo by remember { mutableStateOf(a?.accNo ?: "") }
    var phone by remember { mutableStateOf(a?.phone ?: "") }
    var monthly by remember { mutableStateOf(a?.let { amt(it.monthly) } ?: "") }
    var start by remember { mutableStateOf(a?.let { ym(it.start) } ?: thisMonth()) }
    var tenure by remember { mutableStateOf(a?.tenure ?: 60) }
    var opening by remember { mutableStateOf(a?.let { if (it.openingPaid > 0) amt(it.openingPaid) else "" } ?: "") }
    var note by remember { mutableStateOf(a?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { Bar(if (a == null) T["addAccount"] else T["editAccount"], { s.pop() }) },
        bottomBar = {
            BottomButtons {
                BigButton(T["save"]) {
                    val m = num(monthly)
                    if (name.isBlank()) { s.toast(T["giveName"]); return@BigButton }
                    if (m == null || m <= 0) { s.toast(T["enterAmount"]); return@BigButton }
                    val acc = Account(a?.id ?: uid(), name.trim(), accNo.trim(), phone.trim(), m, start.toString(), tenure, num(opening) ?: 0.0, note.trim(), a?.closed ?: false)
                    s.update { bk -> bk.copy(accounts = if (a == null) bk.accounts + acc else bk.accounts.map { if (it.id == acc.id) acc else it }) }
                    s.tick(true); s.pop(); s.toast(T["saved"])
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState(), flingBehavior = rememberCalmFling()).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(6.dp))
            Field(name, { name = it }, T["name"])
            Spacer(Modifier.height(10.dp))
            Field(accNo, { accNo = it }, T["accNo"])
            Spacer(Modifier.height(10.dp))
            Field(monthly, { monthly = it.replace(Regex("[^\\d.]"), "") }, T["monthly"] + " (₹)", numeric = true)
            Spacer(Modifier.height(10.dp))
            Field(phone, { phone = it.replace(Regex("[^\\d+ ]"), "") }, T["phone"], numeric = true)
            Label(T["opened"])
            MonthPicker(start) { start = it }
            Label(T["tenure"])
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(60, 120).forEachIndexed { i, v ->
                    SegmentedButton(tenure == v, { tenure = v }, SegmentedButtonDefaults.itemShape(i, 2), Modifier.height(52.dp)) {
                        Text("$v ${T["months"]}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Label(T["openingPaid"])
            Field(opening, { opening = it.replace(Regex("[^\\d.]"), "") }, T["openingPaid"], numeric = true)
            Muted(T["openingPaidHint"], Modifier.padding(top = 4.dp))
            Label(T["note"])
            Field(note, { note = it }, T["note"])
            if (a != null) {
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = {
                    s.update { bk -> bk.copy(accounts = bk.accounts.map { if (it.id == a.id) it.copy(closed = !it.closed) else it }) }
                    s.pop(); s.toast(T["saved"])
                }, Modifier.fillMaxWidth().height(52.dp)) { Text(if (a.closed) T["reopenAccount"] else T["closeAccount"]) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth().height(52.dp)) { Text(T["deleteAccount"], color = CPending) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmDelete && a != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(T["deleteAccount"]) },
            text = { Text("${a.name}\n\n${T["deleteAccountQ"]}") },
            confirmButton = { TextButton(onClick = {
                val before = s.book
                s.update { bk -> bk.copy(accounts = bk.accounts.filter { it.id != a.id }, payments = bk.payments.filter { it.accountId != a.id }) }
                confirmDelete = false
                s.stack.clear()
                s.toast(T["deleted"], T["undo"]) { s.replaceAll(before) }
            }) { Text(T["delete"], color = CPending) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(T["cancel"]) } },
        )
    }
}

/* ---------------- This month's collection ---------------- */

@Composable
fun MonthScreen(s: AppState) {
    var month by remember { mutableStateOf(thisMonth()) }
    val b = s.book
    val names = remember(b) { b.accounts.associate { it.id to it.name } }
    val list = remember(b, month) { b.payments.filter { it.date.startsWith(month.toString()) }.sortedWith(compareByDescending<Payment> { it.date }.thenByDescending { it.created }) }
    val days = remember(list) { list.groupBy { it.date } }
    val people = remember(list) { list.map { it.accountId }.distinct().size }
    Scaffold(topBar = {
        Bar(monthLabel(month), null) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < thisMonth()) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        }
    }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Card {
                    Text(T["received"], style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(inr(list.sumOf { it.amount }), style = MaterialTheme.typography.displaySmall, color = Brand)
                    Muted("${list.size} ${T["entries"]} · $people ${T["people"]}")
                    if (month == thisMonth()) {
                        val todays = list.filter { it.date == today() }
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Kv(T["todayCollection"], "${inr(todays.sumOf { it.amount })} · ${todays.size}")
                    }
                }
            }
            if (list.isEmpty()) item { Muted(T["nobodyToday"], Modifier.padding(24.dp)) }
            days.forEach { (d, ps) ->
                item(key = "h$d") {
                    Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 2.dp)) {
                        Text(if (d == today()) T["today"] + " · " + dayLabel(d) else dayLabel(d), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text(inr(ps.sumOf { it.amount }), style = MaterialTheme.typography.titleSmall, color = Brand)
                    }
                }
                items(ps, key = { it.id }) { p ->
                    Card(onClick = { s.push(Route.Pay(p.accountId, p.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(names[p.accountId] ?: "?", style = MaterialTheme.typography.titleMedium)
                                if (p.note.isNotBlank()) Muted(p.note)
                            }
                            Text(inr(p.amount), style = MaterialTheme.typography.titleMedium, color = CUpToDate)
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- Settings ---------------- */

@Composable
private fun SettingRow(title: String, sub: String? = null, color: Color = Color.Unspecified, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
            if (sub != null) Muted(sub)
        }
        trailing?.invoke()
    }
}

@Composable
fun SettingsScreen(s: AppState) {
    val ctx = LocalContext.current
    var restoreBook by remember { mutableStateOf<Book?>(null) }
    var erase by remember { mutableStateOf(false) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) try {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(s.book.toJson().toString().toByteArray()) }
            s.toast(T["saved"])
        } catch (e: Exception) { s.toast("${e.message}") }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) try {
            val j = JSONObject(ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: "")
            if (j.optString("app") != "rddiary") s.toast(T["notBackup"]) else restoreBook = parseBook(j)
        } catch (e: Exception) { s.toast(T["notBackup"]) }
    }
    Scaffold(topBar = { Bar(T["more"], null) }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Card(title = T["language"]) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(T.te, { s.setTelugu(true) }, SegmentedButtonDefaults.itemShape(0, 2), Modifier.height(52.dp)) { Text("తెలుగు", style = MaterialTheme.typography.bodyLarge) }
                        SegmentedButton(!T.te, { s.setTelugu(false) }, SegmentedButtonDefaults.itemShape(1, 2), Modifier.height(52.dp)) { Text("English", style = MaterialTheme.typography.bodyLarge) }
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingRow(T["bigText"], trailing = { Switch(s.bigText, { s.setBig(it) }) }) { s.setBig(!s.bigText) }
                }
            }
            item {
                Card(title = T["accounts"]) {
                    SettingRow(T["addAccount"]) { s.push(Route.EditAccount(null)) }
                    SettingRow(T["import"]) { s.push(Route.Import) }
                }
            }
            item {
                Card(title = T["backup"]) {
                    SettingRow(T["backup"], T["backupHelp"]) { backup.launch("rd-diary-backup-${today()}.json") }
                    SettingRow(T["restore"]) { restore.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) }
                }
            }
            item { Card { SettingRow(T["erase"], color = CPending) { erase = true } } }
            item { Muted(T["dataSafe"], Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
        }
    }
    restoreBook?.let { nb ->
        AlertDialog(
            onDismissRequest = { restoreBook = null },
            title = { Text(T["restore"]) },
            text = { Text("${nb.accounts.size} ${T["accountsWord"]}, ${nb.payments.size} ${T["entries"]}\n\n${T["restoreQ"]}") },
            confirmButton = { TextButton(onClick = { s.replaceAll(nb); restoreBook = null; s.toast(T["saved"]) }) { Text(T["restore"], color = CPending) } },
            dismissButton = { TextButton(onClick = { restoreBook = null }) { Text(T["cancel"]) } },
        )
    }
    if (erase) {
        AlertDialog(
            onDismissRequest = { erase = false },
            title = { Text(T["erase"]) },
            text = { Text(T["eraseQ"]) },
            confirmButton = { TextButton(onClick = {
                val before = s.book
                s.replaceAll(Book()); erase = false; s.toast(T["deleted"], T["undo"]) { s.replaceAll(before) }
            }) { Text(T["delete"], color = CPending) } },
            dismissButton = { TextButton(onClick = { erase = false }) { Text(T["cancel"]) } },
        )
    }
}

/* ---------------- Import accounts ---------------- */

@Composable
fun ImportScreen(s: AppState) {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Triple<List<Account>, Int, Int>?>(null) } // new accounts, duplicates, skipped rows
    Scaffold(
        topBar = { Bar(T["import"], { s.pop() }) },
        bottomBar = {
            BottomButtons {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        val r = parseAccounts(text)
                        val have = s.book.accounts.map { if (it.accNo.isNotBlank()) it.accNo.lowercase() else it.name.lowercase() + "|" + it.start }.toHashSet()
                        val fresh = r.accounts.filter { !have.contains(if (it.accNo.isNotBlank()) it.accNo.lowercase() else it.name.lowercase() + "|" + it.start) }
                        result = Triple(fresh, r.accounts.size - fresh.size, r.skipped.size)
                    }, Modifier.weight(1f).height(56.dp)) { Text(T["check"], style = MaterialTheme.typography.titleMedium) }
                    BigButton(T["importN"], Modifier.weight(1f)) {
                        val r = result ?: return@BigButton
                        if (r.first.isEmpty()) return@BigButton
                        s.update { bk -> bk.copy(accounts = bk.accounts + r.first) }
                        s.tick(true); s.pop(); s.toast("${r.first.size} ${T["accountsWord"]} ✓")
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Muted(T["importHelp"])
            Spacer(Modifier.height(12.dp))
            TextField(
                text, { text = it; result = null }, Modifier.fillMaxWidth().heightIn(min = 200.dp),
                placeholder = { Text("Ramesh K\t020123456789\t500\t03/2024\t60\t9000", fontSize = 13.sp) },
                textStyle = TextStyle(fontSize = 14.sp), shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(unfocusedIndicatorColor = Color.Transparent),
            )
            result?.let { (fresh, dup, skipped) ->
                Card(Modifier.padding(top = 12.dp)) {
                    Text("${fresh.size} ${T["found"]}", style = MaterialTheme.typography.titleMedium, color = if (fresh.isNotEmpty()) CUpToDate else CPending)
                    if (dup > 0) Muted("$dup ${T["duplicates"]}")
                    if (skipped > 0) Muted("$skipped ${T["skipped"]}")
                    fresh.take(5).forEach { Muted("• ${it.name} · ${inr(it.monthly)} · ${monthLabel(ym(it.start))} · ${it.tenure}") }
                    if (fresh.size > 5) Muted("…")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
