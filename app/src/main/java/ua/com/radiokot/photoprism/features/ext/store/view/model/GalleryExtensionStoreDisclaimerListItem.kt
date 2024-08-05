package ua.com.radiokot.photoprism.features.ext.store.view.model

import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.HtmlCompat
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.koin.core.component.KoinComponent
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryExtensionStoreDisclaimerBinding
import ua.com.radiokot.photoprism.util.InternalLinkMovementMethod
import ua.com.radiokot.photoprism.util.SafeCustomTabs

object GalleryExtensionStoreDisclaimerListItem :
    AbstractItem<GalleryExtensionStoreDisclaimerListItem.ViewHolder>() {
    override val layoutRes: Int =
        R.layout.list_item_gallery_extension_store_disclaimer

    override val type: Int =
        R.layout.list_item_gallery_extension_store_disclaimer

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GalleryExtensionStoreDisclaimerListItem>(itemView),
        KoinComponent {
        val view = ListItemGalleryExtensionStoreDisclaimerBinding.bind(itemView)

        override fun bindView(item: GalleryExtensionStoreDisclaimerListItem, payloads: List<Any>) {
            view.disclaimerTextView.text = HtmlCompat.fromHtml(
                view.root.context.getString(
                    R.string.extension_store_disclaimer,
                ),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            view.disclaimerTextView.movementMethod = InternalLinkMovementMethod { url ->
                when (url) {
                    "#sponsors" -> {
                        // Custom tabs are preferred here,
                        // as the user may want to proceed with donation.
                        SafeCustomTabs.launchWithFallback(
                            context = view.root.context,
                            intent = CustomTabsIntent.Builder()
                                .setShowTitle(false)
                                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                                .setUrlBarHidingEnabled(true)
                                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
                                .build(),
                            url = getKoin().getProperty("sponsorsListUrl")!!,
                            titleRes = R.string.app_name
                        )
                        true
                    }

                    else ->
                        false
                }
            }
        }

        override fun unbindView(item: GalleryExtensionStoreDisclaimerListItem) {
            // No special handling is needed.
        }
    }
}
