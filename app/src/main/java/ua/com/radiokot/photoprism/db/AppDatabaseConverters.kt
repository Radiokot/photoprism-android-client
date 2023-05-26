package ua.com.radiokot.photoprism.db

import androidx.room.TypeConverter
import com.fasterxml.jackson.module.kotlin.jsonMapper

object AppDatabaseConverters {
    @TypeConverter
    @JvmStatic
    fun stringListToString(list: List<String>?): String? =
        if (list == null)
            null
        else
            jsonMapper().writeValueAsString(list)

    @TypeConverter
    @JvmStatic
    fun stringListFromString(string: String?): List<String>? =
        if (string == null)
            null
        else
            jsonMapper().readerForListOf(String::class.java).readValue(string)
}