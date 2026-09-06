package info.plateaukao.einkbro.browser

/**
 * Identifies subresource requests that WebView is loading as images. Sec-Fetch-Dest is
 * authoritative when supplied; Accept handles extensionless CDN URLs. The path fallback
 * only covers older WebViews that supply neither signal.
 */
object ImageRequestClassifier {
    fun isNetworkImage(
        url: String,
        requestHeaders: Map<String, String>,
        isMainFrame: Boolean,
    ): Boolean {
        if (isMainFrame || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            return false
        }

        val fetchDestination = requestHeaders.headerValue("Sec-Fetch-Dest")
        if (fetchDestination != null) return fetchDestination.equals("image", ignoreCase = true)

        val accept = requestHeaders.headerValue("Accept")
        if (accept?.trimStart()?.startsWith("image/", ignoreCase = true) == true) return true

        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return IMAGE_EXTENSIONS.any(path::endsWith)
    }

    fun Map<String, String>.headerValue(name: String): String? =
        entries.firstOrNull { (headerName, _) -> headerName.equals(name, ignoreCase = true) }?.value

    private val IMAGE_EXTENSIONS = setOf(
        ".avif",
        ".bmp",
        ".gif",
        ".ico",
        ".jpeg",
        ".jpg",
        ".png",
        ".svg",
        ".webp",
    )
}
