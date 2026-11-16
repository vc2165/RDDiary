@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vinay.rddiary.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vinay.rddiary.AppState
import com.vinay.rddiary.Route
import com.vinay.rddiary.T
import com.vinay.rddiary.data.AccState
import com.vinay.rddiary.data.Status
import com.vinay.rddiary.data.inr

@Composable
fun Card(modifier: Modifier = Modifier, title: String? = null, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            content()
        }
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

@Composable
fun Kv(label: String, value: String, color: Color = Color.Unspecified, bold: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick) else Modifier).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Text(value, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun Bar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, T["cancel"]) } },
        actions = actions,
    )
}

/** A big, easy-to-hit main button. */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, color: Color = Brand, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

fun statusColor(s: Status) = when (s) {
    Status.UP_TO_DATE -> CUpToDate
    Status.PENDING -> CPending
    Status.ADVANCE -> CAdvance
    Status.COMPLETED, Status.CLOSED -> CDone
}

/** Short words for an account's position, e.g. "Pending ₹1,000 · 2 months". */
fun statusText(st: AccState): String = when (st.status) {
    Status.UP_TO_DATE -> T["upToDate"]
    Status.PENDING -> "${T["pending"]} ${inr(st.pendingAmount)} · ${st.pendingMonths} ${if (st.pendingMonths == 1) T["month"] else T["months"]}"
    Status.ADVANCE -> "${T["advance"]} ${inr(st.advanceAmount)} · ${st.advanceMonths} ${if (st.advanceMonths == 1) T["month"] else T["months"]}"
    Status.COMPLETED -> T["completed"]
    Status.CLOSED -> T["closed"]
}

@Composable
fun StatusPill(st: AccState) {
    val c = statusColor(st.status)
    Text(
        statusText(st), color = c, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** One account in a list: tap the row to open it, tap "Pay" to enter money straight away. */
@Composable
fun AccountRow(s: AppState, st: AccState) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp))
            .clickable { s.push(Route.Detail(st.acc.id)) },
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(statusColor(st.status)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(st.acc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(st.acc.accNo, "${inr(st.acc.monthly)}/${T["month"]}").filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                StatusPill(st)
            }
            if (st.status != Status.CLOSED && st.status != Status.COMPLETED) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = { s.push(Route.Pay(st.acc.id)) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(48.dp)) {
                    Text("₹ " + T["pay"], style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Donut chart that draws itself in, gently. */
@Composable
fun Donut(parts: List<Pair<Double, Color>>, center: @Composable () -> Unit, modifier: Modifier) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(parts) { p.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    val total = parts.sumOf { it.first }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 20.dp.toPx()
            val r = size.minDimension / 2 - sw / 2
            val tl = Offset(center.x - r, center.y - r)
            val sz = Size(r * 2, r * 2)
            drawArc(CGrey, 0f, 360f, false, tl, sz, style = Stroke(sw))
            if (total > 0) {
                var start = -90f
                parts.forEach { (v, col) ->
                    val sweep = (v / total * 360.0).toFloat() * p.value
                    if (sweep > 0.5f) drawArc(col, start, (sweep - 1.5f).coerceAtLeast(0.5f), false, tl, sz, style = Stroke(sw))
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { center() }
    }
}

/** A legend line under a chart; tapping it opens the matching list. */
@Composable
fun LegendRow(color: Color, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}
