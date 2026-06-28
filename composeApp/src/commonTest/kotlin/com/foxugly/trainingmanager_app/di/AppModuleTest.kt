package com.foxugly.trainingmanager_app.di

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppModuleTest {
    @Test
    fun graphResolvesApiRepositoryAndSharesTokenStore() {
        val fake = FakeTokenStore(access = "a")
        val app = koinApplication {
            modules(
                appModule(
                    tokenStore = fake,
                    apiBaseUrl = "https://test/api/v1/",
                    enableHttpLogging = false,
                ),
            )
        }
        val koin = app.koin

        val api = koin.get<TrainingManagerApi>()
        val repo = koin.get<AuthRepository>()
        assertTrue(api === koin.get<TrainingManagerApi>(), "API must be a singleton")
        assertSame(fake, koin.get<TokenStore>(), "the injected TokenStore must be shared")
        api.close()
        app.close()
    }
}
