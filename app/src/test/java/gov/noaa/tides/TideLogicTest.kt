package gov.noaa.tides

import gov.noaa.tides.model.Station
import gov.noaa.tides.model.TideDomainLogic
import gov.noaa.tides.model.TideFormatters
import gov.noaa.tides.model.TidePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TideLogicTest {

    @Test
    fun searchStations_prioritizesExactIdPrefixAndNamePrefix() {
        val stations = listOf(
            Station(id = "9414290", name = "San Francisco", state = "CA"),
            Station(id = "8518750", name = "The Battery", state = "NY"),
            Station(id = "9410170", name = "San Diego", state = "CA")
        )

        val byId = TideDomainLogic.searchStations("8518", stations)
        assertEquals(1, byId.size)
        assertEquals("8518750", byId.first().id)

        val byName = TideDomainLogic.searchStations("San", stations)
        assertEquals(2, byName.size)
        assertEquals("San Diego", byName[0].name)
        assertEquals("San Francisco", byName[1].name)
    }

    @Test
    fun summarizeDays_groupsHourlyAndEventsByDay() {
        val day1 = LocalDate.of(2026, 11, 1)
        val hourly = listOf(
            TidePoint(time = LocalDateTime.of(2026, 11, 1, 1, 0), value = 1.2),
            TidePoint(time = LocalDateTime.of(2026, 11, 1, 6, 0), value = 5.8),
            TidePoint(time = LocalDateTime.of(2026, 11, 1, 12, 0), value = 0.4)
        )
        val events = listOf(
            TidePoint(time = LocalDateTime.of(2026, 11, 1, 6, 12), value = 5.85, kind = "H"),
            TidePoint(time = LocalDateTime.of(2026, 11, 1, 12, 45), value = 0.35, kind = "L")
        )

        val summaries = TideDomainLogic.summarizeDays(hourly, events)
        assertEquals(1, summaries.size)
        val summary = summaries.first()
        assertEquals(day1, summary.date)
        assertEquals(2, summary.events.size)
        assertNotNull(summary.min)
        assertNotNull(summary.max)
        assertEquals("0.40 ft", TideFormatters.formatHeight(summary.min?.value))
        assertEquals("5.80 ft", TideFormatters.formatHeight(summary.max?.value))
    }
}
