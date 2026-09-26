@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import io.music_assistant.client.api.ErrorMessageBus
import io.music_assistant.client.data.model.client.ClickContext
import io.music_assistant.client.data.model.client.MediaType
import io.music_assistant.client.data.model.client.items.Album
import io.music_assistant.client.data.model.client.items.Artist
import io.music_assistant.client.data.model.client.items.Audiobook
import io.music_assistant.client.data.model.client.items.Genre
import io.music_assistant.client.data.model.client.items.Playlist
import io.music_assistant.client.data.model.client.items.Podcast
import io.music_assistant.client.data.model.client.items.RecommendationFolder
import io.music_assistant.client.input.VolumeButtonService
import io.music_assistant.client.ui.compose.common.ToastDuration
import io.music_assistant.client.ui.compose.common.ToastHost
import io.music_assistant.client.ui.compose.common.providers.ProviderIcon
import io.music_assistant.client.ui.compose.common.rememberToastState
import io.music_assistant.client.ui.compose.common.viewmodel.ActionsViewModel
import io.music_assistant.client.ui.compose.home.players.DspSettingsViewModel
import io.music_assistant.client.ui.compose.home.players.PlayersPager
import io.music_assistant.client.ui.compose.item.ItemDetailsScreen
import io.music_assistant.client.ui.compose.item.ItemDetailsViewModel
import io.music_assistant.client.ui.compose.item.ItemListScreen
import io.music_assistant.client.ui.compose.item.ItemListViewModel
import io.music_assistant.client.ui.compose.item.ViewModeViewModel
import io.music_assistant.client.ui.compose.library.AiRadioScreen
import io.music_assistant.client.ui.compose.library.AiRadioViewModel
import io.music_assistant.client.ui.compose.library.BrowseScreen
import io.music_assistant.client.ui.compose.library.BrowseViewModel
import io.music_assistant.client.ui.compose.library.LibraryCategoriesViewModel
import io.music_assistant.client.ui.compose.library.LibraryCategory
import io.music_assistant.client.ui.compose.library.LibraryListScreen
import io.music_assistant.client.ui.compose.library.LibraryListViewModel
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.library.LibraryScreenState
import io.music_assistant.client.ui.compose.nav.BackHandler
import io.music_assistant.client.ui.compose.nav.ConditionalBackNavDisplay
import io.music_assistant.client.ui.compose.nav.MultiBackStack
import io.music_assistant.client.ui.compose.nav.NavigationItem
import io.music_assistant.client.ui.compose.nav.ScreenState
import io.music_assistant.client.ui.compose.nav.createNavigationItem
import io.music_assistant.client.ui.compose.search.GlobalSearchRequest
import io.music_assistant.client.ui.compose.search.SearchScreen
import io.music_assistant.client.ui.compose.search.SearchScreenState
import io.music_assistant.client.ui.compose.search.SearchViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.nav_home
import musicassistantclient.composeapp.generated.resources.nav_library
import musicassistantclient.composeapp.generated.resources.nav_search
import musicassistantclient.composeapp.generated.resources.players_remote_volume_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Constructs the back stacks backing [MusicSection]'s internal Home/Library/Search switcher.
 * Owned by the caller (the outer app shell) rather than by [MusicSection] itself so that
 * deep-link handling — which needs to work no matter which top-level app tab is showing —
 * can drive it directly instead of only when this section happens to be composed.
 */
@Composable
fun rememberMusicNavState(): MultiBackStack<NavKey> {
    val backStacks = listOf(
        rememberMainNavBackStack(MainNav.Landing),
        rememberMainNavBackStack(MainNav.Library),
        rememberMainNavBackStack(MainNav.Search),
    )
    return remember { MultiBackStack(backStacks) }
}

