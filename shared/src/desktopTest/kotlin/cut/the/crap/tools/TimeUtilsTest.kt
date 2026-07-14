package cut.the.crap.tools

import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

/**
 * Pins the timestamp formats across the `java.time` → kotlinx-datetime swap.
 *
 * These strings are on screen constantly — every link and post card shows one — and a formatting
 * change is exactly the kind of bug that compiles, passes every other test, and is only visible by
 * looking at the running app (as WP2's plural bug was). So the output is asserted literally.
 *
 * The system zone is pinned to UTC for the duration: the production code reads
 * `TimeZone.currentSystemDefault()`, so without this the expectations would depend on where the
 * test happened to run.
 */
class TimeUtilsTest {

    private lateinit var original: TimeZone

    @Before
    fun fixTimeZone() {
        original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(original)
    }

    private val instant = Instant.parse("2026-07-13T21:27:04Z").toEpochMilliseconds()

    @Test
    fun `card timestamp renders as dd MM yy comma HH mm`() {
        assertEquals("13.07.26, 21:27", formatTimestampWithLocalizedFormatter(instant))
    }

    @Test
    fun `date only renders as dd MM yy`() {
        assertEquals("13.07.26", formatDateOnly(instant))
    }

    @Test
    fun `filename stamp is zero-padded and sortable`() {
        assertEquals("2026-07-13_212704", formatTimestampForFileName(instant))
    }

    @Test
    fun `iso-like stamp keeps seconds`() {
        assertEquals("2026-07-13 21:27:04", formatTimestampIsoLike(instant))
    }

    @Test
    fun `single-digit day and month keep their leading zeros`() {
        // The whole point of Padding.ZERO — "1.2.26" would be wrong.
        val early = Instant.parse("2026-02-01T05:06:07Z").toEpochMilliseconds()
        assertEquals("01.02.26", formatDateOnly(early))
        assertEquals("01.02.26, 05:06", formatTimestampWithLocalizedFormatter(early))
        assertEquals("2026-02-01_050607", formatTimestampForFileName(early))
    }

    @Test
    fun `start of day is midnight local`() {
        val start = toStartOfDay(instant)
        assertEquals("13.07.26, 00:00", formatTimestampWithLocalizedFormatter(start))
    }

    @Test
    fun `end of day is the last millisecond, not the next day`() {
        val end = toEndOfDay(instant)
        assertEquals("13.07.26, 23:59", formatTimestampWithLocalizedFormatter(end))
        // 23:59:59.999 — one millisecond short of midnight.
        assertEquals(toStartOfDay(instant) + 86_400_000 - 1, end)
    }

    @Test
    fun `normalizeToStartOfDay is toStartOfDay`() {
        // It always was; the Calendar version just took a different route there.
        assertEquals(toStartOfDay(instant), normalizeToStartOfDay(instant))
    }

    @Test
    fun `a day range covers exactly one day`() {
        assertEquals(86_400_000L - 1, toEndOfDay(instant) - toStartOfDay(instant))
    }
}
