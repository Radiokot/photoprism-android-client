package ua.com.radiokot.photoprism.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.navigationrail.NavigationRailView

class UnlimitedNavigationRailView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : NavigationRailView(context, attrs) {
    override fun getMaxItemCount(): Int {
        return Int.MAX_VALUE
    }
}
