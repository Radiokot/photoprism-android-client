package ua.com.radiokot.photoprism.features.ext.store.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.HttpUrl
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.ext.data.model.ActivatedGalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.ext.store.data.model.GalleryExtensionOnSale
import ua.com.radiokot.photoprism.features.ext.store.data.storage.GalleryExtensionStorePreferences
import ua.com.radiokot.photoprism.features.ext.store.data.storage.GalleryExtensionsOnSaleRepository

class GalleryExtensionStoreViewModel(
    private val extensionsOnSaleRepository: GalleryExtensionsOnSaleRepository,
    private val galleryExtensionsStateRepository: GalleryExtensionsStateRepository,
    private val storePreferences: GalleryExtensionStorePreferences,
    private val onlinePurchaseUrlFactory: (extension: GalleryExtension) -> HttpUrl,
) : ViewModel() {
    private val log = kLogger("ExtensionStoreVM")

    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    val isLoading = MutableLiveData(false)
    val itemsList = MutableLiveData<List<GalleryExtensionStoreListItem>>()
    val mainError = MutableLiveData<Error?>(null)
    val isDisclaimerVisible = MutableLiveData(!storePreferences.isDisclaimerAccepted)

    init {
        update()

        subscribeToRepositories()
    }

    private fun subscribeToRepositories() {
        Observable.combineLatest(
            extensionsOnSaleRepository
                .items
                .filter { extensionsOnSaleRepository.isFresh }
                .map { extensionsOnSale ->
                    extensionsOnSale
                        .associateBy { it.extension.ordinal }
                },
            galleryExtensionsStateRepository
                .state
                .map { extensionsState ->
                    extensionsState.activatedExtensions
                        .mapTo(mutableSetOf(), ActivatedGalleryExtension::type)
                },
        ) { extensionsOnSaleByIndex: Map<Int, GalleryExtensionOnSale>,
            activatedExtensions: Set<GalleryExtension> ->

            GalleryExtension.values().mapNotNull { galleryExtension ->
                // Do not show extensions not on sale.
                val onSale = extensionsOnSaleByIndex[galleryExtension.ordinal]
                    ?: return@mapNotNull null

                GalleryExtensionStoreItem(
                    extension = galleryExtension,
                    price = onSale.price,
                    currency = onSale.currency,
                    pageUrl = onSale.pageUrl,
                    isAlreadyActivated = galleryExtension in activatedExtensions,
                )
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { extensionStoreItems ->
                itemsList.postValue(extensionStoreItems.map(::GalleryExtensionStoreListItem))
            }
            .autoDispose(this)

        extensionsOnSaleRepository.errors
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { error ->
                log.error(error) {
                    "subscribeToRepositories(): extensions_on_sale_loading_failed"
                }

                if (itemsList.value == null) {
                    mainError.value = Error.LoadingFailed
                } else {
                    eventsSubject.onNext(Event.ShowFloatingLoadingFailedError)
                }
            }
            .autoDispose(this)

        extensionsOnSaleRepository.loading
            .subscribe(isLoading::postValue)
            .autoDispose(this)

    }

    private fun update(force: Boolean = false) {
        log.debug {
            "update(): updating:" +
                    "\nforce=$force"
        }

        if (force) {
            extensionsOnSaleRepository.update()
        } else {
            extensionsOnSaleRepository.updateIfNotFresh()
        }
    }

    fun onRetryClicked() {
        log.debug {
            "onRetryClicked(): updating"
        }

        update(force = true)
    }

    fun onSwipeRefreshPulled() {
        log.debug {
            "onSwipeRefreshPulled(): force_updating"
        }

        update(force = true)
    }

    fun onBuyNowClicked(listItem: GalleryExtensionStoreListItem) {
        log.debug {
            "onBuyNowClicked(): buy_now_clicked:" +
                    "\nlistItem=$listItem"
        }

        listItem.source?.also { item ->
            val purchaseUrl = onlinePurchaseUrlFactory(item.extension).toString()

            log.debug {
                "onItemCardClicked(): opening_online_purchase:" +
                        "\nitem=$item," +
                        "\npurchaseUrl=$purchaseUrl"
            }

            eventsSubject.onNext(Event.OpenOnlinePurchase(purchaseUrl))
        }
    }

    fun onItemCardClicked(listItem: GalleryExtensionStoreListItem) {
        log.debug {
            "onItemCardClicked(): item_card_clicked:" +
                    "\nlistItem=$listItem"
        }

        listItem.source?.also { item ->
            val pageUrl = item.pageUrl

            log.debug {
                "onItemCardClicked(): opening_extension_page:" +
                        "\nitem=$item," +
                        "\npageUrl=$pageUrl"
            }

            eventsSubject.onNext(
                Event.OpenExtensionPage(
                    extension = item.extension,
                    url = pageUrl,
                )
            )
        }
    }

    fun onDisclaimerGotItClicked() {
        log.debug {
            "onDisclaimerGotItClicked(): got_it_clicked"
        }

        storePreferences.isDisclaimerAccepted = true
        isDisclaimerVisible.postValue(false)
    }

    fun onActivateKeyClicked() {
        log.debug {
            "onActivateKeyClicked(): activate_key_clicked"
        }

        eventsSubject.onNext(Event.OpenKeyActivation)
    }

    sealed interface Event {
        /**
         * Show a dismissible floating error saying that the loading is failed.
         * Retry is possible: the [onRetryClicked] method should be called.
         */
        object ShowFloatingLoadingFailedError : Event

        class OpenOnlinePurchase(val url: String) : Event

        class OpenExtensionPage(
            val extension: GalleryExtension,
            val url: String,
        ) : Event

        object OpenKeyActivation : Event
    }

    sealed interface Error {
        /**
         * The loading is failed and could be retried.
         * The [onRetryClicked] method should be called.
         */
        object LoadingFailed : Error
    }
}
