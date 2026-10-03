package io.music_assistant.client.di

import io.ktor.client.plugins.HttpTimeout
import io.music_assistant.client.data.repository.HouseManualRepository
import io.music_assistant.client.ui.compose.housemanual.HouseManualViewModel
import io.music_assistant.client.utils.createPlatformHttpClient
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Public, no-auth fetch of the House Manual text (see [HouseManualRepository]). */
val houseManualModule = module {
    single(named("houseManualHttpClient")) {
        createPlatformHttpClient {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = 8_000
                connectTimeoutMillis = 5_000
            }
        }
    }
    single { HouseManualRepository(get(named("houseManualHttpClient")), get()) }
    viewModelOf(::HouseManualViewModel)
}
