package gov.noaa.tides.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gov.noaa.tides.data.NoaaApiService
import gov.noaa.tides.data.RecentStationsStore
import gov.noaa.tides.data.TideRepository
import gov.noaa.tides.model.Station
import gov.noaa.tides.model.StationData
import gov.noaa.tides.model.TideDomainLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TideUiState(
    val isLoadingCatalog: Boolean = true,
    val isLoadingData: Boolean = false,
    val catalog: List<Station> = emptyList(),
    val recentStations: List<Station> = emptyList(),
    val visibleStations: List<Station> = emptyList(),
    val searchQuery: String = "",
    val noSearchMatches: Boolean = false,
    val selectedStation: Station? = null,
    val stationData: StationData? = null,
    val errorMessage: String? = null
)

class TideViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TideRepository(
        apiService = NoaaApiService(),
        recentStationsStore = RecentStationsStore(application.applicationContext)
    )

    private val _uiState = MutableStateFlow(TideUiState())
    val uiState: StateFlow<TideUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    fun initialize(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val recent = repository.getRecentStations()
            _uiState.update {
                it.copy(
                    isLoadingCatalog = true,
                    errorMessage = null,
                    recentStations = recent
                )
            }

            try {
                val catalog = repository.getStationCatalog(forceRefresh = forceRefresh)
                if (catalog.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingCatalog = false,
                            errorMessage = "Unable to load the NOAA station catalog. Check your network connection and try again."
                        )
                    }
                    return@launch
                }

                val defaultList = recent.ifEmpty { catalog.take(25) }
                val initialStation = _uiState.value.selectedStation
                    ?: defaultList.firstOrNull()
                    ?: catalog.first()

                val updatedRecent = repository.rememberStation(initialStation)

                _uiState.update { state ->
                    val computed = computeVisibleStations(
                        query = state.searchQuery,
                        catalog = catalog,
                        recentStations = updatedRecent
                    )
                    state.copy(
                        isLoadingCatalog = false,
                        catalog = catalog,
                        recentStations = updatedRecent,
                        visibleStations = computed.first,
                        noSearchMatches = computed.second,
                        selectedStation = initialStation
                    )
                }

                fetchStationPredictions(initialStation, forceRefresh = forceRefresh)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCatalog = false,
                        errorMessage = "Unable to load the NOAA station catalog. Check your network connection and try again."
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val (visible, noMatches) = computeVisibleStations(
                query = query,
                catalog = state.catalog,
                recentStations = state.recentStations
            )
            state.copy(
                searchQuery = query,
                visibleStations = visible,
                noSearchMatches = noMatches
            )
        }
    }

    fun selectStation(station: Station, forceRefresh: Boolean = false) {
        val updatedRecent = repository.rememberStation(station)
        _uiState.update { state ->
            val (visible, noMatches) = computeVisibleStations(
                query = state.searchQuery,
                catalog = state.catalog,
                recentStations = updatedRecent
            )
            state.copy(
                selectedStation = station,
                recentStations = updatedRecent,
                visibleStations = visible,
                noSearchMatches = noMatches,
                errorMessage = null
            )
        }
        fetchStationPredictions(station, forceRefresh = forceRefresh)
    }

    fun refreshCurrentStation() {
        val station = _uiState.value.selectedStation
        if (station == null) {
            initialize(forceRefresh = true)
        } else {
            fetchStationPredictions(station, forceRefresh = true)
        }
    }

    private fun fetchStationPredictions(station: Station, forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingData = true, errorMessage = null) }
            try {
                val data = repository.loadStationData(station.id, forceRefresh = forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoadingData = false,
                        stationData = data.copy(station = station),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingData = false,
                        errorMessage = "Failed to load predictions for ${station.name} (${station.id}). Please try again."
                    )
                }
            }
        }
    }

    private fun computeVisibleStations(
        query: String,
        catalog: List<Station>,
        recentStations: List<Station>
    ): Pair<List<Station>, Boolean> {
        val trimmed = query.trim()
        val fallback = recentStations.ifEmpty { catalog.take(25) }
        if (trimmed.isEmpty()) {
            return fallback to false
        }
        val matches = TideDomainLogic.searchStations(trimmed, catalog, limit = 30)
        return if (matches.isEmpty()) {
            fallback to true
        } else {
            matches to false
        }
    }
}
