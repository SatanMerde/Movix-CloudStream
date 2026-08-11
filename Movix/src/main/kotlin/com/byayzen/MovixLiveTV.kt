package com.byayzen

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

object MovixLiveTV {

    private const val TAG = "MovixLiveTV"

    /**
     * Fetches the list of channels for a given catalog category.
     * Returns a list of LiveSearchResponse for the CloudStream home page.
     */
    suspend fun fetchCatalog(
        apibase: String,
        catalogId: String,
        headers: Map<String, String>
    ): List<SearchResponse> {
        val url = "$apibase/livetv/catalog/tv/$catalogId"
        Log.d(TAG, "Fetching catalog: $url")

        return try {
            val response = app.get(url, headers = headers, timeout = 15).text
            val parsed = tryParseJson<LiveTvCatalogResponse>(response)
            parsed?.metas?.mapNotNull { meta ->
                val channelName = meta.name ?: return@mapNotNull null
                val channelId = meta.id ?: return@mapNotNull null
                newMovieSearchResponse(
                    channelName,
                    "livetv/$channelId",
                    TvType.Live
                ) {
                    this.posterUrl = meta.poster
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.d(TAG, "Error fetching catalog $catalogId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Loads channel detail for the detail page in CloudStream.
     */
    suspend fun loadChannel(
        apibase: String,
        channelId: String,
        headers: Map<String, String>,
        mainUrl: String
    ): LoadResponse? {
        // Try to get the channel info from the stream endpoint with mode=sources
        val url = "$apibase/livetv/stream/tv/$channelId?mode=sources"
        Log.d(TAG, "Loading channel: $url")

        return try {
            val response = app.get(url, headers = headers, timeout = 15).text

            // Try to extract channel name from multiple possible response formats
            val sourcesResponse = tryParseJson<LiveTvSourcesResponse>(response)
            val streamResponse = tryParseJson<LiveTvStreamResponse>(response)

            val serverCount = sourcesResponse?.sources?.size
                ?: streamResponse?.streams?.size ?: 0

            // Use channel ID as fallback name, clean it up
            val channelName = channelId
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }

            val description = if (serverCount > 0) {
                "\uD83D\uDCE1 $serverCount source(s) disponible(s) — Chaîne en direct"
            } else {
                "\uD83D\uDCE1 Chaîne en direct"
            }

            newMovieLoadResponse(
                channelName,
                "livetv/$channelId",
                TvType.Live,
                "livetv/$channelId"
            ) {
                this.plot = description
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error loading channel $channelId: ${e.message}")
            null
        }
    }

    /**
     * Extracts stream links for a live TV channel.
     * First tries mode=sources to get the list of sources,
     * then fetches each source by index.
     */
    suspend fun loadStreamLinks(
        apibase: String,
        channelId: String,
        headers: Map<String, String>,
        mainUrl: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(TAG, "Loading stream links for: $channelId")

        try {
            // Step 1: Get available sources
            val sourcesUrl = "$apibase/livetv/stream/tv/$channelId?mode=sources"
            val sourcesResponse = app.get(sourcesUrl, headers = headers, timeout = 15).text
            Log.d(TAG, "Sources response length: ${sourcesResponse.length}")

            // Try parsing as sources list first
            val sourcesData = tryParseJson<LiveTvSourcesResponse>(sourcesResponse)

            if (sourcesData?.sources != null && sourcesData.sources.isNotEmpty()) {
                // We have a list of sources, fetch each one by index
                Log.d(TAG, "Found ${sourcesData.sources.size} sources")

                for ((index, source) in sourcesData.sources.withIndex()) {
                    try {
                        val streamUrl = "$apibase/livetv/stream/tv/$channelId?sourceIndex=$index"
                        val streamResponse = app.get(streamUrl, headers = headers, timeout = 15).text
                        val streamData = tryParseJson<LiveTvStreamResponse>(streamResponse)

                        streamData?.streams?.forEach { stream ->
                            processStream(stream, source.name ?: "Source ${index + 1}", mainUrl, callback)
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Error fetching source $index: ${e.message}")
                    }
                }
            }

            // Also try parsing as direct streams response
            val directStreams = tryParseJson<LiveTvStreamResponse>(sourcesResponse)
            if (directStreams?.streams != null && directStreams.streams.isNotEmpty()) {
                Log.d(TAG, "Found ${directStreams.streams.size} direct streams")
                directStreams.streams.forEach { stream ->
                    processStream(stream, "Direct", mainUrl, callback)
                }
            }

            // Step 2: Also try without mode parameter as fallback
            try {
                val fallbackUrl = "$apibase/livetv/stream/tv/$channelId"
                val fallbackResponse = app.get(fallbackUrl, headers = headers, timeout = 15).text
                val fallbackData = tryParseJson<LiveTvStreamResponse>(fallbackResponse)
                fallbackData?.streams?.forEach { stream ->
                    processStream(stream, "Live", mainUrl, callback)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Fallback stream fetch error: ${e.message}")
            }

        } catch (e: Exception) {
            Log.d(TAG, "Error loading stream links: ${e.message}")
            return false
        }

        return true
    }

    /**
     * Processes a single stream entry and passes it to the callback.
     */
    private fun processStream(
        stream: LiveTvStream,
        sourceName: String,
        mainUrl: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val streamUrl = stream.url ?: return
        if (streamUrl.isBlank()) return

        val streamName = stream.name ?: stream.title ?: sourceName
        val displayName = "LIVE | $streamName"

        // Determine stream type
        val linkType = when {
            streamUrl.contains(".m3u8") -> ExtractorLinkType.M3U8
            streamUrl.contains(".mpd") -> ExtractorLinkType.DASH
            streamUrl.contains(".ts") -> ExtractorLinkType.VIDEO
            else -> ExtractorLinkType.M3U8 // Default to M3U8 for live streams
        }

        // Extract custom headers if provided
        val customHeaders = stream.behaviorHints?.proxyHeaders?.request ?: emptyMap()

        val finalHeaders = buildMap {
            putAll(customHeaders)
            if (!containsKey("Referer")) put("Referer", mainUrl)
            if (!containsKey("Origin")) put("Origin", mainUrl)
            if (!containsKey("User-Agent")) put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:149.0) Gecko/20100101 Firefox/149.0")
        }

        Log.d(TAG, "Adding stream: $displayName -> $streamUrl")

        callback(
            newExtractorLink(
                source = "MovixLive",
                name = displayName,
                url = streamUrl,
                type = linkType
            ) {
                this.headers = finalHeaders
                this.quality = Qualities.Unknown.value
                this.referer = customHeaders["Referer"] ?: mainUrl
            }
        )
    }
}
