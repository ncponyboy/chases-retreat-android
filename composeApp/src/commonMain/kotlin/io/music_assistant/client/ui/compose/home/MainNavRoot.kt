package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import io.music_assistant.client.api.DeepLinkBus
import io.music_assistant.client.api.DeepLinkDestination
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.ui.compose.common.ConnectionStatusBanner
import io.music_assistant.client.ui.compose.common.viewmodel.ActionsViewModel
import io.music_assistant.client.ui.compose.home.players.DspSettingsViewModel
import io.music_assistant.client.ui.compose.family.FamilyCalendarScreen
import io.music_assistant.client.ui.compose.housemanual.HouseManualScreen
import io.music_assistant.client.ui.compose.item.ViewModeViewModel
import io.music_assistant.client.ui.compose.localbusinesses.LocalBusinessesScreen
import io.music_assistant.client.ui.compose.locked.LockedStatusScreen
import io.music_assistant.client.ui.compose.nav.AdaptiveNavigationBarLayout
import io.music_assistant.client.ui.compose.nav.ConditionalBackNavDisplay
import io.music_assistant.client.ui.compose.nav.MultiBackStack
import io.music_assistant.client.ui.compose.nav.createNavigationItem
import io.music_assistant.client.ui.compose.photoshare.PhotoShareScreen
import io.music_assistant.client.ui.compose.thingstodo.ThingsToDoScreen
import io.music_assistant.client.ui.compose.welcome.WelcomeScreen
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.nav_house_manual
import musicassistantclient.composeapp.generated.resources.nav_local_businesses
import musicassistantclient.composeapp.generated.resources.nav_music
import musicassistantclient.composeapp.generated.resources.nav_photo_share
import musicassistantclient.composeapp.generated.resources.nav_things_to_do
import musicassistantclient.composeapp.generated.resources.nav_welcome
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The app's top-level shell: a 6-tab bar (Welcome / Music / Local Businesses / Things to Do /
 * House Manual / Photo Share). Welcome is the default landing tab — see [AppTab] — with Music
 * Assistant's own Home/Library/Search navigation nested one level down inside the Music tab
 * (see [MusicSection]).
 *
 * Only the Music tab depends on Music Assistant being reachable — the other five are static,
 * local content and stay usable regardless of the server's connection state (see the Music
 * entry below, gated by [MusicTabGate]).
 *
 * Deep-link handling lives here rather than inside [MusicSection] so a link still works no
 * matter which top-level tab happens to be showing: per [MultiBackStack.toEntries], only the
 * root tab (index 0) and the current tab stay composed, so a `LaunchedEffect` inside MusicSection
 * would go dormant whenever a different tab (e.g. House Manual) was active.
 */
