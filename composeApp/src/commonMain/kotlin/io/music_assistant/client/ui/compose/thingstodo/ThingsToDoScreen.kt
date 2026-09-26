package io.music_assistant.client.ui.compose.thingstodo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/** One place/activity. [description] and [url] are both optional — a bare address needs neither. */
data class ThingsToDoItem(
    val name: String,
    val description: String? = null,
    val url: String? = null,
)

data class ThingsToDoSection(
    val title: String,
    val subtitle: String? = null,
    val items: List<ThingsToDoItem>,
)

/**
 * Static, hand-curated for the property (sourced from the old chasesretreat.weebly.com area-info
 * page) — not server data, so no ViewModel/network layer. Update this list directly.
 */
private val thingsToDoSections = listOf(
    ThingsToDoSection(
        title = "Piney Creek",
        subtitle = "Right in the neighborhood",
        items = listOf(
            ThingsToDoItem(
                name = "New River Access",
                description = "The South Fork of the New River runs right through the neighborhood — " +
                    "slow and shallow in summer, popular for tubing and kayaking. One access point is " +
                    "in the neighborhood itself; another is King's Creek State Park.",
                url = "https://www.ncparks.gov/park-features/new-river-state-park-kings-creek-access",
            ),
            ThingsToDoItem(
                name = "Riverside Canoe",
                description = "Rent kayaks, tubes, and canoes — they'll shuttle you upriver so you can " +
                    "float back down. Trips start at 2 hours. A summer favorite.",
                url = "http://www.riversidecanoeing.com",
            ),
            ThingsToDoItem(
                name = "Red Dog Bistro",
                description = "Gourmet food and cocktails in a small, don't-miss spot. " +
                    "Tell Kelly & Hanna hello.",
                url = "http://www.reddognc.com",
            ),
            ThingsToDoItem(
                name = "Pine Valley Grocery / Citgo",
                description = "Closest store — 7519 NC Hwy. 113 N., Piney Creek, NC 28663",
            ),
            ThingsToDoItem(
                name = "Thistle Meadow Winery",
                description = "About 19 minutes away.",
                url = "http://www.thistlemeadowwinery.com/",
            ),
        ),
    ),
    ThingsToDoSection(
        title = "West Jefferson",
        subtitle = "~23 minutes — restaurants, breweries, shops, and a movie theater " +
            "(also the closest Walmart, Ingles, and Lowe's)",
        items = listOf(
            ThingsToDoItem(
                name = "Ashe County Cheese",
                description = "Fresh cheese made in the mountains — the only cheese factory in the " +
                    "southeastern US.",
                url = "https://www.ashecountycheese.com/",
            ),
            ThingsToDoItem(
                name = "Boondocks Brewing",
                description = "Good beer, large menu.",
                url = "http://www.boondocksbeer.com/",
            ),
            ThingsToDoItem(
                name = "Old Barn Winery",
                description = "A favorite of ours — wine and live music with a mountain view.",
                url = "https://oldbarnwinery.com/",
            ),
            ThingsToDoItem(
                name = "Saloon Studio Live",
                description = "A live music venue with an Old West Town atmosphere.",
                url = "https://saloonstudioslive.com/",
            ),
            ThingsToDoItem(
                name = "Kool Nites and Hot Rods Cruise",
                description = "Local classic car cruise-in event.",
                url = "https://ashechamber.com/event.php?id=2531",
            ),
            ThingsToDoItem(
                name = "Live Downtown Cam",
                description = "See what's happening downtown right now.",
                url = "https://www.ashecountyrealestate.com/west-jefferson-webcam",
            ),
            ThingsToDoItem(
                name = "Appalachian Delivery & Courier",
                description = "Like Uber Eats, but for the mountains.",
                url = "https://appalachiandeliverycourier.square.site/",
            ),
            ThingsToDoItem(
                name = "Visit West Jefferson",
                description = "Town guide — shops, restaurants, and more.",
                url = "http://visitwestjefferson.org/",
            ),
        ),
    ),
    ThingsToDoSection(
        title = "Sparta",
        subtitle = "~20 minutes — last stop for gas and groceries before the house",
        items = listOf(
            ThingsToDoItem(
                name = "Laconia Ale Works",
                description = "Makers of Red Dog beer. 433 N. Main Street, Sparta, NC.",
                url = "https://www.facebook.com/LaconiaAleWorks/",
            ),
            ThingsToDoItem(
                name = "Main Street Pizzeria and Tap House",
                description = "Good pizza, and Guinness on tap.",
                url = "https://www.spartapizzeria.com/",
            ),
            ThingsToDoItem(
                name = "Food Lion",
                description = "381 S Main St Ste 508, Sparta, NC 28675",
            ),
        ),
    ),
    ThingsToDoSection(
        title = "Other Places",
        items = listOf(
            ThingsToDoItem(
                name = "Lansing, NC",
                description = "A quaint mountain town about 30 minutes away.",
            ),
            ThingsToDoItem(
                name = "Lansing Trading Co.",
                url = "http://www.amyanneapparel.com/",
            ),
            ThingsToDoItem(
                name = "Molly Chomper Hard Cider",
                url = "http://molleychomper.com",
            ),
            ThingsToDoItem(
                name = "Grayson Highlands State Park",
                description = "Great views and hiking, about 30 minutes away.",
                url = "https://dwr.virginia.gov/vbwt/sites/grayson-highlands-state-park/",
            ),
            ThingsToDoItem(
                name = "Mount Jefferson",
                description = "More great views and hiking.",
                url = "https://www.ncparks.gov/state-parks/mount-jefferson-state-natural-area",
            ),
            ThingsToDoItem(
                name = "Blowing Rock",
                description = "A favorite day trip — shops, restaurants, and a pub.",
                url = "https://blowingrock.com/",
            ),
            ThingsToDoItem(
                name = "Abingdon, VA",
                description = "About an hour away, but worth it.",
                url = "https://visitabingdonvirginia.com/",
            ),
        ),
    ),
)

@Composable
fun ThingsToDoScreen(contentPadding: PaddingValues = PaddingValues()) {
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
        item { StargazingCard() }
        thingsToDoSections.forEach { section ->
            item {
                Column {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    section.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            items(section.items) { thingToDoItem ->
                ThingsToDoRow(
                    item = thingToDoItem,
                    onClick = thingToDoItem.url?.let { url -> { uriHandler.openUri(url) } },
                )
            }
        }
    }
}

@Composable
private fun ThingsToDoRow(item: ThingsToDoItem, onClick: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(16.dp),
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        item.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
