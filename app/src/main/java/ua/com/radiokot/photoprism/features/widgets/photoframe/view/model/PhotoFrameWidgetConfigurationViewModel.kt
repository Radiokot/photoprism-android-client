package ua.com.radiokot.photoprism.features.widgets.photoframe.view.model

import android.appwidget.AppWidgetManager
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetWorker
import ua.com.radiokot.photoprism.util.BackPressActionsStack

class PhotoFrameWidgetConfigurationViewModel(
    val searchViewModel: GallerySearchViewModel,
    allowedMediaTypes: Set<GalleryMedia.TypeName>,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val workManager: WorkManager,
) : ViewModel() {
    private val log = kLogger("PhotoFrameWidgetConfigurationVM")

    private val selectedShapeSubject: BehaviorSubject<PhotoFrameWidgetShape> = BehaviorSubject.create()
    val selectedShape = selectedShapeSubject.observeOnMain().distinctUntilChanged()
    val isDateShown: MutableLiveData<Boolean> = MutableLiveData()
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.observeOnMain()
    private val backPressActionsStack = BackPressActionsStack()
    val backPressedCallback: OnBackPressedCallback =
        backPressActionsStack.onBackPressedCallback
    private val closeSearchConfigurationOnBackPress = {
        searchViewModel.switchBackFromConfiguring()
    }
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var appliedSearchConfig: SearchConfig? = null

    init {
        searchViewModel.availableMediaTypes.value = allowedMediaTypes
    }

    private var isInitialized = false
    fun initOnce(widgetId: Int) {
        if (isInitialized) {
            return
        }

        this.widgetId = widgetId
        selectedShapeSubject.onNext(widgetsPreferences.getShape(widgetId))
        isDateShown.value = widgetsPreferences.isDateShown(widgetId)

        widgetsPreferences.getSearchConfig(widgetId)
            ?.also(searchViewModel::applySearchConfig)
        subscribeToSearch()

        log.debug {
            "initOnce(): initialized:" +
                    "\nwidgetId=$widgetId"
        }

        isInitialized = true
    }

    private fun subscribeToSearch() = searchViewModel.state.subscribe { state ->
        log.debug {
            "subscribeToSearch(): received_new_state:" +
                    "\nstate=$state"
        }

        appliedSearchConfig = when (state) {
            is GallerySearchViewModel.State.Applied ->
                state.search.config

            is GallerySearchViewModel.State.Configuring ->
                state.alreadyAppliedSearch?.config

            GallerySearchViewModel.State.NoSearch ->
                null
        }

        when (state) {
            is GallerySearchViewModel.State.Applied -> {
                backPressActionsStack.removeAction(closeSearchConfigurationOnBackPress)
            }

            GallerySearchViewModel.State.NoSearch -> {
                backPressActionsStack.removeAction(closeSearchConfigurationOnBackPress)
            }

            is GallerySearchViewModel.State.Configuring -> {
                // Make the back button press close the search configuration view.
                backPressActionsStack.pushUniqueAction(closeSearchConfigurationOnBackPress)
            }
        }

        log.debug {
            "subscribeToSearch(): handled_new_state:" +
                    "\nstate=$state"
        }
    }.autoDispose(this)

    fun onShapeClicked(shape: PhotoFrameWidgetShape) {
        log.debug {
            "onShapeClicked: selecting:" +
                    "\nshape=$shape"
        }

        selectedShapeSubject.onNext(shape)
    }

    fun onDoneClicked() {
        savePreferences()
        updateAndFinish()
    }

    fun onCancelClicked() {
        log.debug {
            "updateAndFinish(): finishing_not_successfully"
        }

        eventsSubject.onNext(
            Event.Finish(
                isConfigurationSuccessful = false,
            )
        )
    }

    private fun savePreferences() {
        val shape = checkNotNull(selectedShapeSubject.value) {
            "The shape must be selected at this moment"
        }
        val isDateShown = isDateShown.value == true
        val searchConfig = appliedSearchConfig

        widgetsPreferences.setShape(widgetId, shape)
        widgetsPreferences.setSearchConfig(widgetId, searchConfig)
        widgetsPreferences.setDateShown(widgetId, isDateShown)

        log.debug {
            "savePreferences(): preferences_saved:" +
                    "\nwidgetId=$widgetId," +
                    "\nshape=$shape," +
                    "\nsearchConfig=$searchConfig," +
                    "\nisDateShown=$isDateShown"
        }
    }

    private fun updateAndFinish() {
        // This work request is immediate and should not have any constraints.
        // If it fails – it fails now and doesn't interfere
        // with further periodic updates.
        val workRequest = OneTimeWorkRequestBuilder<UpdatePhotoFrameWidgetWorker>()
            .setInputData(
                UpdatePhotoFrameWidgetWorker.getInputData(
                    widgetId = widgetId,
                )
            )
            .addTag(UpdatePhotoFrameWidgetWorker.TAG)
            .build()

        workManager
            .enqueueUniqueWork(
                "${UpdatePhotoFrameWidgetWorker.TAG}:$widgetId:immediate",
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )

        log.debug {
            "updateAndFinish(): enqueued_immediate_update"
        }

        log.debug {
            "updateAndFinish(): finishing_with_success"
        }

        eventsSubject.onNext(
            Event.Finish(
                isConfigurationSuccessful = true,
            )
        )
    }

    sealed interface Event {
        class Finish(
            val isConfigurationSuccessful: Boolean,
        ) : Event
    }
}
