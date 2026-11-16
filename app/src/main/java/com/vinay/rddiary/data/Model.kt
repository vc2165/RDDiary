package com.vinay.rddiary.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** One customer RD account. */
data class Account(
    val id: String,
    val name: String,
    val accNo: String = "",
    val phone: String = "",
    val monthly: Double,
    val start: String,          // first month, "yyyy-MM"
    val tenure: Int = 60,       // months: 60, or 120 after extension
    val openingPaid: Double = 0.0, // deposited before the app was used
    val note: String = "",
    val closed: Boolean = false,
)

/** One payment collected from a customer. */
data class Payment(
    val id: String,
    val accountId: String,
    val date: String,           // "yyyy-MM-dd"
    val amount: Double,
    val note: String = "",
    val created: Long = 0L,
)

data class Book(val accounts: List<Account> = emptyList(), val payments: List<Payment> = emptyList())

enum class Status { UP_TO_DATE, PENDING, ADVANCE, COMPLETED, CLOSED }

/** Everything the screens need to know about one account, worked out from due vs paid. */
data class AccState(
    val acc: Account,
    val monthsDue: Int,       // installments due up to and including this month
    val due: Double,          // ₹ due till now
    val paid: Double,         // ₹ paid till now (incl. opening)
    val diff: Double,         // paid - due (>0 advance, <0 pending)
    val status: Status,
    val pendingMonths: Int,
    val advanceMonths: Int,
    val monthsPaid: Int,      // how many installments are covered by what's been paid
    val maturity: YearMonth,  // last installment month
    val monthsLeft: Int,      // months until maturity from this month (0 = this month)
    val paidThisMonth: Double,
) {
    val pendingAmount get() = if (diff < 0) -diff else 0.0
    val advanceAmount get() = if (diff > 0) diff else 0.0
    val fullAmount get() = acc.monthly * acc.tenure
}

fun ym(s: String): YearMonth = YearMonth.parse(s)
fun thisMonth(): YearMonth = YearMonth.now()
fun today(): String = LocalDate.now().toString()
fun uid(): String = java.lang.Long.toString(System.currentTimeMillis(), 36) + java.lang.Long.toString((Math.random() * 1e12).toLong(), 36).take(6)

fun Book.paymentsOf(id: String) = payments.filter { it.accountId == id }

fun Book.state(a: Account, now: YearMonth = thisMonth()): AccState {
    val start = ym(a.start)
    val maturity = start.plusMonths((a.tenure - 1).toLong())
    val elapsed = ChronoUnit.MONTHS.between(start, now).toInt() + 1 // this month counts
    val monthsDue = elapsed.coerceIn(0, a.tenure)
    val due = monthsDue * a.monthly
    val pays = paymentsOf(a.id)
    val paid = a.openingPaid + pays.sumOf { it.amount }
    val diff = paid - due
    val m = if (a.monthly > 0) a.monthly else 1.0
    val monthsPaid = min(a.tenure, floor((paid + 0.001) / m).toInt())
    val pendingMonths = if (diff < -0.5) ceil(-diff / m - 0.001).toInt() else 0
    val advanceMonths = if (diff > 0.5) floor(diff / m + 0.001).toInt() else 0
    val status = when {
        a.closed -> Status.CLOSED
        paid + 0.5 >= a.monthly * a.tenure -> Status.COMPLETED
        diff < -0.5 -> Status.PENDING
        advanceMonths >= 1 -> Status.ADVANCE
        else -> Status.UP_TO_DATE
    }
    val nowKey = now.toString()
    return AccState(
        a, monthsDue, due, paid, diff, status, pendingMonths, advanceMonths, monthsPaid, maturity,
        ChronoUnit.MONTHS.between(now, maturity).toInt(),
        pays.filter { it.date.startsWith(nowKey) }.sumOf { it.amount },
    )
}

fun Book.states(now: YearMonth = thisMonth()) = accounts.map { state(it, now) }

