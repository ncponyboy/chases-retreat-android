package io.music_assistant.client.di

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.music_assistant.client.data.repository.FamilyCalendarRepository
import io.music_assistant.client.data.repository.HotTubRepository
import io.music_assistant.client.ui.compose.family.FamilyCalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import io.music_assistant.client.utils.createPlatformHttpClient
import io.music_assistant.client.utils.myJson
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Narrowly-scoped Home Assistant integration used only for the hot tub status card — see
 * [io.music_assistant.client.branding.HomeAssistantConfig] for the security rationale. This
 * client is never shared with any other feature.
 */
val homeAssistantModule = module {
    single(named("homeAssistantHttpClient")) {
        createPlatformHttpClient {
            expectSuccess = true
            install(ContentNegotiation) { json(myJson) }
            install(HttpTimeout) {
                requestTimeoutMillis = 8_000
                connectTimeoutMillis = 5_000
            }
        }
    }
    single { HotTubRepository(get(named("homeAssistantHttpClient"))) }

    // Family calendar: the family member's own Home Assistant, at an address they type in. Its
    // own client (no expectSuccess) because the sign-in flow reads error bodies on 4xx.
    single(named("familyCalendarHttpClient")) {
        createPlatformHttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 8_000
            }
        }
    }
    single { FamilyCalendarRepository(get(named("familyCalendarHttpClient")), get(named(SECRETS))) }
    viewModelOf(::FamilyCalendarViewModel)
}
