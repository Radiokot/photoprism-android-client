package ua.com.radiokot.photoprism.features.ext.marketplace.view.model

import android.view.View
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
import ua.com.radiokot.photoprism.databinding.ListItemGalleryExtensionMarketplaceItemBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.ext.marketplace.CURRENCY_NUMBER_FORMAT
import ua.com.radiokot.photoprism.features.ext.view.GalleryExtensionResources
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

class GalleryExtensionMarketplaceListItem(
    @StringRes
    val title: Int,
    @StringRes
    val description: Int,
    val bannerUrl: String,
    val price: BigDecimal,
    /**
     * ISO-4217 3-letter code.
     */
    currency: String,
    val isBuyButtonVisible: Boolean,
    val isActivatedVisible: Boolean,
    val source: GalleryExtensionMarketplaceItem?,
) : AbstractItem<GalleryExtensionMarketplaceListItem.ViewHolder>() {
    private val currency = Currency.getInstance(currency)

    override val layoutRes: Int
        get() = R.layout.list_item_gallery_extension_marketplace_item

    override val type: Int = layoutRes

    override var identifier = source?.extension?.ordinal?.toLong() ?: -1

    constructor(
        source: GalleryExtensionMarketplaceItem,
    ) : this(
        title = GalleryExtensionResources.getTitle(source.extension),
        description = GalleryExtensionResources.getDescription(source.extension),
        bannerUrl = GalleryExtensionResources.getBannerUrl(source.extension),
        price = source.price,
        currency = source.currency,
        isBuyButtonVisible = !source.isAlreadyActivated,
        isActivatedVisible = source.isAlreadyActivated,
        source = source,
    )

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GalleryExtensionMarketplaceListItem>(itemView),
        KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemGalleryExtensionMarketplaceItemBinding.bind(itemView)
        private val picasso: Picasso by inject()
        private val currencyNumberFormat: NumberFormat by inject(named(CURRENCY_NUMBER_FORMAT))

        override fun bindView(item: GalleryExtensionMarketplaceListItem, payloads: List<Any>) {
            view.bannerImageView.contentDescription =
                view.bannerImageView.context.getString(item.title)

            picasso
                .load(item.bannerUrl)
                .hardwareConfigIfAvailable()
                .placeholder(R.drawable.image_placeholder)
                .fit()
                .centerCrop()
                .into(view.bannerImageView)

            view.titleTextView.setText(item.title)
            view.descriptionTextView.setText(item.description)
            view.priceTextView.text = currencyNumberFormat
                .apply { currency = item.currency }
                .format(item.price)

            view.buyButton.isInvisible = !item.isBuyButtonVisible
            view.activatedLabel.isVisible = item.isActivatedVisible
        }

        override fun unbindView(item: GalleryExtensionMarketplaceListItem) {
            picasso.cancelRequest(view.bannerImageView)
        }
    }
}
