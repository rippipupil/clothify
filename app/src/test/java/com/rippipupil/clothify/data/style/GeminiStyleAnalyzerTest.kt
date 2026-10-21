package com.rippipupil.clothify.data.style

import com.rippipupil.clothify.domain.TagVocabulary
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiStyleAnalyzerTest {
    private val server = MockWebServer()
    private val jpeg = byteArrayOf(1, 2, 3)

    @After
    fun tearDown() = server.shutdown()

    private fun response(text: String) =
        """{"candidates":[{"content":{"parts":[{"text":${kotlinx.serialization.json.JsonPrimitive(text)}}]}}]}"""

    private fun analyzer(key: String = "k") =
        GeminiStyleAnalyzer(OkHttpClient(), key, "gemini-flash-latest", server.url("/").toString())

    @Test
    fun parserKeepsKnownTagsWithoutDuplicates() =
        assertEquals(
            listOf("sudadera", "negro"),
            GeminiResponseParser.parse(response("""{"tags":["sudadera","inventada","sudadera","negro"]}""")),
        )

    @Test
    fun parserToleratesBrokenJson() {
        assertEquals(emptyList<String>(), GeminiResponseParser.parse("nope"))
        assertEquals(emptyList<String>(), GeminiResponseParser.parse(response("not json")))
    }

    @Test
    fun requestAsksForStructuredJsonWithEveryTag() {
        val body = GeminiRequest.body("QUJD")
        assertTrue(body.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(body.contains("\"data\":\"QUJD\""))
        TagVocabulary.all.forEach { assertTrue(it.id, body.contains("\"${it.id}\"")) }
    }

    @Test
    fun missingKeyFailsWithoutRequest() = runTest {
        assertEquals(StyleResult.Failure(FailureReason.MISSING_KEY), analyzer(key = "").analyzeJpeg(jpeg))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun successReturnsTags() = runTest {
        server.enqueue(MockResponse().setBody(response("""{"tags":["chaqueta","denim"]}""")))
        assertEquals(StyleResult.Success(listOf("chaqueta", "denim")), analyzer().analyzeJpeg(jpeg))
        val request = server.takeRequest()
        assertEquals("/v1beta/models/gemini-flash-latest:generateContent", request.requestUrl!!.encodedPath)
        assertEquals("k", request.getHeader("x-goog-api-key"))
    }

    @Test
    fun noValidTagsIsNoTags() = runTest {
        server.enqueue(MockResponse().setBody(response("""{"tags":["inventada"]}""")))
        assertEquals(StyleResult.Failure(FailureReason.NO_TAGS), analyzer().analyzeJpeg(jpeg))
    }

    @Test
    fun rateLimitIsQuotaExceeded() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        assertEquals(StyleResult.Failure(FailureReason.QUOTA_EXCEEDED), analyzer().analyzeJpeg(jpeg))
    }

    @Test
    fun unreachableIsNoInternet() = runTest {
        val a = analyzer()
        server.shutdown()
        assertEquals(StyleResult.Failure(FailureReason.NO_INTERNET), a.analyzeJpeg(jpeg))
    }

    @Test
    fun otherErrorsAreUnknown() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertEquals(StyleResult.Failure(FailureReason.UNKNOWN), analyzer().analyzeJpeg(jpeg))
    }
}
