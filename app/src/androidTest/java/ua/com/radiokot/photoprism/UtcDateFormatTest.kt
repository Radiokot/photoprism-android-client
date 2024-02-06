package ua.com.radiokot.photoprism

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import ua.com.radiokot.photoprism.di.UTC_DATE_TIME_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DATE_TIME_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DAY_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DAY_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.dateFormatModule
import java.text.DateFormat

@RunWith(AndroidJUnit4::class)
class UtcDateFormatTest {
    @Test
    fun ensureTimeZones() {
        val koin = koinApplication {
            modules(dateFormatModule)
        }.koin

        listOf(
            UTC_MONTH_DATE_FORMAT,
            UTC_MONTH_YEAR_DATE_FORMAT,
            UTC_DAY_DATE_FORMAT,
            UTC_DAY_YEAR_DATE_FORMAT,
            UTC_DATE_TIME_DATE_FORMAT,
            UTC_DATE_TIME_YEAR_DATE_FORMAT,
            UTC_DATE_TIME_YEAR_DATE_FORMAT,
        ).forEach { dateFormatName ->
            val format = koin.get<DateFormat>(named(dateFormatName))

            Assert.assertTrue(
                "$dateFormatName format must have the UTC timezone",
                format.timeZone.id == "UTC"
            )
        }
    }
}
