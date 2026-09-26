package io.music_assistant.client.ui.compose.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.chase_software_photo
import org.jetbrains.compose.resources.painterResource

/**
 * Developer/studio credit splash — the same "Chase's Software" branding shown before the
 * app-specific splash on Chase's other apps (Triangle Happenings, High Country Outdoors, etc.).
 * Purely cosmetic, not tied to connection state; the retreat-specific splash/status screen
 * ([AutoLoginSplash] / [io.music_assistant.client.ui.compose.locked.LockedStatusScreen]) follows it.
 */
@Composable
fun ChasesSoftwareSplash(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.88f,
        animationSpec = tween(durationMillis = 700),
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2B3D6B),
                        Color(0xFF1A2640),
                    ),
                ),
            )
            .scale(scale)
            .alpha(alpha),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(206.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B4E7A))
                    .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.chase_software_photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(200.dp).clip(CircleShape),
                )
            }

            Spacer(Modifier.height(36.dp))

            Text(
                text = "Chase's Software",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "🐾  Professional Development  🐾",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
