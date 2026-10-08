package com.example.offline

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val isRunning: Boolean = false,
    val phase: String = "",
    val currentUrl: String = "",
    val pagesDownloaded: Int = 0,
    val maxPages: Int = 15,
    val assetsDownloaded: Int = 0,
    val totalBytes: Long = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)

class OfflineSiteDownloader(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    suspend fun downloadEntireWebsite(
        startUrl: String,
        maxPagesLimit: Int = 20,
        maxDepth: Int = 2
    ): Result<SavedSiteResult> = withContext(Dispatchers.IO) {
        val siteId = java.util.UUID.randomUUID().toString()
        val siteDir = File(context.filesDir, "saved_sites/$siteId")
        val assetsDir = File(siteDir, "assets")
        siteDir.mkdirs()
        assetsDir.mkdirs()

        _progress.value = DownloadProgress(
            isRunning = true,
            phase = "Initializing archive...",
            currentUrl = startUrl,
            maxPages = maxPagesLimit
        )

        val baseUri = Uri.parse(startUrl)
        val targetHost = baseUri.host?.lowercase() ?: ""
        if (targetHost.isEmpty()) {
            _progress.value = _progress.value.copy(isRunning = false, error = "Invalid URL")
            return@withContext Result.failure(IllegalArgumentException("Invalid URL"))
        }

        // URL queue: Pair(url, depth)
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(Pair(startUrl, 0))

        val visitedUrls = mutableSetOf<String>()
        val downloadedPages = mutableMapOf<String, File>() // url -> local HTML file
        val downloadedAssets = mutableMapOf<String, String>() // assetUrl -> relative asset filename
        var totalBytesDownloaded = 0L
        var siteTitle = targetHost

        try {
            // STEP 1: Crawl and fetch HTML pages
            while (queue.isNotEmpty() && visitedUrls.size < maxPagesLimit) {
                val (currentUrl, depth) = queue.removeFirst()
                val normalizedCurrent = normalizeUrl(currentUrl)

                if (visitedUrls.contains(normalizedCurrent)) continue
                visitedUrls.add(normalizedCurrent)

                _progress.value = _progress.value.copy(
                    phase = "Downloading page ${visitedUrls.size}/$maxPagesLimit",
                    currentUrl = currentUrl,
                    pagesDownloaded = visitedUrls.size
                )

                try {
                    val request = Request.Builder()
                        .url(currentUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) DevChrome/1.0")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val htmlBody = response.body?.string() ?: return@use
                        val doc = Jsoup.parse(htmlBody, currentUrl)

                        if (visitedUrls.size == 1) {
                            val titleText = doc.title().trim()
                            if (titleText.isNotEmpty()) {
                                siteTitle = titleText
                            }
                        }

                        // Save temporarily to process after asset discovery
                        val pageFileName = if (downloadedPages.isEmpty()) "index.html" else "page_${visitedUrls.size}.html"
                        val pageFile = File(siteDir, pageFileName)
                        downloadedPages[normalizedCurrent] = pageFile

                        // Discover same-origin links for next depth
                        if (depth < maxDepth && visitedUrls.size < maxPagesLimit) {
                            val links = doc.select("a[href]")
                            for (link in links) {
                                val absHref = link.attr("abs:href")
                                if (absHref.isNotBlank()) {
                                    val linkUri = Uri.parse(absHref)
                                    val linkHost = linkUri.host?.lowercase() ?: ""
                                    val scheme = linkUri.scheme?.lowercase() ?: ""

                                    if ((scheme == "http" || scheme == "https") &&
                                        linkHost == targetHost &&
                                        !absHref.contains("#") &&
                                        !isMediaExtension(absHref)
                                    ) {
                                        val normLink = normalizeUrl(absHref)
                                        if (!visitedUrls.contains(normLink)) {
                                            queue.add(Pair(absHref, depth + 1))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip failed page and continue
                }
            }

            // STEP 2: Collect and download all assets (CSS, JS, Images, Icons)
            _progress.value = _progress.value.copy(
                phase = "Extracting assets from downloaded pages..."
            )

            val assetUrlsToDownload = mutableSetOf<String>()

            // Re-parse downloaded pages to discover assets
            val parsedDocs = mutableMapOf<String, org.jsoup.nodes.Document>()
            for ((pageUrl, pageFile) in downloadedPages) {
                try {
                    val request = Request.Builder().url(pageUrl).build()
                    httpClient.newCall(request).execute().use { resp ->
                        val html = resp.body?.string() ?: return@use
                        val doc = Jsoup.parse(html, pageUrl)
                        parsedDocs[pageUrl] = doc

                        // Stylesheets
                        doc.select("link[rel~=(?i)stylesheet]").forEach {
                            val src = it.attr("abs:href")
                            if (src.isNotBlank()) assetUrlsToDownload.add(src)
                        }

                        // Scripts
                        doc.select("script[src]").forEach {
                            val src = it.attr("abs:src")
                            if (src.isNotBlank()) assetUrlsToDownload.add(src)
                        }

                        // Images
                        doc.select("img[src], img[data-src]").forEach {
                            val src = it.attr("abs:src").ifBlank { it.attr("abs:data-src") }
                            if (src.isNotBlank()) assetUrlsToDownload.add(src)
                        }

                        // Favicon / icons
                        doc.select("link[rel~=(?i)icon]").forEach {
                            val src = it.attr("abs:href")
                            if (src.isNotBlank()) assetUrlsToDownload.add(src)
                        }
                    }
                } catch (_: Exception) {}
            }

            // Download assets
            var assetIndex = 0
            for (assetUrl in assetUrlsToDownload) {
                assetIndex++
                _progress.value = _progress.value.copy(
                    phase = "Downloading asset $assetIndex/${assetUrlsToDownload.size}",
                    currentUrl = assetUrl,
                    assetsDownloaded = assetIndex
                )

                try {
                    val req = Request.Builder().url(assetUrl).build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes() ?: return@use
                            val extension = getExtensionFromUrl(assetUrl)
                            val hash = sha256Short(assetUrl)
                            val assetFileName = "asset_${hash}${extension}"
                            val assetFile = File(assetsDir, assetFileName)

                            FileOutputStream(assetFile).use { it.write(bytes) }
                            totalBytesDownloaded += bytes.size
                            downloadedAssets[assetUrl] = "assets/$assetFileName"
                        }
                    }
                } catch (_: Exception) {}
            }

            // STEP 3: Rewrite links in HTML to make everything work offline!
            _progress.value = _progress.value.copy(
                phase = "Rewriting internal links for offline browsing..."
            )

            for ((pageUrl, doc) in parsedDocs) {
                val targetFile = downloadedPages[pageUrl] ?: continue

                // 1. Rewrite stylesheets
                doc.select("link[rel~=(?i)stylesheet]").forEach {
                    val abs = it.attr("abs:href")
                    downloadedAssets[abs]?.let { localRel -> it.attr("href", localRel) }
                }

                // 2. Rewrite scripts
                doc.select("script[src]").forEach {
                    val abs = it.attr("abs:src")
                    downloadedAssets[abs]?.let { localRel -> it.attr("src", localRel) }
                }

                // 3. Rewrite images
                doc.select("img[src]").forEach {
                    val abs = it.attr("abs:src")
                    downloadedAssets[abs]?.let { localRel -> it.attr("src", localRel) }
                }

                // 4. Rewrite internal page links to local page HTML files
                doc.select("a[href]").forEach {
                    val abs = it.attr("abs:href")
                    val norm = normalizeUrl(abs)
                    downloadedPages[norm]?.let { localPageFile ->
                        it.attr("href", localPageFile.name)
                    }
                }

                // Write the localized HTML to disk
                targetFile.writeText(doc.outerHtml(), Charsets.UTF_8)
            }

            val indexFile = File(siteDir, "index.html")
            val result = SavedSiteResult(
                id = siteId,
                title = siteTitle,
                originalUrl = startUrl,
                localIndexPath = indexFile.absolutePath,
                totalPages = downloadedPages.size,
                totalAssets = downloadedAssets.size,
                totalBytes = totalBytesDownloaded
            )

            _progress.value = DownloadProgress(
                isRunning = false,
                isComplete = true,
                phase = "Website downloaded successfully!",
                pagesDownloaded = downloadedPages.size,
                assetsDownloaded = downloadedAssets.size,
                totalBytes = totalBytesDownloaded
            )

            Result.success(result)
        } catch (e: Exception) {
            _progress.value = DownloadProgress(
                isRunning = false,
                error = e.localizedMessage ?: "Download failed"
            )
            Result.failure(e)
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.substringBefore("#").trimEnd('/')
    }

    private fun getExtensionFromUrl(url: String): String {
        val clean = url.substringBefore("?").substringBefore("#")
        val ext = clean.substringAfterLast(".", "")
        return if (ext.isNotEmpty() && ext.length in 2..5) ".$ext" else ".bin"
    }

    private fun isMediaExtension(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".pdf") || lower.endsWith(".zip") ||
               lower.endsWith(".mp4") || lower.endsWith(".mp3") ||
               lower.endsWith(".apk")
    }

    private fun sha256Short(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.take(6).joinToString("") { "%02x".format(it) }
    }
}

data class SavedSiteResult(
    val id: String,
    val title: String,
    val originalUrl: String,
    val localIndexPath: String,
    val totalPages: Int,
    val totalAssets: Int,
    val totalBytes: Long
)
