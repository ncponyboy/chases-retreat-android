package io.music_assistant.client.ui.compose.photoshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Shared Google Photos album guests can view and add to. */
private const val PHOTO_ALBUM_URL = "https://photos.app.goo.gl/yLr3sMACBGnEQ6eQ9"

/**
 * A dedicated tab rather than folding this into the Welcome screen's link list — guests landed
 * cold in Google Photos on the first version of this and it wasn't obvious what to do there, so
 * this explains what we're asking for and how to actually add a photo before sending them out.
 */
@Composable
fun PhotoShareScreen(contentPadding: PaddingValues = PaddingValues()) {
    val uriHandler = LocalUriHandler.current
    // contentPadding (from the outer tab bar) never carries a top value — it only reserves
    // room for the bottom bar / rail — so the status bar inset has to be added separately,
    // or this screen's content renders underneath the clock/battery icons.
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = statusBarPadding + contentPadding.calculateTopPadding() + 32.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Share Your Memories",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Share your memories of your stay at Chase's Retreat! A sunset from the " +
                "deck, a hike, a night in the hot tub — anything that made your trip.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "To upload: tap the button below, then look for the + (add photo) button " +
                "inside Google Photos and pick your favorite shot.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = { uriHandler.openUri(PHOTO_ALBUM_URL) }) {
            Text("Open Photo Album")
        }
    }
}
