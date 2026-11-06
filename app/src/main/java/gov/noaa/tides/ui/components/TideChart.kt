package gov.noaa.tides.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gov.noaa.tides.model.TideFormatters
import gov.noaa.tides.model.TidePoint
import gov.noaa.tides.ui.theme.CardBorder
import gov.noaa.tides.ui.theme.CardSurface
import gov.noaa.tides.ui.theme.TextPrimary
import gov.noaa.tides.ui.theme.TextSecondary
import gov.noaa.tides.ui.theme.TideAccentMint
import gov.noaa.tides.ui.theme.TideFillBlue
import gov.noaa.tides.ui.theme.TideLineBlue
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WeeklyTideChart(
    hourly: List<TidePoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(hourly) { mutableStateOf<Int?>(null) }
    val selectedPoint = selectedIndex?.let { hourly.getOrNull(it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedPoint != null) {
                Text(
                    text = TideFormatters.formatTooltip(selectedPoint.time),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = TideFormatters.formatHeight(selectedPoint.value),
                    color = TideAccentMint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Text(
                    text = "Tap or drag across chart to inspect tide height",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (hourly.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No prediction points available",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        val minValue = remember(hourly) { hourly.minOf { it.value } }
        val maxValue = remember(hourly) { hourly.maxOf { it.value } }
        val yPadding = ((maxValue - minValue) * 0.15).coerceAtLeast(0.5)
        val yMin = minValue - yPadding
        val yMax = maxValue + yPadding
        val yRange = (yMax - yMin).coerceAtLeast(1.0)

        val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE d", Locale.US) }

        val labelPaint = remember {
            Paint().apply {
                color = android.graphics.Color.parseColor("#A6B4C9")
                textSize = 28f
                isAntiAlias = true
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .pointerInput(hourly) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        selectedIndex = (fraction * (hourly.size - 1)).roundToInt()
                    }
                }
                .pointerInput(hourly) {
                    detectHorizontalDragGestures(
                        onDragEnd = { },
                        onHorizontalDrag = { change, _ ->
                            val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            selectedIndex = (fraction * (hourly.size - 1)).roundToInt()
                        }
                    )
                }
        ) {
            val leftPad = 68f
            val rightPad = 16f
            val topPad = 16f
            val bottomPad = 42f

            val plotWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            fun xForIndex(index: Int): Float {
                if (hourly.size <= 1) return leftPad
                return leftPad + (index.toFloat() / (hourly.size - 1).toFloat()) * plotWidth
            }

            fun yForValue(v: Double): Float {
                val normalized = ((v - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                return topPad + plotHeight * (1f - normalized)
            }

            // Horizontal grid lines and Y-axis labels
            val gridSteps = 4
            for (step in 0..gridSteps) {
                val fraction = step.toFloat() / gridSteps
                val value = yMax - fraction * yRange
                val y = topPad + fraction * plotHeight

                drawLine(
                    color = Color(0x14FFFFFF),
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1.dp.toPx()
                )

                val label = String.format(Locale.US, "%.1fft", value)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    4f,
                    y + 9f,
                    labelPaint
                )
            }

            // Zero line if in range
            if (0.0 in yMin..yMax) {
                val zeroY = yForValue(0.0)
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(leftPad, zeroY),
                    end = Offset(size.width - rightPad, zeroY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // X-axis day labels at midnight transitions
            var lastDate = hourly.first().time.toLocalDate()
            for (i in hourly.indices) {
                val currentDate = hourly[i].time.toLocalDate()
                if (i == 0 || currentDate != lastDate) {
                    val x = xForIndex(i)
                    val label = currentDate.format(dayFormatter)
                    if (x + 60f <= size.width) {
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            size.height - 8f,
                            labelPaint
                        )
                    }
                    lastDate = currentDate
                }
            }

            // Build line and fill paths
            val linePath = Path()
            val fillPath = Path()
            val baselineY = topPad + plotHeight

            hourly.forEachIndexed { index, point ->
                val x = xForIndex(index)
                val y = yForValue(point.value)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, baselineY)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (index == hourly.lastIndex) {
                    fillPath.lineTo(x, baselineY)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(TideFillBlue, Color.Transparent),
                    startY = topPad,
                    endY = baselineY
                )
            )

            drawPath(
                path = linePath,
                color = TideLineBlue,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Selected point crosshair and dot
            selectedIndex?.let { idx ->
                val point = hourly.getOrNull(idx) ?: return@let
                val cx = xForIndex(idx)
                val cy = yForValue(point.value)

                drawLine(
                    color = TideAccentMint.copy(alpha = 0.5f),
                    start = Offset(cx, topPad),
                    end = Offset(cx, baselineY),
                    strokeWidth = 1.5.dp.toPx()
                )

                drawCircle(
                    color = TideAccentMint,
                    radius = 6.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF07111C),
                    radius = 2.5.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