@Composable
fun MainNavigationRoot(
    homeScreenViewModel: HomeScreenViewModel = koinViewModel(),
    actionsViewModel: ActionsViewModel = koinViewModel(),
    viewModeViewModel: ViewModeViewModel = koinViewModel(),
    dspSettingsViewModel: DspSettingsViewModel = koinViewModel(),
) {
    val deepLinkBus: DeepLinkBus = koinInject()
    val musicBackStack = rememberMusicNavState()

    // Hoisted above the Music tab so expanding the now-playing view can also hide this
    // shell's own (outer) navigation bar, not just Music's internal switcher.
    var playerExpanded by remember { mutableStateOf(false) }

    val backStacks = listOf(
        rememberAppTabBackStack(AppTab.Welcome),
        rememberAppTabBackStack(AppTab.Music),
        rememberAppTabBackStack(AppTab.LocalBusinesses),
        rememberAppTabBackStack(AppTab.ThingsToDo),
        rememberAppTabBackStack(AppTab.HouseManual),
        rememberAppTabBackStack(AppTab.PhotoShare),
        // Not in the nav bar: reached from the Welcome tab's "Family" card (see FAMILY_TAB_INDEX).
        rememberAppTabBackStack(AppTab.FamilyCalendar),
    )
    val multiBackStack = remember { MultiBackStack(backStacks) }

    // Apply a pending navigation deep link (musicassistant://app/<page> or the https App/
    // Universal Link): switches to the Music tab and drives its internal back stack.
    val connectionState by homeScreenViewModel.connectionState.collectAsStateWithLifecycle()
    val pendingDeepLink by deepLinkBus.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingDeepLink, connectionState) {
        val dest = pendingDeepLink ?: return@LaunchedEffect
        val authenticated = (connectionState as? SessionState.Connected)
            ?.dataConnectionState is DataConnectionState.Authenticated
        if (!authenticated) return@LaunchedEffect
        when (dest) {
            DeepLinkDestination.Home -> {
                multiBackStack.currentBackStack = MUSIC_TAB_INDEX
                musicBackStack.currentBackStack = 0
                musicBackStack.resetCurrentBackStack()
            }

            is DeepLinkDestination.Library -> {
                multiBackStack.currentBackStack = MUSIC_TAB_INDEX
                musicBackStack.currentBackStack = 1
                musicBackStack.resetCurrentBackStack()
                // /library/<category> → push the category list onto the Library tab.
                dest.mediaType?.let { musicBackStack.add(MainNav.LibraryList(it)) }
            }

            DeepLinkDestination.Search -> {
                multiBackStack.currentBackStack = MUSIC_TAB_INDEX
                musicBackStack.currentBackStack = 2
                musicBackStack.resetCurrentBackStack()
            }

            DeepLinkDestination.Players -> {
                // Now-playing lives inside the Music tab, so bring it forward too.
                multiBackStack.currentBackStack = MUSIC_TAB_INDEX
                playerExpanded = true
            }
        }
        deepLinkBus.consume(dest)
    }

    val navigationItems = listOf(
        multiBackStack.createNavigationItem(
            backStack = 0,
            icon = Icons.Default.Home,
            label = stringResource(Res.string.nav_welcome),
        ),
        multiBackStack.createNavigationItem(
            backStack = MUSIC_TAB_INDEX,
            icon = Icons.Default.MusicNote,
            label = stringResource(Res.string.nav_music),
        ),
        multiBackStack.createNavigationItem(
            backStack = 2,
            icon = Icons.Default.Call,
            label = stringResource(Res.string.nav_local_businesses),
        ),
        multiBackStack.createNavigationItem(
            backStack = 3,
            icon = Icons.Default.Hiking,
            label = stringResource(Res.string.nav_things_to_do),
        ),
        multiBackStack.createNavigationItem(
            backStack = 4,
            icon = Icons.Default.MenuBook,
            label = stringResource(Res.string.nav_house_manual),
        ),
        multiBackStack.createNavigationItem(
            backStack = 5,
            icon = Icons.Default.PhotoCamera,
            label = stringResource(Res.string.nav_photo_share),
        ),
        // Locked build: no separate Settings entry — there's nothing for a guest to
        // configure. See LockedStatusScreen for the only connection-status UI this build has,
        // now scoped to the Music tab (MusicTabGate) rather than a full-screen destination.
    )

    AdaptiveNavigationBarLayout(
        showNavigation = !playerExpanded,
        navigationItems = navigationItems,
    ) { contentPadding ->
        ConditionalBackNavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            entries = rememberDecoratedNavEntries(
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entries = multiBackStack.toEntries(
                    appTabEntryProvider(
                        contentPadding = contentPadding,
                        multiBackStack = multiBackStack,
                        musicBackStack = musicBackStack,
                        homeScreenViewModel = homeScreenViewModel,
                        actionsViewModel = actionsViewModel,
                        viewModeViewModel = viewModeViewModel,
                        dspSettingsViewModel = dspSettingsViewModel,
                        playerExpanded = playerExpanded,
                        onExpandPlayer = { playerExpanded = it },
                    ),
                ),
            ),
            onBack = {
                multiBackStack.removeLastOrNull()
            },
            backEnabled = !playerExpanded,
        )
    }
}

/** Index of [AppTab.Music] within [MainNavigationRoot]'s back stack list — referenced from
 * multiple deep-link branches, so named rather than repeating the literal. */
private const val MUSIC_TAB_INDEX = 1

/** Index of [AppTab.FamilyCalendar] — a tab with no nav-bar item, opened from the Welcome tab. */
private const val FAMILY_TAB_INDEX = 6

@Composable
private fun appTabEntryProvider(
    contentPadding: PaddingValues,
    multiBackStack: MultiBackStack<NavKey>,
    musicBackStack: MultiBackStack<NavKey>,
    homeScreenViewModel: HomeScreenViewModel,
    actionsViewModel: ActionsViewModel,
    viewModeViewModel: ViewModeViewModel,
    dspSettingsViewModel: DspSettingsViewModel,
    playerExpanded: Boolean,
    onExpandPlayer: (Boolean) -> Unit,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    entry<AppTab.Welcome> {
        WelcomeScreen(
            contentPadding = contentPadding,
            onOpenMusic = { multiBackStack.currentBackStack = MUSIC_TAB_INDEX },
            onOpenLocalBusinesses = { multiBackStack.currentBackStack = 2 },
            onOpenThingsToDo = { multiBackStack.currentBackStack = 3 },
            onOpenHouseManual = { multiBackStack.currentBackStack = 4 },
            onOpenPhotoShare = { multiBackStack.currentBackStack = 5 },
            onOpenFamilyCalendar = { multiBackStack.currentBackStack = FAMILY_TAB_INDEX },
        )
    }

    entry<AppTab.Music> {
        MusicTabGate(
            musicBackStack = musicBackStack,
            contentPadding = contentPadding,
            playerExpanded = playerExpanded,
            onExpandPlayer = onExpandPlayer,
            homeScreenViewModel = homeScreenViewModel,
            actionsViewModel = actionsViewModel,
            viewModeViewModel = viewModeViewModel,
            dspSettingsViewModel = dspSettingsViewModel,
        )
    }

    entry<AppTab.LocalBusinesses> {
        LocalBusinessesScreen(contentPadding = contentPadding)
    }

    entry<AppTab.ThingsToDo> {
        ThingsToDoScreen(contentPadding = contentPadding)
    }

    entry<AppTab.HouseManual> {
        HouseManualScreen(contentPadding = contentPadding)
    }

    entry<AppTab.PhotoShare> {
        PhotoShareScreen(contentPadding = contentPadding)
    }

    entry<AppTab.FamilyCalendar> {
        FamilyCalendarScreen(contentPadding = contentPadding)
    }
}

