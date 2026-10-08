package com.example.devtools

enum class ConsoleLogLevel {
    LOG, INFO, WARN, ERROR, DEBUG, RESULT
}

data class ConsoleLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val level: ConsoleLogLevel,
    val message: String,
    val stack: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val repeatCount: Int = 1
)

data class NetworkRequestEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val method: String,
    val url: String,
    val status: Int,
    val durationMs: Long,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val headers: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseBody: String? = null
)

data class DomSelectedElement(
    val tagName: String,
    val id: String = "",
    val className: String = "",
    val textContent: String = "",
    val outerHtml: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val computedStyles: Map<String, String> = emptyMap()
)

data class StorageEntry(
    val storageType: StorageType,
    val key: String,
    val value: String
)

enum class StorageType {
    LOCAL_STORAGE, SESSION_STORAGE, COOKIE
}

data class PerformanceMetrics(
    val dnsLookupMs: Long = 0,
    val tcpConnectMs: Long = 0,
    val ttfbMs: Long = 0,
    val domContentLoadedMs: Long = 0,
    val fullLoadMs: Long = 0,
    val totalResources: Int = 0,
    val domNodeCount: Int = 0,
    val memoryUsageMb: Double = 0.0,
    val lighthouseScore: Int = 85,
    val suggestions: List<String> = emptyList()
)
