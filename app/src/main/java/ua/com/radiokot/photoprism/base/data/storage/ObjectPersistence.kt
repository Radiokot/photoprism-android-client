package ua.com.radiokot.photoprism.base.data.storage

interface ObjectPersistence<T : Any> {
    fun loadItem(): T?
    fun saveItem(item: T)
    fun hasItem(): Boolean
    fun clear()
}