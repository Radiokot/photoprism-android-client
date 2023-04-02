package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.features.gallery.data.model.photoPrismDateFormat
import ua.com.radiokot.photoprism.features.gallery.logic.GalleryMonthsSequence
import java.text.SimpleDateFormat
import java.util.*

class GalleryMonthsSequenceTest {
    @Test
    fun generateForRange() {
        val format = photoPrismDateFormat.clone() as SimpleDateFormat
        format.timeZone = TimeZone.getTimeZone("UTC+3")
        val endDate = format.parse("2023-04-01T00:05:10Z")!!
        val startDate = format.parse("2021-02-15T22:06:45Z")!!

        val months = GalleryMonthsSequence(
            startDate = startDate,
            endDate = endDate,
        ).toList()

        Assert.assertEquals(27, months.size)

        Assert.assertEquals(1612130400000, months.first().firstDay.time)
        Assert.assertEquals(1614549600000, months.first().nextDayAfter.time)

        Assert.assertEquals(1646085600000, months[13].firstDay.time)
        Assert.assertEquals(1648760400000, months[13].nextDayAfter.time)

        Assert.assertEquals(1680296400000, months.last().firstDay.time)
        Assert.assertEquals(1682888400000, months.last().nextDayAfter.time)
    }

    @Test
    fun generateForSingleDate() {
        val format = photoPrismDateFormat.clone() as SimpleDateFormat
        format.timeZone = TimeZone.getTimeZone("UTC+3")
        val date = format.parse("2023-04-01T00:05:10Z")!!

        val months = GalleryMonthsSequence(
            startDate = date,
            endDate = date
        ).toList()

        Assert.assertEquals(1, months.size)

        Assert.assertEquals(1680296400000, months.first().firstDay.time)
        Assert.assertEquals(1682888400000, months.first().nextDayAfter.time)
    }

    @Test(expected = IllegalArgumentException::class)
    fun incorrectRange() {
        GalleryMonthsSequence(
            startDate = Date(),
            endDate = Date(System.currentTimeMillis() - 360000)
        )
    }
}