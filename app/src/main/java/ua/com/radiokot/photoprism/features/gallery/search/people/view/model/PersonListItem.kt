package ua.com.radiokot.photoprism.features.gallery.search.people.view.model

import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemPersonBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.features.gallery.search.people.data.model.Person
import ua.com.radiokot.photoprism.util.images.CircleImageTransformation

class PersonListItem(
    val name: String?,
    val thumbnailUrl: String,
    val isPersonSelected: Boolean,
    val isNameShown: Boolean,
    val source: Person?,
) : AbstractItem<PersonListItem.ViewHolder>() {
    override val type: Int =
        R.id.list_item_person

    override val layoutRes: Int =
        R.layout.list_item_person

    override var identifier: Long =
        source?.hashCode()?.toLong() ?: -1L

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    constructor(
        source: Person,
        isPersonSelected: Boolean,
        isNameShown: Boolean,
    ) : this(
        name = source.name,
        thumbnailUrl = source.smallThumbnailUrl,
        isPersonSelected = isPersonSelected,
        isNameShown = isNameShown,
        source = source,
    )

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<PersonListItem>(itemView),
        KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val view = ListItemPersonBinding.bind(itemView)
        private val selectedCardBackgroundColor = MaterialColors.getColor(
            itemView,
            com.google.android.material.R.attr.colorSecondaryContainer,
        )
        private val unselectedCardBackgroundColor = Color.TRANSPARENT
        private val picasso: Picasso by inject()

        override fun bindView(item: PersonListItem, payloads: List<Any>) {
            view.imageView.contentDescription = item.name

            picasso
                .load(item.thumbnailUrl)
                .placeholder(R.drawable.image_placeholder_circle)
                .fit()
                .centerCrop()
                .transform(CircleImageTransformation.INSTANCE)
                .into(view.imageView)

            with(view.nameTextView) {
                text = item.name
                isVisible = item.isNameShown
            }

            with(view.listItemPerson) {
                setCardBackgroundColor(
                    if (item.isPersonSelected)
                        selectedCardBackgroundColor
                    else
                        unselectedCardBackgroundColor
                )

                ViewCompat.setTooltipText(this, item.name)
            }
        }

        override fun unbindView(item: PersonListItem) {
            picasso.cancelRequest(view.imageView)
        }
    }
}
