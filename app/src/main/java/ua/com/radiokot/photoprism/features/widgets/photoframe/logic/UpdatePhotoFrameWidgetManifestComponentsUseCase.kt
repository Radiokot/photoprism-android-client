package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import android.content.Context
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.setManifestComponentEnabled
import ua.com.radiokot.photoprism.featureflags.extension.hasPhotoFrameWidget
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.PhotoFrameWidgetConfigurationActivity
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.PhotoFrameWidgetProvider

class UpdatePhotoFrameWidgetManifestComponentsUseCase(
    private val featureFlags: FeatureFlags,
    private val context: Context,
) {
    private val log = kLogger("UpdatePhotoFrameWidgetManifestComponentsUseCase")

    fun invoke() {
        val areComponentsEnabled = featureFlags.hasPhotoFrameWidget

        listOf(
            PhotoFrameWidgetProvider::class.java,
            PhotoFrameWidgetConfigurationActivity::class.java,
        ).forEach { component ->
            log.debug(
                "invoke(): setting_component_enabled:" +
                        "\ncomponent=${component.simpleName}" +
                        "\nisEnabled=$areComponentsEnabled"
            )

            context.setManifestComponentEnabled(component, areComponentsEnabled)
        }
    }
}
