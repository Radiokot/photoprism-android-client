package ua.com.radiokot.photoprism.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ViewErrorBinding
import java.lang.ref.WeakReference

class ErrorView
@JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val viewBinding: ViewErrorBinding

    private var currentError: Error? = null
    private var viewsToReplace = mutableListOf<WeakReference<View>>()

    init {
        viewBinding = ViewErrorBinding.inflate(LayoutInflater.from(context), this, true)
        hide()
    }

    fun showError(error: Error) {
        visibility = View.VISIBLE
        currentError?.unbindView(viewBinding)
        currentError = error

        viewsToReplace.forEach { reference ->
            reference.get()?.visibility = View.INVISIBLE
        }

        error.bindView(viewBinding)
    }

    fun hide() {
        visibility = View.GONE

        viewsToReplace.forEach { reference ->
            reference.get()?.visibility = View.VISIBLE
        }

        currentError?.unbindView(viewBinding)
    }

    /**
     * Set views to make [View.INVISIBLE] while error is shown
     */
    fun replaces(vararg views: View) = apply {
        viewsToReplace += views.map(::WeakReference)
    }

    override fun onDetachedFromWindow() {
        currentError?.unbindView(viewBinding)
        super.onDetachedFromWindow()
    }

    interface Error {
        fun bindView(view: ViewErrorBinding)

        fun unbindView(view: ViewErrorBinding) {
            // Special handling is not always needed.
        }

        class General(
            val message: String,
            @DrawableRes
            val imageRes: Int = R.drawable.image_error,
            val retryButtonText: String? = null,
            val retryButtonClickListener: (() -> Unit)? = null
        ) : Error {
            constructor(
                context: Context,
                @StringRes
                messageRes: Int,
                @StringRes
                retryButtonTextRes: Int? = null,
                retryButtonClickListener: (() -> Unit)? = null
            ) : this(
                message = context.getString(messageRes),
                retryButtonText = retryButtonTextRes?.let(context::getString),
                retryButtonClickListener = retryButtonClickListener,
            )

            override fun bindView(view: ViewErrorBinding) {
                with(view) {
                    messageTextView.text = message

                    with(retryButton) {
                        text = retryButtonText
                        if (retryButtonClickListener != null) {
                            visibility = View.VISIBLE
                            setOnClickListener { retryButtonClickListener.invoke() }
                        } else {
                            visibility = View.GONE
                            setOnClickListener(null)
                        }
                    }

                    imageView.setImageDrawable(
                        ContextCompat.getDrawable(
                            imageView.context,
                            imageRes
                        )
                    )
                }
            }
        }

        class EmptyView(
            val message: String,
        ) : Error {
            constructor(
                context: Context,
                @StringRes
                messageRes: Int,
            ) : this(
                message = context.getString(messageRes),
            )

            override fun bindView(view: ViewErrorBinding) {
                with(view) {
                    messageTextView.text = message
                    retryButton.visibility = View.GONE
                    imageView.setImageDrawable(
                        ContextCompat.getDrawable(
                            imageView.context,
                            R.drawable.image_empty
                        )
                    )
                }
            }
        }
    }
}
