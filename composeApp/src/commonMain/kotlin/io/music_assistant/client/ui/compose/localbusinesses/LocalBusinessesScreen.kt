package io.music_assistant.client.ui.compose.localbusinesses

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/** One entry on the Local Businesses tab: a name and a number a guest can tap to call. */
data class LocalBusiness(
    val name: String,
    val phoneNumber: String,
)

/**
 * Static, hand-curated for the property — not server data, so no ViewModel/network layer.
 * Update this list directly to add, remove, or re-order businesses.
 */
private val localBusinesses = listOf(
    LocalBusiness("Red Dog Bar & Bistro", "(336) 359-8000"),
    LocalBusiness("Piney Creek General Store", "(336) 359-2393"),
    LocalBusiness("Dusty Trails Outfitters", "(336) 977-8375"),
    LocalBusiness("Riverside Canoe & Tube", "(336) 982-9439"),
)

@Composable
fun LocalBusinessesScreen(contentPadding: PaddingValues = PaddingValues()) {
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(localBusinesses) { business ->
            LocalBusinessRow(
                business = business,
                onClick = { uriHandler.openUri("tel:${business.phoneNumber.filter(Char::isDigit)}") },
            )
        }
    }
}

@Composable
private fun LocalBusinessRow(business: LocalBusiness, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text = business.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = business.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
