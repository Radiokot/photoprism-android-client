package ua.com.radiokot.photoprism.extension

import android.annotation.SuppressLint
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.view.Menu
import androidx.appcompat.view.menu.MenuBuilder
import ua.com.radiokot.photoprism.R

@SuppressLint("RestrictedApi")
fun MenuBuilder.showOverflowItemIcons(isBottomBar: Boolean) {
    // Enable icons for overflow menu items.
    setOptionalIconsVisible(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // Apply horizontal margin for overflow menu item icons for more pleasant look.
        val iconMarginHorizontal =
            context.resources.getDimensionPixelSize(R.dimen.menu_icon_margin_horizontal)
        visibleItems.forEach { menuItem ->
            if ((isBottomBar && menuItem.requestsActionButton()
                || !isBottomBar && !menuItem.requestsActionButton())
                && menuItem.icon !is InsetDrawable
            ) {
                menuItem.icon = InsetDrawable(
                    menuItem.icon,
                    iconMarginHorizontal, 0, iconMarginHorizontal, 0
                )
            }
        }
    }
}

fun Menu.showOverflowItemIcons(isBottomBar: Boolean) =
    (this as? MenuBuilder)?.showOverflowItemIcons(isBottomBar)
