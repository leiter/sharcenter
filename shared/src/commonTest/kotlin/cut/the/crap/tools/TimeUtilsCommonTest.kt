package cut.the.crap.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Timezone-**independent** invariants for the [TimeUtils] formatters and day-boundary math, so they
 * can run on EVERY target — including `iosSimulatorArm64Test`, where there is no
 * `java.util.TimeZone.setDefault` to pin the zone (that is what keeps the exact-string
 * `TimeUtilsTest` in `desktopTest` JVM-only).
 *
 * These exist mainly to exercise the kotlinx-datetime path on Kotlin/Native — the same code that was
 * migrated off the deprecated 0.7 APIs — and to prove the native test toolchain runs at all.
 * The exact rendered strings are pinned separately (and thoroughly) by `desktopTest`.
 */
class TimeUtilsCommonTest {

    // An arbitrary 2026 instant. The exact value is irrelevant — every assertion below is a
    // property that holds in any system time zone.
    private val ts = 1_784_000_000_000L

    @Test
    fun fileNameStampHasSortableShape() {
        // yyyy-MM-dd_HHmmss
        assertTrue(
            Regex("""\d{4}-\d{2}-\d{2}_\d{6}""").matches(formatTimestampForFileName(ts)),
            "unexpected filename stamp: ${formatTimestampForFileName(ts)}",
        )
    }

    @Test
    fun dateOnlyAndCardShapes() {
        assertTrue(Regex("""\d{2}\.\d{2}\.\d{2}""").matches(formatDateOnly(ts)))
        assertTrue(Regex("""\d{2}\.\d{2}\.\d{2}, \d{2}:\d{2}""").matches(formatTimestampWithLocalizedFormatter(ts)))
        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""").matches(formatTimestampIsoLike(ts)))
    }

    @Test
    fun startOfDayIsAtOrBeforeTheInstantAndIdempotent() {
        val start = toStartOfDay(ts)
        assertTrue(start <= ts)
        // A local day is at most 25h (spring-forward/fall-back safe).
        assertTrue(ts - start < 25L * 60 * 60 * 1000)
        assertEquals(start, toStartOfDay(start), "toStartOfDay must be idempotent")
        assertEquals(start, normalizeToStartOfDay(ts), "normalizeToStartOfDay is toStartOfDay")
    }

    @Test
    fun endOfDayIsWithinTheSameLocalDayAndAfterTheInstant() {
        val end = toEndOfDay(ts)
        assertTrue(end >= ts)
        // The end-of-day belongs to the same local day as the start-of-day (DST-safe check).
        assertEquals(toStartOfDay(ts), toStartOfDay(end))
    }

    @Test
    fun fileNameStampsOrderChronologically() {
        // Three days apart guarantees a different calendar day in every time zone, so the
        // lexicographic order of the sortable stamps must match the chronological order.
        val later = ts + 3L * 24 * 60 * 60 * 1000
        assertTrue(formatTimestampForFileName(ts) < formatTimestampForFileName(later))
    }
}
