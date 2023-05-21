package ua.com.radiokot.photoprism.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

fun View.fadeIn(duration: Int = context.resources.getInteger(android.R.integer.config_shortAnimTime)) {
    val targetAlpha = alpha
    alpha = 0f
    visibility = View.VISIBLE

    clearAnimation()

    animate()
        .alpha(targetAlpha)
        .setDuration(duration.toLong())
        .setListener(null)
}

fun View.fadeOut(duration: Int = context.resources.getInteger(android.R.integer.config_shortAnimTime)) {
    clearAnimation()
    animate()
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