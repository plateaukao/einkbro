package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.MarkdownBlock
import info.plateaukao.einkbro.unit.MarkdownBlocks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI answer with `![alt](url)` images: text runs keep the AnnotatedString
 * rendering of the plain popup, each image becomes an [RemoteImage] in between.
 * Blocks are keyed by position and content so a streaming answer, which
 * re-splits on every chunk, doesn't refetch images already shown.
 */
@Composable
fun RichMarkdownResponse(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { MarkdownBlocks.split(markdown) }
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            key(index, block) {
                when (block) {
                    is MarkdownBlock.Text -> Text(
                        text = HelperUnit.parseMarkdown(block.markdown),
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )

                    is MarkdownBlock.Image -> RemoteImage(
                        url = block.url,
                        alt = block.alt,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Fetches [url] once and shows it full-width; the alt text stands in until then, or for good on failure. */
@Composable
fun RemoteImage(url: String, alt: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = withContext(Dispatchers.IO) { loadBitmap(url) }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = alt.ifBlank { null },
            modifier = modifier,
            contentScale = ContentScale.FillWidth,
        )
    } else {
        Text(
            text = alt.ifBlank { url },
            color = MaterialTheme.colors.onBackground,
            fontStyle = FontStyle.Italic,
            modifier = modifier,
        )
    }
}

// Wider than any e-reader screen; keeps a stray full-size photo from eating the
// heap on a 2 GB device.
private const val MAX_IMAGE_WIDTH = 1600

private fun loadBitmap(url: String): Bitmap? {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return null
    return try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        val bytes = connection.inputStream.use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= MAX_IMAGE_WIDTH) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (e: Exception) {
        Timber.w(e, "Markdown image failed to load: $url")
        null
    }
}
