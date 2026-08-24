package io.github.seancheng.searchbyimage.data.network

import io.github.seancheng.searchbyimage.domain.EngineCatalog
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterParsingTest {
    @Test
    fun traceMoeParsesNativeResults() {
        val payload = """
            {
              "result": [{
                "anilist": {"siteUrl":"https://anilist.co/anime/1", "title":{"native":"测试动画"}},
                "episode": 3,
                "from": 125.5,
                "similarity": 0.934,
                "image": "https://cdn.example.com/frame.jpg",
                "video": "https://cdn.example.com/clip.mp4"
              }]
            }
        """.trimIndent()
        val adapter = TraceMoeAdapter(requireNotNull(EngineCatalog.find("trace_moe")))
        val result = adapter.parse(payload).single()
        assertEquals("测试动画", result.title)
        assertEquals("第 3 集 · 02:05", result.subtitle)
        assertEquals(93.4, result.similarity ?: 0.0, 0.01)
        assertEquals("https://cdn.example.com/frame.jpg", result.thumbnailUrl)
    }

    @Test
    fun sauceNaoRejectsUnsafeExternalUrl() {
        val payload = """
            {"results":[{
              "header":{"similarity":"88.4","thumbnail":"https://img.example.com/a.jpg"},
              "data":{"title":"作品","ext_urls":["http://unsafe.example/a"]}
            }]}
        """.trimIndent()
        val adapter = SauceNaoAdapter(requireNotNull(EngineCatalog.find("saucenao")))
        assertTrue(adapter.parse(payload).isEmpty())
    }

    @Test
    fun lensoParsesNestedUrlLists() {
        val payload = """
            {"results":[{
              "confidenceScore":91.2,
              "date":"2026-01-20",
              "urlList":[{"title":"来源页面","imageUrl":"https://img.example/a.jpg","sourceUrl":"https://source.example/post"}]
            }],"availablePages":1}
        """.trimIndent()
        val adapter = LensoAdapter(requireNotNull(EngineCatalog.find("lenso")))
        val result = adapter.parse(payload).single()
        assertEquals("来源页面", result.title)
        assertEquals(91.2, result.similarity ?: 0.0, 0.01)
    }

    @Test
    fun tinEyeParsesBacklinksAndRejectsCleartextOnes() {
        val payload = JSONObject(
            """
            {"code":200,"results":{"matches":[{
              "domain":"example.com",
              "score":84.5,
              "image_url":"https://img.example/match.jpg",
              "backlinks":[
                {"backlink":"https://example.com/source","url":"https://img.example/full.jpg","crawl_date":"2026-01-10"},
                {"backlink":"http://unsafe.example/source"}
              ]
            }]}}
            """.trimIndent(),
        )
        val adapter = TinEyeAdapter(requireNotNull(EngineCatalog.find("tineye")))
        val result = adapter.parse(payload).single()
        assertEquals("example.com", result.title)
        assertEquals("https://example.com/source", result.sourceUrl)
        assertEquals(84.5, result.similarity ?: 0.0, 0.01)
    }

    @Test
    fun safeUrlOnlyAllowsHttpsWithoutCredentials() {
        assertEquals("https://example.com/a", safeResultUrl("https://example.com/a"))
        assertNull(safeResultUrl("http://example.com/a"))
        assertNull(safeResultUrl("https://user:pass@example.com/a"))
        assertNull(safeResultUrl("https://127.0.0.1/a"))
        assertNull(safeResultUrl("https://router.local/a"))
    }
}
