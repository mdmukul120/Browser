package com.example.adblock

import android.net.Uri
import java.io.ByteArrayInputStream
import android.webkit.WebResourceResponse
import java.util.concurrent.atomic.AtomicInteger

object AdBlockEngine {
    private val blockedAdsCount = AtomicInteger(0)

    // Known ad, analytics & tracking hosts and patterns
    private val blockedDomains = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "ads.google.com",
        "taboola.com",
        "outbrain.com",
        "adnxs.com",
        "adroll.com",
        "criteo.com",
        "criteo.net",
        "scorecardresearch.com",
        "quantserve.com",
        "moatads.com",
        "amazon-adsystem.com",
        "popads.net",
        "popcash.net",
        "adcolony.com",
        "applovin.com",
        "unity3d.com/ads",
        "smartadserver.com",
        "rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "pubmatic.com",
        "chartbeat.com",
        "hotjar.com",
        "crazyegg.com"
    )

    private val adKeywordPatterns = listOf(
        "/ads/", "/ad/", "/advert/", "/banner/", "adserver", "adsbygoogle",
        "tracking", "telemetry", "analytics.js", "gtag/js"
    )

    // Cosmetic CSS to hide ad spaces
    const val COSMETIC_ADBLOCK_CSS = """
        ins.adsbygoogle,
        [id*="google_ads"],
        [class*="adsbox"],
        [class*="ad-banner"],
        [class*="ad-container"],
        [class*="ad_wrapper"],
        [class*="sponsored-post"],
        [id^="ad-"],
        [class^="ad-"],
        .taboola,
        .outbrain,
        #carbonads {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            max-height: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
    """

    fun isAdOrTracker(url: String): Boolean {
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false

            // Check if domain or any parent domain is in blocked list
            for (blocked in blockedDomains) {
                if (host == blocked || host.endsWith(".$blocked")) {
                    return true
                }
            }

            // Path keyword check
            val path = uri.path?.lowercase() ?: ""
            for (pattern in adKeywordPatterns) {
                if (path.contains(pattern)) {
                    return true
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        }
        return false
    }

    fun createEmptyResponse(): WebResourceResponse {
        blockedAdsCount.incrementAndGet()
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun getBlockedCount(): Int = blockedAdsCount.get()

    fun resetCount() {
        blockedAdsCount.set(0)
    }
}
