package gov.noaa.tides.data

import android.content.Context
import gov.noaa.tides.model.Station
import org.json.JSONArray
import org.json.JSONObject

class RecentStationsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "noaa_tides_prefs"
        private const val KEY_RECENT_STATIONS = "recent_stations_json"
        private const val MAX_RECENT_STATIONS = 10
    }

    fun loadRecentStations(): List<Station> {
        val raw = prefs.getString(KEY_RECENT_STATIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val stations = mutableListOf<Station>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val name = obj.optString("name", "").trim()
                val state = obj.optString("state", "").trim()
                val lat = if (obj.has("lat") && !obj.isNull("lat")) obj.optDouble("lat") else null
                val lng = if (obj.has("lng") && !obj.isNull("lng")) obj.optDouble("lng") else null
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    stations.add(
                        Station(
                            id = id,
                            name = name,
                            state = state,
                            lat = lat?.takeIf { !it.isNaN() },
                            lng = lng?.takeIf { !it.isNaN() }
                        )
                    )
                }
            }
            stations.take(MAX_RECENT_STATIONS)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun rememberStation(station: Station): List<Station> {
        val updated = buildList {
            add(station)
            addAll(loadRecentStations().filterNot { it.id == station.id })
        }.take(MAX_RECENT_STATIONS)

        saveRecentStations(updated)
        return updated
    }

    private fun saveRecentStations(stations: List<Station>) {
        val array = JSONArray()
        stations.take(MAX_RECENT_STATIONS).forEach { station ->
            val obj = JSONObject().apply {
                put("id", station.id)
                put("name", station.name)
                put("state", station.state)
                if (station.lat != null) put("lat", station.lat)
                if (station.lng != null) put("lng", station.lng)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_STATIONS, array.toString()).apply()
    }
}
