package io.music_assistant.client.di

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.music_assistant.client.data.repository.StargazingRepository
import io.music_assistant.client.utils.createPlatformHttpClient
import io.music_assistant.client.utils.myJson
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Public, no-auth data sources for the Stargazing card (NOAA space weather, ISS pass
 * predictions) — kept separate from [homeAssistantModule] since these are unrelated,
 * unauthenticated third-party services, not the property's own local server.
 */
val stargazingModule = module {
    single(named("stargazingHttpClient")) {
        createPlatformHttpClient {
            expectSuccess = true
            install(ContentNegotiation) { json(myJson) }
            install(HttpTimeout) {
                requestTimeoutMillis = 8_000
                connectTimeoutMillis = 5_000
            }
        }
    }
    single { StargazingRepository(get(named("stargazingHttpClient"))) }
}
