package ua.com.radiokot.photoprism.base.data.model

class DataPage<T>(
    val items: List<T>,
    val nextCursor: String,
)