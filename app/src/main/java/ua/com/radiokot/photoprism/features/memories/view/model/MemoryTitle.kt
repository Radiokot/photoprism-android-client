package ua.com.radiokot.photoprism.features.memories.view.model

import android.content.Context
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.memories.data.model.Memory

sealed class MemoryTitle {

    abstract fun getString(context: Context): String

    class YearsAgo(
        val years: Int,
    ) : MemoryTitle() {
        override fun getString(context: Context) = context.resources.getQuantityString(
            R.plurals.years_ago,
            years,
            years,
        )
    }

    companion object {
        fun forMemory(memory: Memory) = when (val typeData = memory.typeData) {
            is Memory.TypeData.ThisDayInThePast ->
                YearsAgo(
                    years = typeData.yearsAgo,
                )
        }
    }
}
