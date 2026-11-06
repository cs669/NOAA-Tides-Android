package gov.noaa.tides.data

import gov.noaa.tides.model.Station
import gov.noaa.tides.model.TideFormatters
import gov.noaa.tides.model.TidePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class NoaaApiService {

    companion object {
        private const val BASE_URL = "https://api.tidesandcurrents.noaa.gov/api/prod/datagetter"
        private const val STATION_CATALOG_URL =
            "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations.json?type=tidepredictions"
        private val beginEndFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
    }

    suspend fun fetchStationCatalog(): List<Station> = withContext(Dispatchers.IO) {
        val rawJson = getUrl(STATION_CATALOG_URL, timeoutMs = 30_000)
        val root = JSONObject(rawJson)
        val stationsArray = root.optJSONArray("stations") ?: return@withContext emptyList()

        val result = ArrayList<Station>(stationsArray.length())
        for (i in 0 until stationsArray.length()) {
            val item = stationsArray.optJSONObject(i) ?: continue
            val station = normalizeStation(item)
            if (station.id.isNotEmpty() && station.name.isNotEmpty()) {
                result.add(station)
            }
        }

        result.sortedWith(
            compareBy<Station> { it.name.lowercase(Locale.US) }
                .thenBy { it.state.lowercase(Locale.US) }
                .thenBy { it.id }
        )
    }

    suspend fun fetchPredictions(
        stationId: String,
        interval: String,
        start: LocalDate,
        end: LocalDate
    ): List<TidePoint> = withContext(Dispatchers.IO) {
        val params = linkedMapOf(
            "product" to "predictions",
            "application" to "NOAA Tide Dashboard",
            "begin_date" to start.format(beginEndFormatter),
            "end_date" to end.format(beginEndFormatter),
            "datum" to "MLLW",
            "station" to stationId,
            "time_zone" to "lst_ldt",
            "units" to "english",
            "interval" to interval,
            "format" to "json"
        )

        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        val rawJson = getUrl("$BASE_URL?$queryString", timeoutMs = 20_000)
        val root = JSONObject(rawJson)
        val predictionsArray = root.optJSONArray("predictions") ?: return@withContext emptyList()

        val points = ArrayList<TidePoint>(predictionsArray.length())
        for (i in 0 until predictionsArray.length()) {
            val entry = predictionsArray.optJSONObject(i) ?: continue
            parsePrediction(entry)?.let { points.add(it) }
        }
        points
    }

    private fun normalizeStation(item: JSONObject): Station {
        val id = item.optString("id", "").trim()
        val name = item.optString("name", "").trim()
        val state = item.optString("state", "").trim()
        val lat = if (item.has("lat") && !item.isNull("lat")) item.optDouble("lat") else null
        val lng = if (item.has("lng") && !item.isNull("lng")) item.optDouble("lng") else null
        return Station(
            id = id,
            name = name,
            state = state,
            lat = lat?.takeIf { !it.isNaN() },
            lng = lng?.takeIf { !it.isNaN() }
        )
    }

    private fun parsePrediction(entry: JSONObject): TidePoint? {
        val rawTime = entry.optString("t", "")
        val rawValue = entry.optString("v", "")
        if (rawTime.isEmpty() || rawValue.isEmpty()) return null

        val parsedTime = TideFormatters.parseNoaaTimestamp(rawTime) ?: return null
        val parsedValue = rawValue.toDoubleOrNull() ?: return null
        val kind = if (entry.has("type") && !entry.isNull("type")) {
            entry.optString("type").takeIf { it.isNotBlank() }
        } else {
            null
        }
        return TidePoint(time = parsedTime, value = parsedValue, kind = kind)
    }

    private fun getUrl(urlString: String, timeoutMs: Int): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NOAATideDashboard-Android/1.0")
        }

        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }

            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP $responseCode from NOAA API: $body")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }
}
