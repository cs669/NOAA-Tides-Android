package gov.noaa.tides.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gov.noaa.tides.model.DaySummary
import gov.noaa.tides.model.Station
import gov.noaa.tides.model.StationData
import gov.noaa.tides.model.TideFormatters
import gov.noaa.tides.ui.components.WeeklyTideChart
import gov.noaa.tides.ui.theme.CardBorder
import gov.noaa.tides.ui.theme.CardSurface
import gov.noaa.tides.ui.theme.ErrorCoral
import gov.noaa.tides.ui.theme.HeroBgEnd
import gov.noaa.tides.ui.theme.HeroBgStart
import gov.noaa.tides.ui.theme.OceanBgBottom
import gov.noaa.tides.ui.theme.OceanBgMiddle
import gov.noaa.tides.ui.theme.OceanBgTop
import gov.noaa.tides.ui.theme.TextMuted
import gov.noaa.tides.ui.theme.TextPrimary
import gov.noaa.tides.ui.theme.TextSecondary
import gov.noaa.tides.ui.theme.TideAccentMint
import gov.noaa.tides.ui.theme.TideLineBlue
import gov.noaa.tides.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TideDashboardScreen(
    uiState: TideUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectStation: (Station) -> Unit,
    onRefresh: () -> Unit
) {
    var showStationSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OceanBgTop, OceanBgMiddle, OceanBgBottom)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroBanner(
                    onOpenSearch = { showStationSheet = true },
                    onRefresh = onRefresh,
                    isRefreshing = uiState.isLoadingCatalog || uiState.isLoadingData
                )
            }

            item {
                StationSelectorBar(
                    selectedStation = uiState.selectedStation,
                    recentStations = uiState.recentStations,
                    onOpenPicker = { showStationSheet = true },
                    onQuickSelect = onSelectStation
                )
            }

            if (uiState.errorMessage != null) {
                item {
                    ErrorBanner(
                        message = uiState.errorMessage,
                        onRetry = onRefresh
                    )
                }
            }

            if (uiState.isLoadingCatalog && uiState.stationData == null) {
                item {
                    LoadingPlaceholder(message = "Loading NOAA station catalog & predictions...")
                }
            } else if (uiState.stationData != null) {
                val data = uiState.stationData

                item {
                    SummaryMetricsGrid(data = data, isLoading = uiState.isLoadingData)
                }

                item {
                    SectionTitle(title = "Weekly tide curve")
                    Spacer(modifier = Modifier.height(8.dp))
                    WeeklyTideChart(hourly = data.hourly)
                }

                item {
                    SectionTitle(title = "Daily breakdown")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(data.days, key = { it.date.toString() }) { day ->
                            DayForecastCard(day = day)
                        }
                    }
                }

                item {
                    FooterAttribution(station = data.station)
                }
            }
        }

        if (showStationSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showStationSheet = false },
                sheetState = sheetState,
                containerColor = OceanBgMiddle,
                contentColor = TextPrimary
            ) {
                StationSearchSheetContent(
                    searchQuery = uiState.searchQuery,
                    visibleStations = uiState.visibleStations,
                    selectedStation = uiState.selectedStation,
                    noSearchMatches = uiState.noSearchMatches,
                    onSearchQueryChange = onSearchQueryChange,
                    onSelectStation = { station ->
                        onSelectStation(station)
                        showStationSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    onOpenSearch: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(HeroBgStart, HeroBgEnd)
                )
            )
            .border(1.dp, Color(0x1CFFFFFF), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "NOAA TIDE WEEK VIEW",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find a station",
                        tint = TideLineBlue
                    )
                }
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = TideLineBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh predictions",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Seven days of tide data.",
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Real-time NOAA CO-OPS 7-day tide predictions, interactive curve, and daily high/low breakdown.",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun StationSelectorBar(
    selectedStation: Station?,
    recentStations: List<Station>,
    onOpenPicker: () -> Unit,
    onQuickSelect: (Station) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .clickable { onOpenPicker() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = TideLineBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SELECTED NOAA STATION",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = selectedStation?.formatLabel() ?: "Tap to choose a station",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "Change",
                color = TideLineBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (recentStations.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentStations.forEach { station ->
                    val isSelected = station.id == selectedStation?.id
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) TideLineBlue.copy(alpha = 0.22f)
                                else Color(0x0DFFFFFF)
                            )
                            .border(
                                1.dp,
                                if (isSelected) TideLineBlue else CardBorder,
                                CircleShape
                            )
                            .clickable { onQuickSelect(station) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = station.name,
                            color = if (isSelected) TideLineBlue else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricsGrid(
    data: StationData,
    isLoading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMetricCard(
                label = "CURRENT TIDE HEIGHT",
                value = if (isLoading) "..." else TideFormatters.formatHeight(data.currentValue),
                accentColor = TideAccentMint,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = "STATION",
                value = data.station.name,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMetricCard(
                label = "FORECAST SPAN",
                value = "7 days",
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = "UPDATED",
                value = TideFormatters.formatUpdated(data.generatedAt),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 0.9.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DayForecastCard(day: DaySummary) {
    Column(
        modifier = Modifier
            .width(210.dp)
            .heightIn(min = 215.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x09FFFFFF))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(15.dp)
    ) {
        Text(
            text = TideFormatters.formatDayLabel(day.date),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${day.events.size} tide events",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

        TideRowItem(
            label = "High",
            value = TideFormatters.formatHeight(day.max?.value)
        )
        TideRowItem(
            label = "Low",
            value = TideFormatters.formatHeight(day.min?.value)
        )

        day.events.forEach { point ->
            TideRowItem(
                label = "${point.displayKind} ${TideFormatters.formatClock(point.time)}",
                value = TideFormatters.formatHeight(point.value)
            )
        }
    }
}

@Composable
private fun TideRowItem(label: String, value: String) {
    Column {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 7.dp),
            color = Color(0x14FFFFFF)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 13.sp
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FooterAttribution(station: Station) {
    val stateLabel = station.state.ifBlank { "the NOAA catalog" }
    Text(
        text = buildAnnotatedString {
            append("NOAA predictions for station ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                append(station.id)
            }
            append(" in $stateLabel. Data source: NOAA CO-OPS predictions API.")
        },
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ErrorCoral.copy(alpha = 0.14f))
            .border(1.dp, ErrorCoral.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = TideLineBlue,
                contentColor = OceanBgBottom
            )
        ) {
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoadingPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = TideLineBlue)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StationSearchSheetContent(
    searchQuery: String,
    visibleStations: List<Station>,
    selectedStation: Station?,
    noSearchMatches: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectStation: (Station) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 420.dp, max = 640.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Find a NOAA Station",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "City, station name, state, or station id",
                    color = TextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TideLineBlue
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = TideLineBlue,
                unfocusedBorderColor = CardBorder,
                cursorColor = TideLineBlue
            )
        )

        if (noSearchMatches) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No matching stations yet. Try a shorter city name or a station id.",
                color = WarningAmber,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            items(visibleStations, key = { it.id }) { station ->
                val isSelected = station.id == selectedStation?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) TideLineBlue.copy(alpha = 0.16f)
                            else Color(0x0DFFFFFF)
                        )
                        .border(
                            1.dp,
                            if (isSelected) TideLineBlue else CardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectStation(station) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = buildList {
                                if (station.state.isNotBlank()) add(station.state)
                                add("ID: ${station.id}")
                            }.joinToString(" · "),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (isSelected) {
                        Text(
                            text = "Active",
                            color = TideAccentMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
