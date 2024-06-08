package ua.com.radiokot.photoprism.features.ext.store.view.model

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryExtensionStoreItemBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.features.ext.store.CURRENCY_NUMBER_FORMAT
import ua.com.radiokot.photoprism.features.ext.view.GalleryExtensionResources
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

class GalleryExtensionStoreListItem(
    @StringRes
    val title: Int,
    @StringRes
    val description: Int,
    @DrawableRes
    val banner: Int,
    val price: BigDecimal,
    /**
     * ISO-4217 3-letter code.
     */
    currency: String,
    val isBuyButtonVisible: Boolean,
    val isActivatedVisible: Boolean,
    val source: GalleryExtensionStoreItem?,
) : AbstractItem<GalleryExtensionStoreListItem.ViewHolder>() {
    private val currency = Currency.getInstance(currency)

    override val layoutRes: Int
        get() = R.layout.list_item_gallery_extension_store_item

    override val type: Int = layoutRes

    override var identifier = source?.extension?.ordinal?.toLong() ?: -1

    constructor(
        source: GalleryExtensionStoreItem,
    ) : this(
        title = GalleryExtensionResources.getTitle(source.extension),
        description = GalleryExtensionResources.getDescription(source.extension),
        banner = GalleryExtensionResources.getBanner(source.extension),
        price = source.price,
        currency = source.currency,
        isBuyButtonVisible = !source.isAlreadyActivated,
        isActivatedVisible = source.isAlreadyActivated,
        source = source,
    )

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GalleryExtensionStoreListItem>(itemView),
        KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        val view = ListItemGalleryExtensionStoreItemBinding.bind(itemView)
        private val picasso: Picasso by inject()
        private val currencyNumberFormat: NumberFormat by inject(named(CURRENCY_NUMBER_FORMAT))

        override fun bindView(item: GalleryExtensionStoreListItem, payloads: List<Any>) {
            view.bannerImageView.contentDescription =
                view.bannerImageView.context.getString(item.title)
            view.bannerImageView.setImageResource(item.banner)

            view.titleTextView.setText(item.title)
            view.descriptionTextView.setText(item.description)
            view.priceTextView.text = currencyNumberFormat
                .apply { currency = item.currency }
                .format(item.price)

            view.buyButton.isInvisible = !item.isBuyButtonVisible
            view.activatedLabel.isVisible = item.isActivatedVisible
        }

        override fun unbindView(item: GalleryExtensionStoreListItem) {
            picasso.cancelRequest(view.bannerImageView)
        }
    }
}
