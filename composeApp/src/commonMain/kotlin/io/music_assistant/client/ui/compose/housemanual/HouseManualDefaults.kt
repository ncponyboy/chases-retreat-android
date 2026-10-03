package io.music_assistant.client.ui.compose.housemanual

import io.music_assistant.client.data.repository.HouseManualItem
import io.music_assistant.client.data.repository.HouseManualSection

/**
 * Shown until the live `house-manual.json` has been fetched at least once (e.g. a first launch
 * with no signal). The live file is the source of truth — keep this roughly in sync with it, but
 * it does not need to be updated for every change.
 */
internal val defaultHouseManualSections = listOf(
    HouseManualSection(
        title = "Hot Tub",
        items = listOf(
            HouseManualItem(
                "Chemicals are under the hot tub stairs. Make sure there are bromide tablets " +
                    "in the floater.",
            ),
        ),
        hotTubStatus = true,
    ),
    HouseManualSection(
        title = "Hammocks",
        items = listOf(HouseManualItem("In the truck on the main level.")),
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