/**
 * Gates [MusicSection] behind Music Assistant actually being reachable, showing
 * [LockedStatusScreen] in its place otherwise — scoped to just this tab, so a guest who isn't
 * on the property Wi-Fi (or whose connection drops) can still use every other tab.
 *
 * Once a session has authenticated at least once, brief blips ([SessionState.Reconnecting] /
 * [SessionState.Disconnected.Backgrounded]) keep showing the last-known Music content — with
 * [ConnectionStatusBanner] as a small non-blocking indicator — rather than yanking the guest
 * back to the status screen for a transient reconnect. A session that has never authenticated
 * (cold launch, or a hard failure) always shows the status screen instead.
 */
@Composable
private fun MusicTabGate(
    musicBackStack: MultiBackStack<NavKey>,
    contentPadding: PaddingValues,
    playerExpanded: Boolean,
    onExpandPlayer: (Boolean) -> Unit,
    homeScreenViewModel: HomeScreenViewModel,
    actionsViewModel: ActionsViewModel,
    viewModeViewModel: ViewModeViewModel,
    dspSettingsViewModel: DspSettingsViewModel,
) {
    val serviceClient: ServiceClient = koinInject()
    val sessionState by serviceClient.sessionState.collectAsStateWithLifecycle()

    var everAuthenticated by remember { mutableStateOf(false) }
    LaunchedEffect(sessionState) {
        if ((sessionState as? SessionState.Connected)?.dataConnectionState is DataConnectionState.Authenticated) {
            everAuthenticated = true
        }
    }

    val showMusicSection = when {
        (sessionState as? SessionState.Connected)?.dataConnectionState is DataConnectionState.Authenticated -> true
        everAuthenticated && sessionState is SessionState.Reconnecting -> true
        everAuthenticated && sessionState is SessionState.Disconnected.Backgrounded -> true
        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showMusicSection) {
            MusicSection(
                musicBackStack = musicBackStack,
                contentPadding = contentPadding,
                playerExpanded = playerExpanded,
                onExpandPlayer = onExpandPlayer,
                homeScreenViewModel = homeScreenViewModel,
                actionsViewModel = actionsViewModel,
                viewModeViewModel = viewModeViewModel,
                dspSettingsViewModel = dspSettingsViewModel,
            )
        } else {
            LockedStatusScreen(showExitButton = false)
        }
        ConnectionStatusBanner(modifier = Modifier.align(Alignment.TopCenter))
    }
}

/** The app's 6 top-level tabs. Each is a single leaf screen except [Music], which nests its
 * own internal navigation (see [MainNav] / [MusicSection]). */
private sealed interface AppTab : NavKey {
    @Serializable
    data object Welcome : AppTab

    @Serializable
    data object Music : AppTab

    @Serializable
    data object LocalBusinesses : AppTab

    @Serializable
    data object ThingsToDo : AppTab

    @Serializable
    data object HouseManual : AppTab

    @Serializable
    data object PhotoShare : AppTab

    @Serializable
    data object FamilyCalendar : AppTab
}

@Composable
private fun rememberAppTabBackStack(bottom: AppTab) = rememberNavBackStack(
    SavedStateConfiguration(
        from = SavedStateConfiguration.DEFAULT,
        builderAction = {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(AppTab.Welcome::class, AppTab.Welcome.serializer())
                    subclass(AppTab.Music::class, AppTab.Music.serializer())
                    subclass(AppTab.LocalBusinesses::class, AppTab.LocalBusinesses.serializer())
                    subclass(AppTab.ThingsToDo::class, AppTab.ThingsToDo.serializer())
                    subclass(AppTab.HouseManual::class, AppTab.HouseManual.serializer())
                    subclass(AppTab.PhotoShare::class, AppTab.PhotoShare.serializer())
                    subclass(AppTab.FamilyCalendar::class, AppTab.FamilyCalendar.serializer())
                }
            }
        },
    ),
    bottom,
)
