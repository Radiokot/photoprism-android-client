package ua.com.radiokot.photoprism.features.widgets.photoframe.data.model

import android.content.Context
import android.view.Gravity
import androidx.annotation.GravityInt
import com.squareup.picasso.Transformation
import ua.com.radiokot.photoprism.util.images.ImageTransformations

enum class PhotoFrameWidgetShape {

    ROUNDED_CORNERS {
        override val innerTextGravity: Int =
            Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM

        override fun getTransformation(context: Context) =
            ImageTransformations.roundedCorners(
                cornerRadiusDp = 8,
                context = context,
            )
    },

    FUFA {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.fufa(context)
    },

    SASHA {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.sasha(context)
    },

    LEAF {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.leaf(context)
    },

    BUBA {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.buba(context)
    },

    HEART {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.heart(context)
    },

    NINA {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.nina(context)
    },

    GEAR {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.gear(context)
    },

    VSAUCE {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.vSauce
    },

    HSAUCE {
        override val innerTextGravity: Int =
            Gravity.CENTER

        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.hSauce
    },

    ;

    @get:GravityInt
    abstract val innerTextGravity: Int

    abstract fun getTransformation(context: Context): Transformation
}
