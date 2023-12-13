package com.example.testcomposethierry.ui.view_models


import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.testcomposethierry.BuildConfig
import com.example.testcomposethierry.data.config.AppConfig
import com.example.testcomposethierry.data.models.DataArtElement
import com.example.testcomposethierry.data.repositories.ArtDataRepository
import com.example.testcomposethierry.data.repositories.ArtDataPagingSource
import com.example.testcomposethierry.data.repositories.PersistentDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState {
    object Empty : UiState()
    object InProgress : UiState()
    class Error(val error: Throwable) : UiState()
    object Filled : UiState()
}

@HiltViewModel
class ArtViewModel @Inject constructor(
    private val artDataRepository: ArtDataRepository,
    private val persistentDataManager: PersistentDataManager,
) : ViewModel() {
    // the list of packages installed on the device
    lateinit var listArt: Flow<PagingData<DataArtElement>>
    // The UI state for showing the first page with a spinner or not
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Empty)
    val uiState: StateFlow<UiState>
        get() = _uiState.asStateFlow()
    private val _activeRow: MutableStateFlow<Int> = MutableStateFlow(-1)
    val activeRow: StateFlow<Int>
        get() = _activeRow.asStateFlow()

    // ------------------------------------------
    // non flow variables
    private var initialized = false

    fun init(unexpectedServerDataErrorString: String) {
        if (initialized) {
            return
        }
        initialized = true

        // one can add a RemoteMediator for caching
        // https://developer.android.com/topic/libraries/architecture/paging/v3-network-db
        listArt = Pager(PagingConfig(pageSize = AppConfig.pagingSize)) {
            ArtDataPagingSource(unexpectedServerDataErrorString, artDataRepository)
        }.flow
    }

    fun setActiveRow(owner: LifecycleOwner, rowId: Int) {
        owner.lifecycleScope.launch {
            _activeRow.emit(rowId)
        }
    }

    suspend fun setUiState(newUiState: UiState) {
        if (BuildConfig.DEBUG && newUiState is UiState.Error) {
            println(newUiState.error.message)
        }
        if (newUiState != _uiState) {
            _uiState.emit(newUiState)
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistentDataManager.close()
    }
}