/**
 * The "Music" app tab: Music Assistant's own Home/Library/Search experience, nested under a
 * single top-level tab (see [MainNav] for its own internal routes and [AppTab.Music] for how
 * it's mounted). [playerExpanded] / [onExpandPlayer] are hoisted to the outer app shell so
 * expanding the now-playing view can also hide the outer (5-tab) navigation bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicSection(
    musicBackStack: MultiBackStack<NavKey>,
    contentPadding: PaddingValues,
    playerExpanded: Boolean,
    onExpandPlayer: (Boolean) -> Unit,
    homeScreenViewModel: HomeScreenViewModel = koinViewModel(),
    actionsViewModel: ActionsViewModel = koinViewModel(),
    viewModeViewModel: ViewModeViewModel = koinViewModel(),
    dspSettingsViewModel: DspSettingsViewModel = koinViewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val toastState = rememberToastState()
    val errorBus: ErrorMessageBus = koinInject()
    val volumeButtonService: VolumeButtonService = koinInject()

    LaunchedEffect(Unit) {
        homeScreenViewModel.links.collectLatest { url -> uriHandler.openUri(url) }
    }

    // Surface server-side RPC errors as toasts on top of whichever screen is active.
    // (Per-screen ToastHosts handle ActionsViewModel toasts.)
    LaunchedEffect(Unit) {
        errorBus.messages.collect { msg ->
            val truncated = if (msg.length > 150) msg.take(150) + "…" else msg
            toastState.showToast(truncated, ToastDuration.LONG)
        }
    }

    val playersState by homeScreenViewModel.playersState.collectAsStateWithLifecycle()
    // Single pager state used across all views
    val data = playersState as? HomeScreenViewModel.PlayersState.Data
    val playerPagerState = rememberPagerState(
        initialPage = data?.selectedPlayerIndex ?: 0,
        pageCount = { data?.playerData?.size ?: 0 },
    )

    // Bidirectional pager <-> selection sync
    // Selection→pager runs first (data layer priority), then pager→selection watches user swipes
    LaunchedEffect(playerPagerState, playersState) {
        val currentData = playersState as? HomeScreenViewModel.PlayersState.Data
            ?: return@LaunchedEffect
        val target = currentData.selectedPlayerIndex ?: return@LaunchedEffect
        if (!playerPagerState.isScrollInProgress) {
            playerPagerState.animateScrollToPage(target)
        }

        snapshotFlow { playerPagerState.settledPage }.collect { currentPage ->
            currentData.playerData.getOrNull(currentPage)?.let { playerData ->
                homeScreenViewModel.selectPlayer(playerData.player)
            }
        }
    }

    // Each root screen's scroll/collapsing-top-bar state is owned by its NavEntry
    // (published here while composed) so its lifetime matches the ViewModel it
    // mirrors. The switcher reads these to scroll the active tab to top on re-tap;
    // a tab switch disposes the entry, so re-entry starts fresh instead of stranding
    // a collapsed top bar.
    val homeScreenState = remember { mutableStateOf<HomeScreenState?>(null) }
    val libraryScreenState = remember { mutableStateOf<LibraryScreenState?>(null) }
    val searchScreenState = remember { mutableStateOf<SearchScreenState?>(null) }

    val remoteVolumeHint = stringResource(Res.string.players_remote_volume_hint)
    val viewingRemote = data?.selectedPlayer?.isLocal == false
    val currentHint by rememberUpdatedState(remoteVolumeHint)
    val observingRemote by rememberUpdatedState(viewingRemote)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, volumeButtonService) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            volumeButtonService.buttonPresses.collect {
                if (observingRemote) {
                    toastState.showToast(currentHint, ToastDuration.SHORT)
                }
            }
        }
    }

    val navigationItems = listOf(
        musicBackStack.createNavigationItem(
            backStack = 0,
            icon = Icons.Default.Home,
            label = stringResource(Res.string.nav_home),
            screenState = homeScreenState.value,
        ),
        musicBackStack.createNavigationItem(
            backStack = 1,
            icon = Icons.Default.LibraryMusic,
            label = stringResource(Res.string.nav_library),
            screenState = libraryScreenState.value,
        ),
        musicBackStack.createNavigationItem(
            backStack = 2,
            icon = Icons.Default.Search,
            label = stringResource(Res.string.nav_search),
            screenState = searchScreenState.value,
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (!playerExpanded) {
                MusicTabSwitcher(navigationItems)
            }
            FloatingBarLayout(
                modifier = Modifier.weight(1f),
                floatingBar = {
                    FloatingBar(
                        expanded = playerExpanded,
                        onExpand = onExpandPlayer,
                        content = { expanded, floatingBarInnerPadding ->
                            PlayersPager(
                                playerPagerState = playerPagerState,
                                state = playersState,
                                homeScreenViewModel = homeScreenViewModel,
                                actionsViewModel = actionsViewModel,
                                dspSettingsViewModel = dspSettingsViewModel,
                                expanded = expanded,
                                onClose = { onExpandPlayer(false) },
                                contentPadding = floatingBarInnerPadding,
                            ) { item ->
                                musicBackStack.add(
                                    MainNav.ItemDetails(
                                        itemId = item.itemId,
                                        mediaType = item.mediaType,
                                        providerId = item.provider,
                                    ),
                                )
                            }
                        },
                    )
                },
            ) { floatingBarContentPadding ->
                BackHandler(playerExpanded) {
                    onExpandPlayer(!playerExpanded)
                }

                ConditionalBackNavDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    entries = rememberDecoratedNavEntries(
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entries = musicBackStack.toEntries(
                            mainNavEntryProvider(
                                floatingBarContentPadding,
                                musicBackStack,
                                homeScreenViewModel,
                                actionsViewModel,
                                viewModeViewModel,
                                homeScreenState,
                                libraryScreenState,
                                searchScreenState,
                            ),
                        ),
                    ),
                    onBack = {
                        musicBackStack.removeLastOrNull()
                    },
                    backEnabled = !playerExpanded,
                )
            }
        }
        ToastHost(toastState = toastState)
    }
}

/**
 * Compact switcher between Home/Library/Search — the Music tab's own internal navigation.
 *
 * Unlike the screens it switches between (which get status-bar clearance for free from
 * Material3's [androidx.compose.material3.TopAppBar] defaults), this is a bare row with no
 * such built-in handling — without [Modifier.windowInsetsPadding] here it renders flush against
 * the very top of the screen, colliding with the system status bar / notch.
 */
