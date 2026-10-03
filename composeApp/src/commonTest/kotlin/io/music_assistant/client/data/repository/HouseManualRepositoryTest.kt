package io.music_assistant.client.data.repository

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HouseManualRepositoryTest {
    private val good = """
        {"sections":[
          {"title":"Hot Tub","hotTubStatus":true,"items":[{"text":"Chemicals are under the stairs."}]},
          {"title":"Before You Leave","items":[{"text":"Lock up.","emphasized":true}]}
        ]}
    """.trimIndent()

    /** Wraps [inner] the way Firestore's REST API returns a document with a string field. */
    private fun firestoreDoc(inner: String) =
        """{"name":"projects/p/databases/(default)/documents/houseManual/main","fields":{"json":{"stringValue":${Json.encodeToString(String.serializer(), inner)}}}}"""

    private fun repo(settings: MapSettings = MapSettings(), engine: MockEngine) =
        HouseManualRepository(HttpClient(engine) { expectSuccess = true }, settings, "https://example.test/house-manual.json")

    @Test
    fun parsesSectionsAndFlags() {
        val sections = assertNotNull(HouseManualRepository.parse(good))
        assertEquals(listOf("Hot Tub", "Before You Leave"), sections.map { it.title })
        assertTrue(sections[0].hotTubStatus)
        assertTrue(sections[1].items.single().emphasized)
    }

    @Test
    fun extractsTheJsonFieldFromAFirestoreDocument() {
        assertEquals(good, HouseManualRepository.extractJson(firestoreDoc(good)))
        assertNull(HouseManualRepository.extractJson("""{"fields":{}}"""))
        assertNull(HouseManualRepository.extractJson("not json"))
    }

    @Test
    fun rejectsGarbageAndEmptyDocuments() {
        assertNull(HouseManualRepository.parse("not json"))
        assertNull(HouseManualRepository.parse("""{"sections":[]}"""))
        assertNull(HouseManualRepository.parse("""{"sections":[{"title":"","items":[{"text":"x"}]},{"title":"No lines"}]}"""))
    }

    @Test
    fun successfulFetchIsCachedForOfflineUse() = runTest {
        val settings = MapSettings()
        val ok = repo(settings, MockEngine { respond(firestoreDoc(good)) })
        assertNull(ok.cached())
        assertEquals(2, ok.fetch().getOrThrow().size)

        val offline = repo(settings, MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        assertTrue(offline.fetch().isFailure)
        assertEquals(2, assertNotNull(offline.cached()).size)
    }

    @Test
    fun badFileNeverReplacesTheCache() = runTest {
        val settings = MapSettings()
        repo(settings, MockEngine { respond(firestoreDoc(good)) }).fetch()
        val broken = repo(settings, MockEngine { respond(firestoreDoc("""{"sections":[]}""")) })
        assertTrue(broken.fetch().isFailure)
        assertEquals(2, assertNotNull(broken.cached()).size)
    }
}