/** Accounts still collecting (not closed / completed). */
fun List<AccState>.active() = filter { it.status != Status.CLOSED && it.status != Status.COMPLETED }

data class MonthSummary(
    val active: Int, val upToDate: Int, val pending: Int, val advance: Int,
    val expected: Double,     // this month's installments of active accounts
    val collected: Double,    // ₹ collected (entered) this month
    val pendingTotal: Double, // all arrears
)

fun Book.summary(now: YearMonth = thisMonth()): MonthSummary {
    val st = states(now).active()
    val key = now.toString()
    return MonthSummary(
        st.size,
        st.count { it.status == Status.UP_TO_DATE },
        st.count { it.status == Status.PENDING },
        st.count { it.status == Status.ADVANCE },
        st.filter { !it.maturity.isBefore(now) && !ym(it.acc.start).isAfter(now) }.sumOf { it.acc.monthly },
        payments.filter { it.date.startsWith(key) }.sumOf { it.amount },
        st.sumOf { it.pendingAmount },
    )
}

/* ---------- Import: rows pasted from Excel ---------- */
// Name | Account no | Monthly amount | Opening month | Tenure | Paid so far

data class ImportResult(val accounts: List<Account>, val skipped: List<String>)

private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

fun parseMonth(sIn: String): String? {
    val s = sIn.trim().lowercase().replace(",", " ")
    // 2024-03, 2024-03-15
    Regex("""^(\d{4})[-/.](\d{1,2})([-/.]\d{1,2})?$""").find(s)?.let { m ->
        return runCatching { YearMonth.of(m.groupValues[1].toInt(), m.groupValues[2].toInt()).toString() }.getOrNull()
    }
    // 15/03/2024 or 15-03-24
    Regex("""^(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})$""").find(s)?.let { m ->
        var y = m.groupValues[3].toInt(); if (y < 100) y += 2000
        return runCatching { YearMonth.of(y, m.groupValues[2].toInt()).toString() }.getOrNull()
    }
    // 03/2024 or 3-24
    Regex("""^(\d{1,2})[-/.](\d{2,4})$""").find(s)?.let { m ->
        var y = m.groupValues[2].toInt(); if (y < 100) y += 2000
        return runCatching { YearMonth.of(y, m.groupValues[1].toInt()).toString() }.getOrNull()
    }
    // Mar 2024, march-24
    Regex("""^([a-z]{3,9})[\s\-/.']*(\d{2,4})$""").find(s)?.let { m ->
        val mi = MONTHS.indexOf(m.groupValues[1].take(3))
        if (mi < 0) return null
        var y = m.groupValues[2].toInt(); if (y < 100) y += 2000
        return YearMonth.of(y, mi + 1).toString()
    }
    return null
}

fun num(s: String): Double? = s.replace(Regex("[₹,\\s]|rs\\.?", RegexOption.IGNORE_CASE), "").toDoubleOrNull()

// India Post agent portal "Deposit Accounts List" row:
// 020047761649  M JAYALAKSHMI  3,000.00 Cr.  52  26-Dec-2026
private val PORTAL = Regex("""^\s*(\d{9,16})\s+(.+?)\s+([\d,]+(?:\.\d+)?)\s*(?:Cr\.?|Dr\.?)?\s+(\d{1,3})\s+(\d{1,2})-([A-Za-z]{3})-(\d{4})\s*$""")
// Header / footer lines copied along with a portal page
private val PORTAL_NOISE = Regex("(?i)deposit accounts list|displaying \\d|account no\\s+account name|^\\s*page \\d+ of \\d+|^\\s*select\\b")

