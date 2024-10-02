package ua.com.radiokot.photoprism.features.gallery.search.logic

import java.util.Locale

/**
 * Contains different conditions of matching some query with given fields.
 */
object SearchPredicates {
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /**
     * Query is being lowercased and split by whitespace.
     * Match is confirmed if all query parts are matched with given fields
     * by [String.startsWith] condition.
     *
     * For example, for person's fields "John" (first name) and "Doe" (last name) the following
     * queries will be matched: "john", "jo", "j", "doe", "d", "j d", "jo do", "john doe", "doe john", etc.
     * The same match condition is implemented in Android contacts app.
     *
     * @param query search query
     * @param fields entity fields to match query
     */
    fun generalCondition(query: String, vararg fields: String?): Boolean =
        generalCondition(query, fields.toList())

    /**
     * Query is being lowercased and split by whitespace.
     * Match is confirmed if all query parts are matched with given fields
     * by [String.startsWith] condition.
     *
     * For example, for person's fields "John" (first name) and "Doe" (last name) the following
     * queries will be matched: "john", "jo", "j", "doe", "d", "j d", "jo do", "john doe", "doe john", etc.
     * The same match condition is implemented in Android contacts app.
     *
     * @param query search query
     * @param fields entity fields to match query
     */
    fun generalCondition(query: String, fields: Collection<String?>): Boolean {
        val unmatchedFieldsParts = fields.flatMapTo(mutableSetOf()) { field ->
            if (field != null)
                splitByWhitespace(field.lowercase(Locale.getDefault()))
            else
                emptySet()
        }

        val unmatchedQueryParts =
            splitByWhitespace(query.lowercase(Locale.getDefault())).toMutableList()
        var unmatchedChanged = true
        while (unmatchedFieldsParts.isNotEmpty()
            && unmatchedQueryParts.isNotEmpty()
            && unmatchedChanged
        ) {
            val unmatchedFieldsPartsIterator = unmatchedFieldsParts.iterator()
            unmatchedChanged = false
            while (unmatchedFieldsPartsIterator.hasNext()) {
                val fieldPart = unmatchedFieldsPartsIterator.next()

                val partsIterator = unmatchedQueryParts.iterator()
                while (partsIterator.hasNext()) {
                    val queryPart = partsIterator.next()

                    if (fieldPart.startsWith(queryPart, true)) {
                        partsIterator.remove()
                        unmatchedFieldsPartsIterator.remove()
                        unmatchedChanged = true
                        break
                    }
                }
            }
        }

        return unmatchedQueryParts.isEmpty()
    }

    private fun splitByWhitespace(text: String): Collection<String> {
        return text.split(WHITESPACE_REGEX)
    }
}
