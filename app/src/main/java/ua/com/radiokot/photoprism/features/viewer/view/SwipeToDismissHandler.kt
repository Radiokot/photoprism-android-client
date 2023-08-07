/*
 * Copyright (c) 2023 Oleg Koretsky
 * Copyright (c) 2020 New Vector Ltd
 * Copyright (C) 2018 stfalcon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua.com.radiokot.photoprism.features.viewer.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator

class SwipeToDismissHandler(
    private val swipeView: View,
    private val onDismiss: () -> Unit,
    private val onSwipeViewMove: (translationY: Float, translationLimit: Int) -> Unit,
    private val shouldAnimateDismiss: () -> Boolean = { false },
) : View.OnTouchListener {

    var distanceThreshold: Int = swipeView.height / 4

    private var isTracking = false
    private var startY: Float = 0f
    private val animationDuration: Long by lazy {
        swipeView.context.resources
            .getInteger(android.R.integer.config_shortAnimTime)
            .toLong()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (swipeView.hitRect.contains(event.x.toInt(), event.y.toInt())) {
                    isTracking = true
                }
                startY = event.y
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTracking) {
                    isTracking = false
                    onTrackingEnd(v.height)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTracking) {
                    val translationY = event.y - startY
                    swipeView.translationY = translationY
                    onSwipeViewMove(translationY, distanceThreshold)
                }
                return true
            }

            else -> {
                return false
            }
        }
    }

    private fun onTrackingEnd(parentHeight: Int) {
        val animateTo = when {
            swipeView.translationY < -distanceThreshold ->
                -parentHeight.toFloat()

            swipeView.translationY > distanceThreshold ->
                parentHeight.toFloat()

            else ->
                0f
        }

        if (animateTo != 0f && !shouldAnimateDismiss()) {
            onDismiss()
        } else {
            animateTranslation(animateTo)
        }
    }

    private fun animateTranslation(translationTo: Float) {
        swipeView.animate()
            .translationY(translationTo)
            .setDuration(animationDuration)
            .setInterpolator(AccelerateInterpolator())
            .setUpdateListener { onSwipeViewMove(swipeView.translationY, distanceThreshold) }
            .setAnimatorEndListener {
                if (translationTo != 0f) {
                    onDismiss()
                }

                // remove the update listener, otherwise it will be saved on the next animation execution:
                swipeView.animate().setUpdateListener(null)
            }
            .start()
    }
}

private fun ViewPropertyAnimator.setAnimatorEndListener(
    onAnimationEnd: () -> Unit,
) = setListener(
    object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            onAnimationEnd()
        }
    }
)

private val View.hitRect: Rect
    get() = Rect().also(::getHitRect)
