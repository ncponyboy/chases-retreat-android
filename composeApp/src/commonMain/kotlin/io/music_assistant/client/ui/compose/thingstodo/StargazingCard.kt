package io.music_assistant.client.ui.compose.thingstodo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Tonight's Sky" — sits above the static Things to Do sections. Deliberately shows only what's
 * actually happening in the next few days (see [io.music_assistant.client.stargazing]), not an
 * almanac of every possible event — the property's mountain microclimate means a generic cloud
 * forecast would be misleading anyway, so this leans on "go look yourself" rather than a
 * go/no-go clarity score.
 */
@Composable
fun StargazingCard(viewModel: StargazingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status = (state as? StargazingUiState.Content)?.status ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Tonight's Sky",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = "${status.moonPhaseName} (${status.moonIlluminationPercent}% lit) — " +
                status.moonSkyNote,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (status.events.isEmpty()) {
            Text(
                text = "Nothing special the next few nights — but it's dark out here, worth a " +
                    "look anyway.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            status.events.forEach { event ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = event.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}
