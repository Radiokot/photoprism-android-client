package ua.com.radiokot.photoprism.extension

import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * The underlying [RecyclerView] which manages all the pages.
 *
 * @see [ViewPager2.initialize]
 */
val ViewPager2.recyclerView: RecyclerView
    get() = get(0) as RecyclerView