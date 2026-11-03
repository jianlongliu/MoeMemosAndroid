package me.mudkip.moememos.viewmodel

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.constant.MemosVersionSupport
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.DailyUsageStat
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.SyncStatus
import me.mudkip.moememos.data.service.AccountService
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.getErrorMessage
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.widget.WidgetUpdater
import java.time.LocalDate
import java.time.OffsetDateTime
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemosViewModel @Inject constructor(
    private val memoService: MemoService,
    private val accountService: AccountService,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    var memos = mutableStateListOf<MemoEntity>()
        private set
    var tags = mutableStateListOf<String>()
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set
    var matrix by mutableStateOf(DailyUsageStat.initialMatrix)
        private set

    val host: StateFlow<String?> =
        accountService.currentAccount
            .map { it?.getAccountInfo()?.host }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val syncStatus: StateFlow<SyncStatus> =
        memoService.syncStatus.stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus())

    private val initialLoad = MutableStateFlow(false)
    private var allMemosLoaded = false

    private companion object {
        const val INITIAL_PAGE_SIZE = 80
        const val PAGE_SIZE = 120
    }

    init {
        snapshotFlow { memos.toList() }
            .onEach { matrix = calculateMatrix() }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                loadMemosSnapshot()
            } finally {
                initialLoad.value = true
            }

            memoService.syncStatus
                .map { it.syncing }
                .distinctUntilChanged()
                .collectLatest { syncing ->
                    if (syncing) {
                        return@collectLatest
                    }
                    memoService.memos.collectLatest { latestMemos ->
                        if (allMemosLoaded) {
                            applyMemos(latestMemos)
                        } else {
                            val loadedIds = memos.mapTo(HashSet()) { it.identifier }
                            applyMemos(latestMemos.filter { it.identifier in loadedIds })
                        }
                    }
                }
        }
    }

    private suspend fun loadMemosSnapshot(limit: Int? = INITIAL_PAGE_SIZE) {
        val startedAt = SystemClock.elapsedRealtime()
        val repo = memoService.getRepository()
        val response = if (limit != null) repo.listMemos(limit, 0) else repo.listMemos()
        val queryMs = SystemClock.elapsedRealtime() - startedAt
        when (response) {
            is ApiResponse.Success -> {
                val applyStartedAt = SystemClock.elapsedRealtime()
                applyMemos(response.data)
                val applyMs = SystemClock.elapsedRealtime() - applyStartedAt
                Timber.d("Loaded ${response.data.size} memos from local cache in ${SystemClock.elapsedRealtime() - startedAt}ms (query=${queryMs}ms, apply=${applyMs}ms, paged=${limit != null})")
                if (limit != null && response.data.size == limit) {
                    loadRemainingMemos(startOffset = response.data.size)
                } else {
                    allMemosLoaded = true
                }
            }
            else -> {
                errorMessage = response.getErrorMessage()
            }
        }
    }

    private fun loadRemainingMemos(startOffset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var offset = startOffset
            while (true) {
                val response = memoService.getRepository().listMemos(PAGE_SIZE, offset)
                if (response !is ApiResponse.Success) {
                    return@launch
                }
                val page = response.data
                if (page.isEmpty()) {
                    withContext(Dispatchers.Main) { allMemosLoaded = true }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    appendMemos(page)
                }
                offset += page.size
                if (page.size < PAGE_SIZE) {
                    withContext(Dispatchers.Main) { allMemosLoaded = true }
                    return@launch
                }
                delay(40)
            }
        }
    }

    private fun appendMemos(page: List<MemoEntity>) {
        if (page.isEmpty()) return
        val existingIds = memos.mapTo(HashSet()) { it.identifier }
        memos.addAll(page.filter { it.identifier !in existingIds })
        errorMessage = null
    }

    suspend fun refreshLocalSnapshot() = withContext(viewModelScope.coroutineContext) {
        loadMemosSnapshot(limit = null)
    }

    suspend fun awaitInitialLoad() {
        initialLoad.first { it }
    }

    private fun applyMemos(latestMemos: List<MemoEntity>) {
        memos.clear()
        memos.addAll(latestMemos)
        errorMessage = null
    }

    suspend fun loadMemos(syncAfterLoad: Boolean = true) = withContext(viewModelScope.coroutineContext) {
        if (syncAfterLoad) {
            val compatibility = accountService.checkCurrentAccountSyncCompatibility(isAutomatic = true)
            if (compatibility !is AccountService.SyncCompatibility.Allowed) {
                return@withContext
            }

            val syncResult = memoService.sync(false)
            if (syncResult is ApiResponse.Success) {
                WidgetUpdater.updateWidgets(appContext)
            } else {
                if (!syncResult.isAccessTokenInvalidFailure()) {
                    errorMessage = syncResult.getErrorMessage()
                }
            }
        }
    }

    suspend fun refreshMemos(allowHigherV1Version: String? = null): ManualSyncResult = withContext(viewModelScope.coroutineContext) {
        when (val compatibility = accountService.checkCurrentAccountSyncCompatibility(
            isAutomatic = false,
            allowHigherV1Version = allowHigherV1Version
        )) {
            is AccountService.SyncCompatibility.Blocked -> {
                return@withContext ManualSyncResult.Blocked(
                    compatibility.message ?: MemosVersionSupport.supportedVersionsMessage(appContext)
                )
            }
            is AccountService.SyncCompatibility.RequiresConfirmation -> {
                return@withContext ManualSyncResult.RequiresConfirmation(
                    version = compatibility.version,
                    message = compatibility.message
                )
            }
            AccountService.SyncCompatibility.Allowed -> Unit
        }

        val syncResult = memoService.sync(true)
        if (syncResult is ApiResponse.Success) {
            if (allowHigherV1Version != null) {
                accountService.rememberAcceptedUnsupportedSyncVersion(allowHigherV1Version)
            }
            WidgetUpdater.updateWidgets(appContext)
        } else {
            val message = syncResult.getErrorMessage()
            errorMessage = message
            return@withContext ManualSyncResult.Failed(message)
        }
        ManualSyncResult.Completed
    }

    private fun ApiResponse<Unit>.isAccessTokenInvalidFailure(): Boolean {
        return this is ApiResponse.Failure.Exception && this.throwable == MoeMemosException.accessTokenInvalid
    }

    fun loadTags() = viewModelScope.launch {
        memoService.getRepository().listTags().suspendOnSuccess {
            tags.clear()
            tags.addAll(data)
        }
    }

    suspend fun updateMemoPinned(memoIdentifier: String, pinned: Boolean) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().updateMemo(memoIdentifier, pinned = pinned).suspendOnSuccess {
            updateMemo(data)
            // Update widgets after pinning/unpinning a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun editMemo(memoIdentifier: String, content: String, resourceList: List<ResourceEntity>?, visibility: MemoVisibility): ApiResponse<MemoEntity> = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().updateMemo(memoIdentifier, content, resourceList, visibility).suspendOnSuccess {
            updateMemo(data)
            // Update widgets after editing a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun archiveMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().archiveMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            // Update widgets after archiving a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun deleteMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().deleteMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            // Update widgets after deleting a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun cacheResourceFile(resourceIdentifier: String, downloadedUri: Uri): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().cacheResourceFile(resourceIdentifier, downloadedUri)
    }

    suspend fun getResourceById(resourceIdentifier: String): ResourceEntity? = withContext(viewModelScope.coroutineContext) {
        when (val response = memoService.getRepository().listResources()) {
            is ApiResponse.Success -> response.data.firstOrNull { it.identifier == resourceIdentifier }
            else -> null
        }
    }

    private fun updateMemo(memo: MemoEntity) {
        val index = memos.indexOfFirst { it.identifier == memo.identifier }
        if (index != -1) {
            memos[index] = memo
        }
    }

    private fun calculateMatrix(): List<DailyUsageStat> {
        val countMap = HashMap<LocalDate, Int>()

        for (memo in memos) {
            val date = memo.date.atZone(OffsetDateTime.now().offset).toLocalDate()
            countMap[date] = (countMap[date] ?: 0) + 1
        }

        return DailyUsageStat.initialMatrix.map {
            it.copy(count = countMap[it.date] ?: 0)
        }
    }
}

val LocalMemos =
    compositionLocalOf<MemosViewModel> { error(me.mudkip.moememos.R.string.memos_view_model_not_found.string) }

sealed class ManualSyncResult {
    object Completed : ManualSyncResult()
    data class Blocked(val message: String) : ManualSyncResult()
    data class RequiresConfirmation(val version: String, val message: String) : ManualSyncResult()
    data class Failed(val message: String) : ManualSyncResult()
}
