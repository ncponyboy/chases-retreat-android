package io.music_assistant.client.ui.compose.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.music_assistant.client.branding.BrandConfig
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.mass
import org.jetbrains.compose.resources.painterResource

private data class WelcomeLink(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * The app's default landing tab — a greeting plus links out to the other sections. Kept
 * deliberately simple (no ViewModel/network) since it's just a directory, not content of its own.
 */
@Composable
fun WelcomeScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onOpenMusic: () -> Unit = {},
    onOpenLocalBusinesses: () -> Unit = {},
    onOpenThingsToDo: () -> Unit = {},
    onOpenHouseManual: () -> Unit = {},
    onOpenPhotoShare: () -> Unit = {},
    onOpenFamilyCalendar: () -> Unit = {},
) {
    val links = listOf(
        WelcomeLink(
            label = "Music",
            description = "Browse and play music throughout the house.",
            icon = Icons.Filled.MusicNote,
            onClick = onOpenMusic,
        ),
        WelcomeLink(
            label = "House Manual",
            description = "The hot tub, hammocks, tubes, and everything else you need to know.",
            icon = Icons.Filled.MenuBook,
            onClick = onOpenHouseManual,
        ),
        WelcomeLink(
            label = "Things to Do",
            description = "Rivers, trails, and towns worth the drive.",
            icon = Icons.Filled.Hiking,
            onClick = onOpenThingsToDo,
        ),
        WelcomeLink(
            label = "Local Businesses",
            description = "Restaurants and outfitters nearby — tap to call.",
            icon = Icons.Filled.Call,
            onClick = onOpenLocalBusinesses,
        ),
        WelcomeLink(
            label = "Share Your Photos",
            description = "Add a favorite shot from your stay to our guest photo album.",
            icon = Icons.Filled.PhotoCamera,
            onClick = onOpenPhotoShare,
        ),
        WelcomeLink(
            label = "Family Calendar",
            description = "Family only — sign in with your family account.",
            icon = Icons.Filled.CalendarMonth,
            onClick = onOpenFamilyCalendar,
        ),
    )

    // contentPadding (from the outer tab bar) never carries a top value — it only reserves
    // room for the bottom bar / rail — so the status bar inset has to be added separately,
    // or the husky logo/greeting renders underneath the clock/battery icons.
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarPadding + contentPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.mass),
                    contentDescription = null,
                    modifier = Modifier.height(96.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Welcome to ${BrandConfig.APP_NAME}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Everything you need for your stay is right here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        items(links) { link -> WelcomeLinkCard(link) }
    }
}

@Composable
private fun WelcomeLinkCard(link: WelcomeLink) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = link.onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = link.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text = link.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = link.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
