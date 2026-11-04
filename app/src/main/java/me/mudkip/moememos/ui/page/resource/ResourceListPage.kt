package me.mudkip.moememos.ui.page.resource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredGridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.Attachment
import me.mudkip.moememos.ui.component.MemoImage
import me.mudkip.moememos.ui.theme.rememberMoeHazeStyle
import me.mudkip.moememos.viewmodel.ResourceListViewModel
import me.mudkip.moememos.ui.theme.moePageColor
import me.mudkip.moememos.ui.theme.LocalHazeState

private enum class ResourceFilter {
    IMAGE,
    OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceListPage(
    navController: NavHostController,
    viewModel: ResourceListViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedFilter by rememberSaveable { mutableStateOf(ResourceFilter.IMAGE) }
    val imageResources = viewModel.resources.filter { it.mimeType?.startsWith("image/") == true }
    val otherResources = viewModel.resources.filterNot { it.mimeType?.startsWith("image/") == true }
    val ownHazeState = rememberHazeState()
    val hazeState = LocalHazeState.current ?: ownHazeState
    val hazeStyle = rememberMoeHazeStyle()

    Scaffold(
        containerColor = moePageColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,

    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopAppBarDefaults.TopAppBarExpandedHeight + 12.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        selected = selectedFilter == ResourceFilter.IMAGE,
                        onClick = { selectedFilter = ResourceFilter.IMAGE },
                        label = { Text(R.string.image.string) }
                    )
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        selected = selectedFilter == ResourceFilter.OTHER,
                        onClick = { selectedFilter = ResourceFilter.OTHER },
                        label = { Text(R.string.other.string) }
                    )
                }

                if (selectedFilter == ResourceFilter.IMAGE) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp,
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        staggeredGridItems(imageResources, key = { it.identifier }) { resource ->
                            MemoImage(
                                url = resource.localUri ?: resource.uri,
                                resourceIdentifier = resource.identifier,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        lazyItems(otherResources, key = { it.identifier }) { resource ->
                            Attachment(resource = resource)
                        }
                    }
                }
            }
                TopAppBar(
                    modifier = Modifier.align(Alignment.TopCenter).hazeEffect(hazeState, hazeStyle),
                    title = { Text(text = R.string.resources.string) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = R.string.back.string)
                        }
                    },
                )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadResources()
    }
}
