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
        plugin: MainAPI,
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
                plugin.newMovieSearchResponse(
                    channelName,
                    "livetv/$channelId",
                    TvType.Movie
                ) {
                    this.posterUrl = meta.poster?.takeIf { it.isNotBlank() }
                        ?: "https://ui-avatars.com/api/?name=${channelName.replace(" ", "+")}&background=random&color=fff&size=512"
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
        plugin: MainAPI,
        apibase: String,
        channelId: String,
        headers: Map<String, String>,
        mainUrl: String
    ): LoadResponse? {
        val url = "$apibase/livetv/stream/tv/$channelId?mode=sources"
        Log.d(TAG, "Loading channel: $url")

        return try {
            val response = app.get(url, headers = headers, timeout = 15).text
            val sourcesResponse = tryParseJson<LiveTvSourcesResponse>(response)
            val streamResponse = tryParseJson<LiveTvStreamResponse>(response)

            val serverCount = sourcesResponse?.sources?.size
                ?: streamResponse?.streams?.size ?: 0

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

            plugin.newMovieLoadResponse(
                channelName,
                "livetv/$channelId",
                TvType.Movie,
                "livetv/$channelId" // url is required for dataUrl argument
            ) {
                this.plot = description
                this.posterUrl = "https://ui-avatars.com/api/?name=${channelName.replace(" ", "+")}&background=random"
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error loading channel $channelId: ${e.message}")
            null
        }
    }

    /**
     * Extracts stream links for a live TV channel.
     */
    suspend fun loadStreamLinks(
        plugin: MainAPI,
        apibase: String,
        channelId: String,
        headers: Map<String, String>,
        mainUrl: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(TAG, "Loading stream links for: $channelId")

        try {
            val sourcesUrl = "$apibase/livetv/stream/tv/$channelId?mode=sources"
            val sourcesResponse = app.get(sourcesUrl, headers = headers, timeout = 15).text

            val sourcesData = tryParseJson<LiveTvSourcesResponse>(sourcesResponse)

            if (sourcesData?.sources != null && sourcesData.sources.isNotEmpty()) {
                for ((index, source) in sourcesData.sources.withIndex()) {
                    try {
                        val streamUrl = "$apibase/livetv/stream/tv/$channelId?sourceIndex=$index"
                        val streamResponse = app.get(streamUrl, headers = headers, timeout = 15).text
                        val streamData = tryParseJson<LiveTvStreamResponse>(streamResponse)

                        streamData?.streams?.forEach { stream ->
                            processStream(plugin, stream, source.name ?: "Source ${index + 1}", mainUrl, callback)
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Error fetching source $index: ${e.message}")
                    }
                }
            }

            val directStreams = tryParseJson<LiveTvStreamResponse>(sourcesResponse)
            if (directStreams?.streams != null && directStreams.streams.isNotEmpty()) {
                directStreams.streams.forEach { stream ->
                    processStream(plugin, stream, "Direct", mainUrl, callback)
                }
            }

            try {
                val fallbackUrl = "$apibase/livetv/stream/tv/$channelId"
                val fallbackResponse = app.get(fallbackUrl, headers = headers, timeout = 15).text
                val fallbackData = tryParseJson<LiveTvStreamResponse>(fallbackResponse)
                fallbackData?.streams?.forEach { stream ->
                    processStream(plugin, stream, "Live", mainUrl, callback)
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

    private fun processStream(
        plugin: MainAPI,
        stream: LiveTvStream,
        sourceName: String,
        mainUrl: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val streamUrl = stream.url ?: return
        if (streamUrl.isBlank()) return

        val streamName = stream.name ?: stream.title ?: sourceName
        val displayName = "LIVE | $streamName"

        val linkType = when {
            streamUrl.contains(".m3u8") -> ExtractorLinkType.M3U8
            streamUrl.contains(".mpd") -> ExtractorLinkType.DASH
            streamUrl.contains(".ts") -> ExtractorLinkType.VIDEO
            else -> ExtractorLinkType.M3U8 
        }

        val customHeaders = stream.behaviorHints?.proxyHeaders?.request ?: emptyMap()

        val finalHeaders = buildMap {
            putAll(customHeaders)
            if (!containsKey("Referer")) put("Referer", mainUrl)
            if (!containsKey("Origin")) put("Origin", mainUrl)
            if (!containsKey("User-Agent")) put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:149.0) Gecko/20100101 Firefox/149.0")
        }

        Log.d(TAG, "Adding stream: $displayName -> $streamUrl")

        val extLink = ExtractorLink(
            source = "MovixLive",
            name = displayName,
            url = streamUrl,
            referer = customHeaders["Referer"] ?: mainUrl,
            quality = Qualities.Unknown.value,
            type = linkType,
            headers = finalHeaders
        )
        
        callback(extLink)
    }
}
