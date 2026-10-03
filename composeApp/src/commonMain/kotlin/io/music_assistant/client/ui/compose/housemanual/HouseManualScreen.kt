package io.music_assistant.client.ui.compose.housemanual

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.data.repository.HouseManualItem
import io.music_assistant.client.data.repository.HouseManualSection
import org.koin.compose.viewmodel.koinViewModel
import io.music_assistant.client.utils.isAndroidPlatform

private data class OtherApp(val name: String, val iosUrl: String, val androidUrl: String)

/** A deliberately low-key mention, not a promo card — see the House Manual's own "not
 *  cluttered" design goal. Shown once, at the very bottom, below all real guest content. */
private val otherApps = listOf(
    OtherApp(
        name = "High Country Events",
        iosUrl = "https://apps.apple.com/us/app/high-country-events/id6756491713",
        androidUrl = "https://play.google.com/store/apps/details?id=com.highcountry.events",
    ),
    OtherApp(
        name = "High Country Outdoors",
        iosUrl = "https://apps.apple.com/us/app/high-country-outdoors/id6760595939",
        androidUrl = "https://play.google.com/store/apps/details?id=com.jaime.high_country_outdoors",
    ),
)

@Composable
fun HouseManualScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HouseManualViewModel = koinViewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    // Re-check for edits every time the guest opens this tab.
    LaunchedEffect(Unit) { viewModel.refresh() }
    // contentPadding (from the outer tab bar) never carries a top value — it only reserves
    // room for the bottom bar / rail — so the status bar inset has to be added separately,
    // or this screen's title renders underneath the clock/battery icons.
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarPadding + contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        sections.forEach { section ->
            item { HouseManualSectionCard(section) }
            if (section.hotTubStatus) item { HotTubStatusCard() }
        }
        item { OtherAppsFooter(onOpenApp = { app -> uriHandler.openUri(if (isAndroidPlatform) app.androidUrl else app.iosUrl) }) }
    }
}

@Composable
private fun OtherAppsFooter(onOpenApp: (OtherApp) -> Unit) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "More Apps from Chase's Software",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        otherApps.forEach { app ->
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.clickable { onOpenApp(app) },
            )
        }
    }
}

@Composable
private fun HouseManualSectionCard(section: HouseManualSection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val showBullets = section.items.size > 1
            section.items.forEach { line ->
                HouseManualLine(line, showBullet = showBullets)
            }
        }
    }
}

@Composable
private fun HouseManualLine(line: HouseManualItem, showBullet: Boolean) {
    val color = if (line.emphasized) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (line.emphasized) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(top = 2.dp),
            )
        } else if (showBullet) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyLarge,
                color = color,
            )
        }
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = if (line.emphasized) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.align(Alignment.Top),
        )
    }
}