@Composable
private fun MusicTabSwitcher(items: List<NavigationItem>) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items.forEachIndexed { index, item ->
            SegmentedButton(
                selected = item.selected,
                onClick = item.onClick,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                    )
                },
            ) {
                item.label?.let { Text(it) }
            }
        }
    }
}

@Composable
private fun mainNavEntryProvider(
    contentPadding: PaddingValues,
    musicBackStack: MultiBackStack<NavKey>,
    homeScreenViewModel: HomeScreenViewModel,
    actionsViewModel: ActionsViewModel,
    viewModeViewModel: ViewModeViewModel,
    homeScreenState: MutableState<HomeScreenState?>,
    libraryScreenState: MutableState<LibraryScreenState?>,
    searchScreenState: MutableState<SearchScreenState?>,
): (NavKey) -> NavEntry<NavKey> {
    // Hoisted here (outlives the per-NavEntry SearchViewModel) to carry an empty-quick-search
    // escalation from the library tab to the Search tab. Set by ItemList, consumed by SearchScreen.
    var pendingSearch by remember { mutableStateOf<GlobalSearchRequest?>(null) }
    return entryProvider {
        entry<MainNav.Landing> {
            val screenState = rememberPublishedScreenState(homeScreenState) {
                HomeScreenState.create()
            }
            HomeScreen(
                homeScreenViewModel,
                contentPadding = contentPadding,
                onNavigateClick = { item ->
                    when (item) {
                        is Artist,
                        is Album,
                        is Playlist,
                        is Podcast,
                        is Audiobook,
                        is Genre,
                            -> {
                            musicBackStack.add(
                                MainNav.ItemDetails(
                                    itemId = item.itemId,
                                    mediaType = item.mediaType,
                                    providerId = item.provider,
                                ),
                            )
                        }

                        else -> Unit
                    }
                },
                providerIconFetcher = { modifier, provider ->
                    actionsViewModel.getProviderIcon(provider)
                        ?.let { ProviderIcon(modifier, it) }
                },
                actionsViewModel = actionsViewModel,
                state = screenState,
            )
        }

        entry<MainNav.Library> {
            val libraryCategoriesViewModel = koinViewModel<LibraryCategoriesViewModel>()
            val screenState = rememberPublishedScreenState(libraryScreenState) {
                LibraryScreenState.create()
            }

            LibraryScreen(
                libraryCategoriesViewModel,
                contentPadding = contentPadding,
                state = screenState,
                onCategoryClick = { category ->
                    when (category) {
                        LibraryCategory.BROWSE ->
                            musicBackStack.add(MainNav.Browse(path = null, title = null))

                        LibraryCategory.AI_RADIO -> musicBackStack.add(MainNav.AiRadio)

                        else ->
                            category.mediaType?.let { musicBackStack.add(MainNav.LibraryList(it)) }
                    }
                },
            )
        }

        entry<MainNav.AiRadio> {
            AiRadioScreen(
                viewModel = koinViewModel<AiRadioViewModel>(),
                contentPadding = contentPadding,
                onBack = { musicBackStack.removeLastOrNull() },
            )
        }

        entry<MainNav.LibraryList> {
            val libraryListViewModel = koinViewModel<LibraryListViewModel> {
                parametersOf(it.mediaType)
            }

            LibraryListScreen(
                libraryListViewModel = libraryListViewModel,
                viewModeViewModel = viewModeViewModel,
                contentPadding = contentPadding,
                actionsViewModel = actionsViewModel,
                onBack = { musicBackStack.removeLastOrNull() },
                onGlobalSearch = { query ->
                    pendingSearch = GlobalSearchRequest(query, it.mediaType)
                    musicBackStack.currentBackStack = 2
                    musicBackStack.resetCurrentBackStack()
                },
                onNavigateClick = { item ->
                    when (item) {
                        is Artist,
                        is Album,
                        is Playlist,
                        is Podcast,
                        is Audiobook,
                        is Genre,
                            -> {
                            musicBackStack.add(
                                MainNav.ItemDetails(
                                    itemId = item.itemId,
                                    mediaType = item.mediaType,
                                    providerId = item.provider,
                                ),
                            )
                        }

                        else -> Unit
                    }
                },
            )
        }

        entry<MainNav.Browse> { browse ->
            val browseViewModel = koinViewModel<BrowseViewModel> {
                parametersOf(browse.path)
            }

            BrowseScreen(
                browseViewModel = browseViewModel,
                title = browse.title,
                contentPadding = contentPadding,
                actionsViewModel = actionsViewModel,
                onBack = { musicBackStack.removeLastOrNull() },
                onNavigateClick = { item ->
                    when (item) {
                        is RecommendationFolder ->
                            if (item.isParentLink) {
                                // The server's ".." entry maps to our own back navigation.
                                musicBackStack.removeLastOrNull()
                            } else {
                                // BrowseFolder carries an explicit `path`; `uri` is only a fallback.
                                musicBackStack.add(
                                    MainNav.Browse(path = item.path ?: item.uri, title = item.displayName),
                                )
                            }

                        is Artist,
                        is Album,
                        is Playlist,
                        is Podcast,
                        is Audiobook,
                        is Genre,
                        -> musicBackStack.add(
                            MainNav.ItemDetails(
                                itemId = item.itemId,
                                mediaType = item.mediaType,
                                providerId = item.provider,
                            ),
                        )

                        else -> Unit
                    }
                },
            )
        }

        entry<MainNav.ItemList> {
            val itemListViewModel = koinViewModel<ItemListViewModel> {
                parametersOf(it.itemList)
            }

            ItemListScreen(
                title = it.title,
                mediaType = it.itemList.mediaType,
                itemListViewModel = itemListViewModel,
                viewModeViewModel = viewModeViewModel,
                actionsViewModel = actionsViewModel,
                onNavigateClick = { item ->
                    when (item) {
                        is Artist,
                        is Album,
                        is Playlist,
                        is Podcast,
                        is Audiobook,
                        is Genre,
                            -> {
                            musicBackStack.add(
                                MainNav.ItemDetails(
                                    itemId = item.itemId,
                                    mediaType = item.mediaType,
                                    providerId = item.provider,
                                ),
                            )
                        }

                        else -> Unit
                    }
                },
                onBack = { musicBackStack.removeLastOrNull() },
                contentPadding = contentPadding,
                clickContext = it.clickContext,
            )
        }

        entry<MainNav.ItemDetails> {
            val itemDetailsViewModel = koinViewModel<ItemDetailsViewModel> {
                parametersOf(it.itemId, it.mediaType, it.providerId)
            }

            ItemDetailsScreen(
                itemDetailsViewModel = itemDetailsViewModel,
                viewModeViewModel = viewModeViewModel,
                actionsViewModel = actionsViewModel,
                onBack = { musicBackStack.removeLastOrNull() },
                onNavigateToItem = { itemId, mediaType, providerId ->
                    musicBackStack.add(
                        MainNav.ItemDetails(
                            itemId = itemId,
                            mediaType = mediaType,
                            providerId = providerId,
                        ),
                    )
                },
                onNavigateToList = { title, itemList, clickContext ->
                    musicBackStack.add(MainNav.ItemList(title, itemList, clickContext))
                },
                contentPadding = contentPadding,
            )
        }

        entry<MainNav.Search> {
            val searchViewModel = koinViewModel<SearchViewModel>()
            val screenState = rememberPublishedScreenState(searchScreenState) {
                SearchScreenState.create()
            }

            SearchScreen(
                searchViewModel = searchViewModel,
                onNavigateToItem = { itemId, mediaType, providerId ->
                    musicBackStack.add(
                        MainNav.ItemDetails(
                            itemId = itemId,
                            mediaType = mediaType,
                            providerId = providerId,
                        ),
                    )
                },
                contentPadding = contentPadding,
                actionsViewModel = actionsViewModel,
                state = screenState,
                pendingSearch = pendingSearch,
                onSearchConsumed = { pendingSearch = null },
            )
        }
    }
}

