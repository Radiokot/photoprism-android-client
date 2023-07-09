package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.features.gallery.data.model.parsePhotoPrismDate
import ua.com.radiokot.photoprism.features.gallery.logic.GalleryMonthsSequence
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Date

class GalleryMonthsSequenceTest {
    @Test
    fun generateForRange() {
        val startDate = LocalDate(parsePhotoPrismDate("2021-02-15T22:06:45Z")!!)
        val endDate = LocalDate(parsePhotoPrismDate("2023-04-01T00:05:10Z")!!)

        val months = GalleryMonthsSequence(
            startLocalDate = startDate,
            endLocalDate = endDate,
        ).toList()

        Assert.assertEquals(27, months.size)

        Assert.assertEquals("LocalDate(2021-02-01T00:00:00Z)", months.first().firstDay.toString())
        Assert.assertEquals("LocalDate(2021-03-01T00:00:00Z)", months.first().nextDayAfter.toString())

        Assert.assertEquals("LocalDate(2022-03-01T00:00:00Z)", months[13].firstDay.toString())
        Assert.assertEquals("LocalDate(2022-04-01T00:00:00Z)", months[13].nextDayAfter.toString())

        Assert.assertEquals("LocalDate(2023-04-01T00:00:00Z)", months.last().firstDay.toString())
        Assert.assertEquals("LocalDate(2023-05-01T00:00:00Z)", months.last().nextDayAfter.toString())
    }

    @Test
    fun generateForSingleDate() {
        val date = LocalDate(parsePhotoPrismDate("2023-04-01T00:05:10Z")!!)

        val months = GalleryMonthsSequence(
            startLocalDate = date,
            endLocalDate = date
        ).toList()

        Assert.assertEquals(1, months.size)

        Assert.assertEquals("LocalDate(2023-04-01T00:00:00Z)", months.first().firstDay.toString())
        Assert.assertEquals("LocalDate(2023-05-01T00:00:00Z)", months.first().nextDayAfter.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun incorrectRange() {
        GalleryMonthsSequence(
            startLocalDate = LocalDate(Date()),
            endLocalDate = LocalDate(Date(System.currentTimeMillis() - 360000))
        )
    }
}
