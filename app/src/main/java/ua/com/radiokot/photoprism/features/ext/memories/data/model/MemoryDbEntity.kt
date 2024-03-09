package ua.com.radiokot.photoprism.features.ext.memories.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import java.util.Date

@Entity(
    "memories",
)
@TypeConverters(MemoryDbEntity.Converters::class)
data class MemoryDbEntity(
    @PrimaryKey
    @ColumnInfo("searchQuery")
    val searchQuery: String,
    @ColumnInfo("is_seen")
    val isSeen: Boolean,
    @ColumnInfo("created_at_ms")
    val createdAtMs: Long,
    @ColumnInfo("preview_hash")
    val previewHash: String,
    @ColumnInfo("type_data")
    val typeData: TypeData,
) {
    class Converters : KoinComponent {
        private val jsonMapper: JsonObjectMapper by inject()

        @TypeConverter
        fun typeDataFromJson(value: String?): TypeData? =
            value?.let(jsonMapper::readValue)

        @TypeConverter
        fun typeDataToJson(value: TypeData?): String? =
            value?.let(jsonMapper::writeValueAsString)
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "_t",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = TypeData.ThisDayInThePast::class, name = "1"),
    )
    sealed class TypeData {
        abstract fun toMemoryTypeData(): Memory.TypeData

        class ThisDayInThePast
        @JsonCreator
        constructor(
            @JsonProperty("year")
            val year: Int
        ) : TypeData() {
            override fun toMemoryTypeData() = Memory.TypeData.ThisDayInThePast(
                year = year,
            )
        }

        companion object {
            fun fromMemoryTypeData(typeData: Memory.TypeData) = when (typeData) {
                is Memory.TypeData.ThisDayInThePast ->
                    ThisDayInThePast(
                        year = typeData.year,
                    )
            }
        }
    }

    constructor(memory: Memory) : this(
        searchQuery = memory.searchQuery,
        isSeen = memory.isSeen,
        createdAtMs = memory.createdAt.time,
        previewHash = memory.previewHash,
        typeData = TypeData.fromMemoryTypeData(memory.typeData),
    )

    fun toMemory(previewUrlFactory: MediaPreviewUrlFactory) = Memory(
        typeData = typeData.toMemoryTypeData(),
        searchQuery = this.searchQuery,
        isSeen = this.isSeen,
        createdAt = Date(this.createdAtMs),
        previewHash = this.previewHash,
        previewUrlFactory = previewUrlFactory,
    )
}
