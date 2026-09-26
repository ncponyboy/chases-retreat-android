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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.music_assistant.client.utils.isAndroidPlatform

/** One line within a [HouseManualSection]. [emphasized] calls out something safety-critical. */
data class HouseManualItem(
    val text: String,
    val emphasized: Boolean = false,
)

data class HouseManualSection(
    val title: String,
    val items: List<HouseManualItem>,
)

/**
 * Static, hand-curated for the property. Update this list directly to add, remove, or re-order
 * sections. The one live element on this screen — [HotTubStatusCard] — is injected separately,
 * right after the "Hot Tub" section below, rather than living in this data.
 */
private val houseManualSections = listOf(
    HouseManualSection(
        title = "Hot Tub",
        items = listOf(
            HouseManualItem(
                "Chemicals are under the hot tub stairs. Make sure there are bromide tablets " +
                    "in the floater.",
            ),
        ),
    ),
    HouseManualSection(
        title = "Hammocks",
        items = listOf(
            HouseManualItem("In the truck on the main level."),
        ),
    ),
    HouseManualSection(
        title = "River Tubes",
        items = listOf(
            HouseManualItem("In the HVAC room, to the right of the washer and dryer in the basement."),
        ),
    ),
    HouseManualSection(
        title = "Pool Room Music",
        items = listOf(
            HouseManualItem("Use the tablet, or the Music tab in this app."),
            HouseManualItem("The basement tablet can control everything."),
            HouseManualItem("Connect to the Denon directly if you want to use your own music service."),
        ),
    ),
    HouseManualSection(
        title = "Cleaning",
        items = listOf(
            HouseManualItem("There's a vacuum on every floor, and cleaning supplies located throughout the house."),
        ),
    ),
    HouseManualSection(
        title = "Before You Leave",
        items = listOf(
            HouseManualItem("Throw out any perishable foods."),
            HouseManualItem("Wash dirty sheets."),
            HouseManualItem("Put the house back the way you found it."),
            HouseManualItem("Close the blinds."),
            HouseManualItem(
                "VERY IMPORTANT: physically check that every exterior door on every level " +
                    "is dead-bolted shut.",
                emphasized = true,
            ),
        ),
    ),
)

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
fun HouseManualScreen(contentPadding: PaddingValues = PaddingValues()) {
    val uriHandler = LocalUriHandler.current
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
        item { HouseManualSectionCard(houseManualSections.first()) }
        item { HotTubStatusCard() }
        items(houseManualSections.drop(1)) { section ->
            HouseManualSectionCard(section)
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