/**
 * Creates a root screen's [ScreenState] scoped to the calling NavEntry and publishes it to a
 * root-level [holder] while composed, so the switcher can drive scroll-to-top on re-tap
 * without hoisting the state above the entry (which would desync it from the entry's ViewModel).
 */
@Composable
private fun <S : ScreenState> rememberPublishedScreenState(
    holder: MutableState<S?>,
    create: @Composable () -> S,
): S {
    val screenState = create()
    DisposableEffect(screenState) {
        holder.value = screenState
        onDispose { holder.value = null }
    }
    return screenState
}

/** Routes internal to [MusicSection] — Music Assistant's own Home/Library/Search navigation. */
sealed interface MainNav : NavKey {
    @Serializable
    data object Landing : MainNav

    @Serializable
    data object Library : MainNav

    @Serializable
    data class LibraryList(val mediaType: MediaType) : MainNav

    /** Stations of the optional `ai_radio` plugin. Not a media type, hence its own route. */
    @Serializable
    data object AiRadio : MainNav

    /**
     * One level of the folder-style Browse tree. [path] is the server browse path (null = root);
     * [stackingId] keeps stacked levels distinct in the back stack (mirrors [ItemDetails]).
     */
    @OptIn(ExperimentalUuidApi::class)
    @Serializable
    data class Browse(
        val path: String?,
        val title: String?,
        val stackingId: String = Uuid.generateV4().toString(),
    ) : MainNav

