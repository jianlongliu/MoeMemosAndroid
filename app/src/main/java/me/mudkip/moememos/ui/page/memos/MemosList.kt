package me.mudkip.moememos.ui.page.memos

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState
import me.mudkip.moememos.ui.theme.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemoEditGesture
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Settings
import me.mudkip.moememos.ext.settingsDataStore
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.ArchivedMemoCard
import me.mudkip.moememos.ui.component.MemosCard
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.util.edgeToEdgeContentPadding
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.util.hasCustomTag
import me.mudkip.moememos.util.memoMatchesSearch
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.ManualSyncResult
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemosList(
    contentPadding: PaddingValues,
    hazeState: HazeState? = null,
    lazyListState: LazyListState = rememberLazyListState(),
    tag: String? = null,
    searchString: String? = null,
    additionalBottomPadding: Dp = 16.dp,
    onRefresh: (suspend () -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    onMemoClick: ((String) -> Unit)? = null,
    archivedResults: List<MemoEntity> = emptyList(),
) {
    val context = LocalContext.current
    val shellHazeState = LocalHazeState.current
    val resolvedHazeState = hazeState ?: shellHazeState
    val navController = LocalRootNavController.current
    val viewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val settings by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = Settings())
    val editGesture = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.editGesture
    val refreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var visibilityFilter by rememberSaveable { mutableStateOf(MemoVisibilityFilter.ALL) }
    var syncAlert by remember { mutableStateOf<PullRefreshSyncAlert?>(null) }
    val memoSnapshot by remember {
        derivedStateOf { viewModel.memos.toList() }
    }
    val filteredMemos = remember(memoSnapshot, tag, searchString, visibilityFilter) {
        val pinned = memoSnapshot.filter { it.pinned }
        val nonPinned = memoSnapshot.filter { !it.pinned }
        var fullList = pinned + nonPinned

        tag?.let { tag ->
            fullList = fullList.filter { memo ->
                hasCustomTag(memo.content, tag)
            }
        }

        searchString?.let { searchString ->
            if (searchString.isNotEmpty()) {
                fullList = fullList.filter { memo ->
                    memoMatchesSearch(memo.content, searchString)
                }
            }
        }

        fullList = fullList.filter { memo ->
            when (visibilityFilter) {
                MemoVisibilityFilter.ALL -> true
                MemoVisibilityFilter.PUBLIC -> memo.visibility == MemoVisibility.PUBLIC
                MemoVisibilityFilter.PROTECTED ->
                    memo.visibility == MemoVisibility.PROTECTED || memo.visibility == MemoVisibility.SPACE
                MemoVisibilityFilter.PRIVATE -> memo.visibility == MemoVisibility.PRIVATE
            }
        }

        fullList
    }
    var listTopId: String? by rememberSaveable {
        mutableStateOf(null)
    }
    val listContentPadding = edgeToEdgeContentPadding(
        contentPadding,
        additionalBottomPadding
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                if (onRefresh != null) {
                    onRefresh()
                } else {
                    when (val result = viewModel.refreshMemos()) {
                        ManualSyncResult.Completed -> Unit
                        is ManualSyncResult.Blocked -> {
                            syncAlert = PullRefreshSyncAlert.Blocked(result.message)
                        }
                        is ManualSyncResult.RequiresConfirmation -> {
                            syncAlert = PullRefreshSyncAlert.RequiresConfirmation(result.version, result.message)
                        }
                        is ManualSyncResult.Failed -> {
                            syncAlert = PullRefreshSyncAlert.Failed(result.message)
                        }
                    }
                }
                isRefreshing = false
            }
        },
        state = refreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(contentPadding)
                .then(
                    if (resolvedHazeState != null) {
                        Modifier.hazeSource(state = resolvedHazeState)
                    } else {
                        Modifier
                    }
                ),
            state = lazyListState,
            contentPadding = listContentPadding
        ) {
            item(key = "visibility-filter") {
                VisibilityFilterRow(
                    selected = visibilityFilter,
                    onSelect = { visibilityFilter = it },
                )
            }
            if (filteredMemos.isEmpty() && archivedResults.isEmpty()) {
                item(key = "empty") {
                    Text(stringResource(R.string.no_memos), modifier = Modifier.padding(24.dp))
                }
            }
            items(filteredMemos, key = { it.identifier }) { memo ->
                MemosCard(
                    memo = memo,
                    onClick = { selectedMemo ->
                        if (onMemoClick != null) {
                            onMemoClick(selectedMemo.identifier)
                        } else navController.navigate(
                            "${RouteName.MEMO_DETAIL}?memoId=${Uri.encode(selectedMemo.identifier)}"
                        )
                    },
                    editGesture = editGesture ?: MemoEditGesture.NONE,
                    previewMode = true,
                    showSyncStatus = currentAccount !is Account.Local,
                    onTagClick = onTagClick
                )
            }
            if (archivedResults.isNotEmpty()) {
                item(key = "archived-header") {
                    Text(
                        stringResource(R.string.archived),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                items(archivedResults, key = { "archived-${it.identifier}" }) { memo ->
                    ArchivedMemoCard(memo)
                }
            }
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Timber.d(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMemos()
    }

    LaunchedEffect(filteredMemos.firstOrNull()?.identifier) {
        if (listTopId != null && filteredMemos.isNotEmpty() && listTopId != filteredMemos.first().identifier) {
            lazyListState.scrollToItem(0)
        }

        listTopId = filteredMemos.firstOrNull()?.identifier
    }

    when (val alert = syncAlert) {
        null -> Unit
        is PullRefreshSyncAlert.Blocked -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                }
            )
        }
        is PullRefreshSyncAlert.RequiresConfirmation -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            syncAlert = null
                            scope.launch {
                                when (val result = viewModel.refreshMemos(alert.version)) {
                                    ManualSyncResult.Completed -> Unit
                                    is ManualSyncResult.Blocked -> {
                                        syncAlert = PullRefreshSyncAlert.Blocked(result.message)
                                    }
                                    is ManualSyncResult.RequiresConfirmation -> {
                                        syncAlert = PullRefreshSyncAlert.RequiresConfirmation(result.version, result.message)
                                    }
                                    is ManualSyncResult.Failed -> {
                                        syncAlert = PullRefreshSyncAlert.Failed(result.message)
                                    }
                                }
                            }
                        }
                    ) {
                        Text(R.string.still_sync.string)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.cancel.string)
                    }
                }
            )
        }
        is PullRefreshSyncAlert.Failed -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.sync_failed.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                }
            )
        }
    }
}

private sealed class PullRefreshSyncAlert {
    data class Blocked(val message: String) : PullRefreshSyncAlert()
    data class RequiresConfirmation(val version: String, val message: String) : PullRefreshSyncAlert()
    data class Failed(val message: String) : PullRefreshSyncAlert()
}

@Composable
private fun VisibilityFilterRow(
    selected: MemoVisibilityFilter,
    onSelect: (MemoVisibilityFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemoVisibilityFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes)) },
            )
        }
    }
}

private enum class MemoVisibilityFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    PUBLIC(R.string.filter_public),
    PROTECTED(R.string.filter_protected),
    PRIVATE(R.string.filter_private),
}
