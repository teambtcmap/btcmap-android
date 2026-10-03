package org.btcmap.desktop

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okio.Buffer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * The desktop host must load the remote images the shared screens request (area
 * chips, place photos, search results). Coil has no built-in fetcher on the
 * JVM, so this guards the `coil-network-okhttp` dependency that supplies one.
 */
class RemoteImageLoadingTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    @Test
    fun defaultLoaderFetchesHttpImages() = runBlocking<Unit> {
        val png = ByteArrayOutputStream().also { out ->
            ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", out)
        }.toByteArray()

        serverRule.server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "image/png")
                .body(Buffer().write(png))
                .build(),
        )

        val loader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        val result = loader.execute(
            ImageRequest.Builder(PlatformContext.INSTANCE)
                .data(serverRule.server.url("/chip.png").toString())
                .build(),
        )

        assertTrue("expected a decoded image, got $result", result is SuccessResult)
    }

    @Test
    fun defaultLoaderDecodesSvgImages() = runBlocking<Unit> {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="2" height="2">
              <rect width="2" height="2" fill="#0e95af"/>
            </svg>
        """.trimIndent().toByteArray()

        serverRule.server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "image/svg+xml")
                .body(Buffer().write(svg))
                .build(),
        )

        val loader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        val result = loader.execute(
            ImageRequest.Builder(PlatformContext.INSTANCE)
                .data(serverRule.server.url("/flag.svg").toString())
                .build(),
        )

        assertTrue("expected a decoded SVG, got $result", result is SuccessResult)
    }
}