/** Reads one portal row. Opening month = next due month − months paid; paid so far = denomination × months paid. */
fun parsePortalRow(line: String): Account? {
    val m = PORTAL.find(line) ?: return null
    val (acc, name, den, paid, _, mon, year) = m.destructured
    val mi = MONTHS.indexOf(mon.lowercase())
    if (mi < 0) return null
    val monthly = den.replace(",", "").toDoubleOrNull() ?: return null
    val months = paid.toInt()
    val start = YearMonth.of(year.toInt(), mi + 1).minusMonths(months.toLong())
    return Account(uid(), name.trim().replace(Regex("\\s+"), " "), acc, "", monthly, start.toString(), if (months >= 60) 120 else 60, monthly * months)
}

fun parseAccounts(text: String): ImportResult {
    val out = ArrayList<Account>()
    val skipped = ArrayList<String>()
    for (raw in text.split(Regex("\\r?\\n"))) {
        if (raw.isBlank() || PORTAL_NOISE.containsMatchIn(raw)) continue
        parsePortalRow(raw)?.let { out += it.copy(id = uid() + out.size); continue }
        var cols = raw.split('\t')
        if (cols.size < 3) cols = raw.split(Regex(""",(?=(?:[^"]*"[^"]*")*[^"]*$)"""))
        val c = cols.map { it.trim().removeSurrounding("\"") }
        if (c.size < 4) { skipped += raw; continue }
        val name = c[0]
        val monthly = num(c.getOrElse(2) { "" })
        val start = parseMonth(c.getOrElse(3) { "" })
        if (name.isEmpty() || monthly == null || monthly <= 0 || start == null) { skipped += raw; continue } // header rows land here too
        val tenure = c.getOrNull(4)?.let { num(it)?.toInt() }?.takeIf { it in 1..240 } ?: 60
        val paid = c.getOrNull(5)?.let { num(it) } ?: 0.0
        out += Account(uid() + out.size, name, c.getOrElse(1) { "" }, "", monthly, start, tenure, paid)
    }
    return ImportResult(out, skipped)
}

/* ---------- JSON (storage + backup) ---------- */

fun Book.toJson(): JSONObject = JSONObject()
    .put("app", "rddiary").put("v", 1)
    .put("accounts", JSONArray().apply {
        accounts.forEach {
            put(JSONObject().put("id", it.id).put("name", it.name).put("accNo", it.accNo).put("phone", it.phone)
                .put("monthly", it.monthly).put("start", it.start).put("tenure", it.tenure).put("openingPaid", it.openingPaid)
                .put("note", it.note).put("closed", it.closed))
        }
    })
    .put("payments", JSONArray().apply {
        payments.forEach {
            put(JSONObject().put("id", it.id).put("accountId", it.accountId).put("date", it.date).put("amount", it.amount)
                .put("note", it.note).put("created", it.created))
        }
    })

fun parseBook(o: JSONObject): Book {
    val accs = o.optJSONArray("accounts") ?: JSONArray()
    val pays = o.optJSONArray("payments") ?: JSONArray()
    return Book(
        (0 until accs.length()).mapNotNull { i ->
            val a = accs.optJSONObject(i) ?: return@mapNotNull null
            Account(
                a.optString("id", uid() + i), a.optString("name"), a.optString("accNo"), a.optString("phone"),
                a.optDouble("monthly", 0.0), a.optString("start", thisMonth().toString()), a.optInt("tenure", 60),
                a.optDouble("openingPaid", 0.0), a.optString("note"), a.optBoolean("closed", false),
            )
        },
        (0 until pays.length()).mapNotNull { i ->
            val p = pays.optJSONObject(i) ?: return@mapNotNull null
            Payment(p.optString("id", uid() + i), p.optString("accountId"), p.optString("date"), p.optDouble("amount", 0.0),
                p.optString("note"), p.optLong("created", 0L))
        },
    )
}

/* ---------- Formatting ---------- */

private val nf0 = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply { maximumFractionDigits = 0 }
fun inr(n: Double): String = (if (n < -0.5) "−" else "") + "₹" + nf0.format(kotlin.math.abs(Math.round(n)))
fun amt(n: Double): String = if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()
fun max0(n: Double) = max(0.0, n)
