package gov.noaa.tides.data

import android.util.LruCache
import gov.noaa.tides.model.Station
import gov.noaa.tides.model.StationData
import gov.noaa.tides.model.TideDomainLogic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalDateTime

class TideRepository(
    private val apiService: NoaaApiService,
    private val recentStationsStore: RecentStationsStore
) {
    @Volatile
    private var cachedCatalog: List<Station>? = null

    private val stationDataCache = LruCache<String, StationData>(8)

    suspend fun getStationCatalog(forceRefresh: Boolean = false): List<Station> {
        val existing = cachedCatalog
        if (!forceRefresh && !existing.isNullOrEmpty()) {
            return existing
        }
        val fetched = apiService.fetchStationCatalog()
        if (fetched.isNotEmpty()) {
            cachedCatalog = fetched
        }
        return fetched
    }

    fun getRecentStations(): List<Station> = recentStationsStore.loadRecentStations()

    fun rememberStation(station: Station): List<Station> =
        recentStationsStore.rememberStation(station)

    suspend fun loadStationData(
        stationId: String,
        forceRefresh: Boolean = false
    ): StationData = coroutineScope {
        if (!forceRefresh) {
            stationDataCache.get(stationId)?.let { return@coroutineScope it }
        }

        val start = LocalDate.now()
        val end = start.plusDays(6)

        val hourlyDeferred = async { apiService.fetchPredictions(stationId, "h", start, end) }
        val eventsDeferred = async { apiService.fetchPredictions(stationId, "hilo", start, end) }

        val hourly = hourlyDeferred.await()
        val events = eventsDeferred.await()
        val chartPoints = hourly.ifEmpty { events }
        val days = TideDomainLogic.summarizeDays(chartPoints, events)
        val now = LocalDateTime.now()
        val current = TideDomainLogic.findClosestPoint(chartPoints, now)

        val catalog = cachedCatalog.orEmpty()
        val station = catalog.firstOrNull { it.id == stationId }
            ?: Station(id = stationId, name = stationId, state = "")

        val data = StationData(
            station = station,
            generatedAt = now,
            hourly = chartPoints,
            events = events,
            days = days,
            minValue = chartPoints.minOfOrNull { it.value },
            maxValue = chartPoints.maxOfOrNull { it.value },
            currentValue = current?.value,
            currentTime = current?.time
        )

        stationDataCache.put(stationId, data)
        data
    }
}
