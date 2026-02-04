package ua.com.radiokot.photoprism.features.map.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.maplibre.android.maps.AttributionDialogManager
import org.maplibre.android.maps.MapLibreMap

class MaterialAttributionDialogManager(
    private val context: Context,
    maplibreMap: MapLibreMap,
) : AttributionDialogManager(context, maplibreMap) {

    private var dialog: AlertDialog? = null

    @SuppressLint("PrivateResource")
    override fun showAttributionDialog(attributionTitles: Array<out String?>) {
        dialog =
            MaterialAlertDialogBuilder(context)
                .setTitle(org.maplibre.android.R.string.maplibre_attributionsDialogTitle)
                .setAdapter(
                    ArrayAdapter(
                        context,
                        org.maplibre.android.R.layout.maplibre_attribution_list_item,
                        attributionTitles
                    ), this
                )
                .show()
    }

    override fun onStop() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
    }
}
