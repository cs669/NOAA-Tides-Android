package gov.noaa.tides.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class Station(
    val id: String,
    val name: String,
    val state: String = "",
    val lat: Double? = null,
    val lng: Double? = null
) {
    fun formatLabel(): String {
        val displayName = name.ifBlank { id.ifBlank { "Unknown station" } }
        return buildList {
            add(displayName)
            if (state.isNotBlank()) add(state)
            if (id.isNotBlank()) add(id)
        }.joinToString(" · ")
    }
}

data class TidePoint(
    val time: LocalDateTime,
    val value: Double,
    val kind: String? = null
) {
    val displayKind: String
        get() = when (kind?.uppercase(Locale.US)) {
            "H" -> "High"
            "L" -> "Low"
            null, "" -> "Tide"
            else -> kind
        }
}

data class DaySummary(
    val date: LocalDate,
    val samples: List<TidePoint>,
    val events: List<TidePoint>,
    val min: TidePoint?,
    val max: TidePoint?
)

data class StationData(
    val station: Station,
    val generatedAt: LocalDateTime,
    val hourly: List<TidePoint>,
    val events: List<TidePoint>,
    val days: List<DaySummary>,
    val minValue: Double?,
    val maxValue: Double?,
    val currentValue: Double?,
    val currentTime: LocalDateTime?
)

object TideFormatters {
    private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dayPrefixFormatter = DateTimeFormatter.ofPattern("EEE, MMM ", Locale.US)
    private val updatedFormatter = DateTimeFormatter.ofPattern("MMM dd, h:mm a", Locale.US)
    private val tooltipFormatter = DateTimeFormatter.ofPattern("EEE MMM dd, h:mm a", Locale.US)
    private val noaaEntryFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    fun formatHeight(value: Double?): String =
        if (value == null) "--" else String.format(Locale.US, "%.2f ft", value)

    fun formatClock(value: LocalDateTime): String =
        value.format(clockFormatter)

    fun formatDayLabel(value: LocalDate): String =
        value.format(dayPrefixFormatter) + value.dayOfMonth.toString()

    fun formatUpdated(value: LocalDateTime): String =
        value.format(updatedFormatter)

    fun formatTooltip(value: LocalDateTime): String =
        value.format(tooltipFormatter)

    fun parseNoaaTimestamp(raw: String): LocalDateTime? {
        val trimmed = raw.trim()
        return try {
            if ('T' in trimmed) {
                LocalDateTime.parse(trimmed)
            } else {
                LocalDateTime.parse(trimmed, noaaEntryFormatter)
            }
        } catch (_: Exception) {
            null
        }
    }
}

object TideDomainLogic {
    fun searchStations(
        query: String,
        stations: List<Station>,
        limit: Int = 30
    ): List<Station> {
        val cleanedQuery = query.trim().lowercase(Locale.US)
        if (cleanedQuery.isEmpty()) {
            return stations.take(limit)
        }

        data class ScoredStation(val score: Int, val name: String, val station: Station)

        val scored = mutableListOf<ScoredStation>()
        for (station in stations) {
            val idLower = station.id.lowercase(Locale.US)
            val nameLower = station.name.lowercase(Locale.US)
            val stateLower = station.state.lowercase(Locale.US)
            val latLower = station.lat?.toString()?.lowercase(Locale.US).orEmpty()
            val lngLower = station.lng?.toString()?.lowercase(Locale.US).orEmpty()

            val haystack = "$idLower $nameLower $stateLower $latLower $lngLower"
            if (cleanedQuery !in haystack) continue

            val score = when {
                idLower.startsWith(cleanedQuery) -> 0
                nameLower.startsWith(cleanedQuery) -> 1
                cleanedQuery in nameLower -> 2
                else -> 3
            }
            scored.add(ScoredStation(score, station.name, station))
        }

        return scored
            .sortedWith(compareBy<ScoredStation> { it.score }.thenBy { it.name.lowercase(Locale.US) })
            .take(limit)
            .map { it.station }
    }

    fun summarizeDays(hourly: List<TidePoint>, events: List<TidePoint>): List<DaySummary> {
        val samplesByDate = linkedMapOf<LocalDate, MutableList<TidePoint>>()
        val eventsByDate = linkedMapOf<LocalDate, MutableList<TidePoint>>()
        val minByDate = mutableMapOf<LocalDate, TidePoint>()
        val maxByDate = mutableMapOf<LocalDate, TidePoint>()

        for (point in hourly) {
            val date = point.time.toLocalDate()
            samplesByDate.getOrPut(date) { mutableListOf() }.add(point)
            eventsByDate.putIfAbsent(date, mutableListOf())

            val currentMin = minByDate[date]
            if (currentMin == null || point.value < currentMin.value) {
                minByDate[date] = point
            }

            val currentMax = maxByDate[date]
            if (currentMax == null || point.value > currentMax.value) {
                maxByDate[date] = point
            }
        }

        for (event in events) {
            val date = event.time.toLocalDate()
            eventsByDate[date]?.add(event)
        }

        return samplesByDate.keys.sorted().map { date ->
            DaySummary(
                date = date,
                samples = samplesByDate[date].orEmpty(),
                events = eventsByDate[date].orEmpty(),
                min = minByDate[date],
                max = maxByDate[date]
            )
        }
    }

    fun findClosestPoint(points: List<TidePoint>, referenceTime: LocalDateTime): TidePoint? =
        points.minByOrNull { abs(Duration.between(it.time, referenceTime).seconds) }
}
