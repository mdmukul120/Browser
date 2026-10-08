package com.example.devtools

import android.webkit.JavascriptInterface
import org.json.JSONObject

interface DevToolsListener {
    fun onConsoleLogReceived(entry: ConsoleLogEntry)
    fun onNetworkRequestCaptured(entry: NetworkRequestEntry)
    fun onElementSelected(element: DomSelectedElement)
    fun onStorageExtracted(entries: List<StorageEntry>)
    fun onPerformanceMetricsUpdated(metrics: PerformanceMetrics)
    fun onPageSourceReady(source: String)
}

class DevToolsBridge(private val listener: DevToolsListener) {

    @JavascriptInterface
    fun onConsoleLog(levelStr: String, message: String, stack: String) {
        val level = try {
            ConsoleLogLevel.valueOf(levelStr.uppercase())
        } catch (_: Exception) {
            ConsoleLogLevel.LOG
        }
        val entry = ConsoleLogEntry(
            level = level,
            message = message,
            stack = stack.ifBlank { null }
        )
        listener.onConsoleLogReceived(entry)
    }

    @JavascriptInterface
    fun onNetworkRequest(
        url: String,
        method: String,
        status: Int,
        duration: Long,
        type: String,
        preview: String
    ) {
        val entry = NetworkRequestEntry(
            method = method,
            url = url,
            status = status,
            durationMs = duration,
            type = type,
            responseBody = preview.ifBlank { null }
        )
        listener.onNetworkRequestCaptured(entry)
    }

    @JavascriptInterface
    fun onDomElementSelected(
        tagName: String,
        id: String,
        className: String,
        textContent: String,
        outerHtml: String,
        attrsJson: String,
        stylesJson: String
    ) {
        val attrs = mutableMapOf<String, String>()
        try {
            val json = JSONObject(attrsJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                attrs[k] = json.optString(k)
            }
        } catch (_: Exception) {}

        val styles = mutableMapOf<String, String>()
        try {
            val json = JSONObject(stylesJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                styles[k] = json.optString(k)
            }
        } catch (_: Exception) {}

        val element = DomSelectedElement(
            tagName = tagName,
            id = id,
            className = className,
            textContent = textContent,
            outerHtml = outerHtml,
            attributes = attrs,
            computedStyles = styles
        )
        listener.onElementSelected(element)
    }

    @JavascriptInterface
    fun onStorageData(localJson: String, sessionJson: String, cookiesJson: String) {
        val list = mutableListOf<StorageEntry>()

        fun parseStorage(jsonStr: String, type: StorageType) {
            try {
                val obj = JSONObject(jsonStr)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    list.add(StorageEntry(type, k, obj.optString(k)))
                }
            } catch (_: Exception) {}
        }

        parseStorage(localJson, StorageType.LOCAL_STORAGE)
        parseStorage(sessionJson, StorageType.SESSION_STORAGE)
        parseStorage(cookiesJson, StorageType.COOKIE)

        listener.onStorageExtracted(list)
    }

    @JavascriptInterface
    fun onPerformanceMetrics(metricsJson: String) {
        try {
            val obj = JSONObject(metricsJson)
            val dns = obj.optLong("dns", 0)
            val tcp = obj.optLong("tcp", 0)
            val ttfb = obj.optLong("ttfb", 0)
            val domReady = obj.optLong("domReady", 0)
            val loadTime = obj.optLong("loadTime", 0)
            val resources = obj.optInt("resources", 0)
            val domNodes = obj.optInt("domNodes", 0)
            val memory = obj.optDouble("memory", 0.0)

            // Compute Lighthouse-like optimization score & suggestions
            var score = 100
            val suggestions = mutableListOf<String>()

            if (ttfb > 600) {
                score -= 15
                suggestions.add("High TTFB ($ttfb ms): Consider improving server response time or using edge CDN caching.")
            }
            if (domReady > 2500) {
                score -= 15
                suggestions.add("Slow DOMContentLoaded ($domReady ms): Defer non-critical JavaScript and eliminate render-blocking CSS.")
            }
            if (loadTime > 4000) {
                score -= 15
                suggestions.add("Slow full page load ($loadTime ms): Optimize heavy assets, lazy-load images, and compress media.")
            }
            if (domNodes > 1500) {
                score -= 10
                suggestions.add("Excessive DOM size ($domNodes elements): Flatten DOM hierarchy to improve rendering performance.")
            }
            if (resources > 80) {
                score -= 10
                suggestions.add("High resource count ($resources requests): Bundle JS/CSS files and sprite or combine images.")
            }

            if (suggestions.isEmpty()) {
                suggestions.add("Page performance is great! Clean resources and rapid render timing.")
            }

            val metrics = PerformanceMetrics(
                dnsLookupMs = dns,
                tcpConnectMs = tcp,
                ttfbMs = ttfb,
                domContentLoadedMs = domReady,
                fullLoadMs = loadTime,
                totalResources = resources,
                domNodeCount = domNodes,
                memoryUsageMb = memory,
                lighthouseScore = score.coerceIn(20, 100),
                suggestions = suggestions
            )
            listener.onPerformanceMetricsUpdated(metrics)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun onPageSourceExtracted(source: String) {
        listener.onPageSourceReady(source)
    }
}
