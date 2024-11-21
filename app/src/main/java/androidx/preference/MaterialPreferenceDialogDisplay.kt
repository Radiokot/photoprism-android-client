package androidx.preference

import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback

/**
 * An [OnPreferenceDisplayDialogCallback] implementation which displays
 * Material3 dialogs for corresponding preferences.
 */
class MaterialPreferenceDialogDisplay : OnPreferenceDisplayDialogCallback {

    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        preference: Preference
    ): Boolean = when (preference) {
        is ListPreference -> {
            showDialog(
                caller,
                MaterialListPreferenceDialogFragmentCompat.newInstance(preference.key),
            )
            true
        }

        is EditTextPreference -> {
            showDialog(
                caller,
                MaterialEditTextPreferenceDialogFragmentCompat.newInstance(preference.key),
            )
            true
        }

        // Another dialog types to be added when needed.
        else -> false
    }

    @Suppress("DEPRECATION")
    private fun showDialog(
        caller: PreferenceFragmentCompat,
        dialogFragment: DialogFragment,
    ) =
        dialogFragment
            .apply { setTargetFragment(caller, 0) }
            .show(
                caller.parentFragmentManager,
                DIALOG_TAG
            )


    private companion object {
        private const val DIALOG_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}
