package ua.com.radiokot.photoprism.util

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.core.content.edit
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence

/**
 * @return A subject which holds the value read from the preferences and writes new values
 * when they are posted with [BehaviorSubject.onNext].
 *
 * @param preferences preferences to read and write the values to
 * @param key preference key to read and write the values by
 * @param readValue value reader from [SharedPreferences]
 * @param writeValue value writer to [SharedPreferences.Editor],
 * the write is applied rather than committed.
 */
@SuppressLint("CheckResult")
inline fun <reified ValueType : Any> preferenceSubject(
    preferences: SharedPreferences,
    key: String,
    readValue: SharedPreferences.(key: String) -> ValueType,
    crossinline writeValue: SharedPreferences.Editor.(key: String, value: ValueType) -> Unit,
): BehaviorSubject<ValueType> =
    BehaviorSubject.createDefault(preferences.readValue(key)).apply {
        skip(1).subscribe { newValue ->
            preferences.edit {
                writeValue(key, newValue)
            }
        }
    }

/**
 * @return a [preferenceSubject] for a boolean value
 *
 * @param preferences preferences to read and write the values to
 * @param key preference key to read and write the values by
 * @param defaultValue value to be held if missing one in the preferences
 * @param onValuePut optional callback executed once a value is put to the preferences
 */
fun booleanPreferenceSubject(
    preferences: SharedPreferences,
    key: String,
    defaultValue: Boolean,
    onValuePut: ((key: String, newValue: Boolean) -> Unit)? = null,
): BehaviorSubject<Boolean> =
    preferenceSubject(
        preferences = preferences,
        key = key,
        readValue = { getBoolean(key, defaultValue) },
        writeValue = { _, newValue ->
            putBoolean(key, newValue)
            onValuePut?.invoke(key, newValue)
        },
    )

/**
 * @return a [preferenceSubject] for a value which can be (de-)serialized from/to string.
 *
 * @param preferences preferences to read and write the values to
 * @param key preference key to read and write the values by
 * @param defaultValue value to be held if missing one in the preferences or failing deserialize
 * @param stringSerializer serializer for the value
 * @param stringDeserializer deserializer for the string value,
 * if returns **null** then [defaultValue] is used
 */
inline fun <reified ValueType : Any> stringifyPreferenceSubject(
    preferences: SharedPreferences,
    key: String,
    defaultValue: ValueType,
    crossinline stringSerializer: (value: ValueType) -> String,
    stringDeserializer: (valueString: String) -> ValueType?,
): BehaviorSubject<ValueType> =
    preferenceSubject(
        preferences = preferences,
        key = key,
        readValue = {
            getString(key, null)
                ?.let(stringDeserializer)
                ?: defaultValue
        },
        writeValue = { _, newValue ->
            putString(key, stringSerializer(newValue))
        }
    )

/**
 * @return A subject which holds the value read from the persistence and writes new values
 * when they are posted with [BehaviorSubject.onNext].
 *
 * @param persistence persistence to read and write the values to
 * @param defaultValue value to be held if missing one in the persistence
 */
@SuppressLint("CheckResult")
inline fun <reified ValueType : Any> objectPersistenceSubject(
    persistence: ObjectPersistence<ValueType>,
    defaultValue: ValueType,
): BehaviorSubject<ValueType> =
    BehaviorSubject.createDefault(persistence.loadItem() ?: defaultValue).apply {
        skip(1).subscribe { newValue ->
            persistence.saveItem(newValue)
        }
    }

