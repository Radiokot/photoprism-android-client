package ua.com.radiokot.photoprism.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible

private val fadeInterpolator = AccelerateDecelerateInterpolator()
private val scaleInterpolator = AccelerateDecelerateInterpolator()

fun View.fadeIn(duration: Int = context.resources.getInteger(android.R.integer.config_shortAnimTime)) {
    val targetAlpha =
        if (!isVisible && alpha != 0f)
            alpha
        else
            1f

    alpha = 0f
    visibility = View.VISIBLE

    clearAnimation()
    animate()
        .setInterpolator(fadeInterpolator)
        .alpha(targetAlpha)
        .setDuration(duration.toLong())
        .setListener(null)
}

fun View.fadeOut(duration: Int = context.resources.getInteger(android.R.integer.config_shortAnimTime)) {
    clearAnimation()
    animate()
        .setInterpolator(fadeInterpolator)
        .alpha(0f)
        .setDuration(duration.toLong())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
            }
        })
}

fun View.fadeVisibility(
    isVisible: Boolean,
    duration: Int = context.resources.getInteger(android.R.integer.config_shortAnimTime)
) {
    if (isVisible) {
        fadeIn(duration)
    } else {
        fadeOut(duration)
    }
}

fun View.animateScale(
    target: Float,
    duration: Int = context.resources.getInteger(android.R.integer.config_shortAnimTime),
) {
    clearAnimation()
    animate()
        .scaleX(target)
        .scaleY(target)
        .setDuration(duration.toLong())
        .setInterpolator(scaleInterpolator)
        .setListener(null)
}
