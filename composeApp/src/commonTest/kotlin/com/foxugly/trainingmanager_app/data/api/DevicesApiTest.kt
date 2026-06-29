package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DevicesApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun registerSucceeds() = runTest {
        val api = api(MockEngine { respond("""{"id":1,"created":true}""", HttpStatusCode.Created, jsonHeader) })
        assertTrue(api.registerDevice(DeviceRegisterBody("a".repeat(40), "android", "Pixel")).isSuccess)
    }

    @Test fun unregisterSucceedsOn204() = runTest {
        val api = api(MockEngine { respond("", HttpStatusCode.NoContent) })
        assertTrue(api.unregisterDevice(DeviceUnregisterBody("a".repeat(40))).isSuccess)
    }
}