    /**
     * Multiple instances of the same item can appear in a back stack - [stackingId] ensures they
     * are treated as different entries.
     */
    @OptIn(ExperimentalUuidApi::class)
    @Serializable
    data class ItemDetails(
        val itemId: String,
        val mediaType: MediaType,
        val providerId: String,
        val stackingId: String = Uuid.generateV4().toString(),
    ) : MainNav

    @Serializable
    data object Search : MainNav

    @Serializable
    data class ItemList(
        val title: String,
        val itemList: io.music_assistant.client.ui.compose.item.ItemList,
        val clickContext: ClickContext,
    ) : MainNav
}

@Composable
private fun rememberMainNavBackStack(bottom: MainNav) = rememberNavBackStack(
    SavedStateConfiguration(
        from = SavedStateConfiguration.DEFAULT,
        builderAction = {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(MainNav.Landing::class, MainNav.Landing.serializer())
                    subclass(MainNav.Library::class, MainNav.Library.serializer())
                    subclass(MainNav.LibraryList::class, MainNav.LibraryList.serializer())
                    subclass(MainNav.AiRadio::class, MainNav.AiRadio.serializer())
                    subclass(MainNav.Browse::class, MainNav.Browse.serializer())
                    subclass(
                        MainNav.ItemDetails::class,
                        MainNav.ItemDetails.serializer(),
                    )
                    subclass(MainNav.Search::class, MainNav.Search.serializer())
                    subclass(MainNav.ItemList::class, MainNav.ItemList.serializer())
                }
            }
        },
    ),
    bottom,
)
