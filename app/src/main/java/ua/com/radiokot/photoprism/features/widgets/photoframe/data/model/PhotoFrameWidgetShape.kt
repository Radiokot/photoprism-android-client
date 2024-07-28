package ua.com.radiokot.photoprism.features.widgets.photoframe.data.model

import android.content.Context
import com.squareup.picasso.Transformation
import ua.com.radiokot.photoprism.util.images.ImageTransformations

enum class PhotoFrameWidgetShape {

    ROUNDED_CORNERS {
        override fun getTransformation(context: Context) =
            ImageTransformations.roundedCorners(
                cornerRadiusDp = 8,
                context = context,
            )
    },

    FUFA {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.fufa(context)
    },

    SASHA {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.sasha(context)
    },

    LEAF {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.leaf(context)
    },

    BUBA {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.buba(context)
    },

    HEART {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.heart(context)
    },

    NINA {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.nina(context)
    },

    GEAR {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.gear(context)
    },

    VSAUCE {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.vSauce
    },

    HSAUCE {
        override fun getTransformation(context: Context): Transformation =
            ImageTransformations.hSauce
    },

    ;

    abstract fun getTransformation(context: Context): Transformation
}
