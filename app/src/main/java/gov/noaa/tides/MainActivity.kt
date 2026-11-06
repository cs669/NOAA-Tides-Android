package gov.noaa.tides

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import gov.noaa.tides.ui.TideDashboardScreen
import gov.noaa.tides.ui.TideViewModel
import gov.noaa.tides.ui.theme.NOAATidesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NOAATidesTheme {
                val tideViewModel: TideViewModel = viewModel()
                val uiState by tideViewModel.uiState.collectAsStateWithLifecycle()

                TideDashboardScreen(
                    uiState = uiState,
                    onSearchQueryChange = tideViewModel::onSearchQueryChange,
                    onSelectStation = tideViewModel::selectStation,
                    onRefresh = tideViewModel::refreshCurrentStation
                )
            }
        }
    }
}
