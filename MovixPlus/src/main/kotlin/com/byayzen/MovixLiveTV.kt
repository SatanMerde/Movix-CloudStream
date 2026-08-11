package com.byayzen

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import android.util.Base64

object MovixLiveTV {

    private const val TAG = "MovixLiveTV"

    private val channelLogos = mapOf(
        "TF1" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/tf1-fr.png",
        "France 2" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/france-2-fr.png",
        "France 3" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/france-3-fr.png",
        "France 4" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/france-4-fr.png",
        "France 5" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/france-5-fr.png",
        "M6" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/m6-fr.png",
        "Arte" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/arte-fr.png",
        "C8" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/c8-fr.png",
        "W9" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/w9-fr.png",
        "TMC" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/tmc-fr.png",
        "TFX" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/tfx-fr.png",
        "NRJ 12" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/nrj-12-fr.png",
        "BFM TV" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png",
        "CNEWS" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/c-news-fr.png",
        "CSTAR" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/c-star-fr.png",
        "Gulli" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/gulli-fr.png",
        "L'Équipe" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/lequipe-fr.png",
        "6ter" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/6ter-fr.png",
        "RMC Story" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/rmc-story-fr.png",
        "RMC Découverte" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/rmc-decouverte-fr.png",
        "RTL9" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/rtl9-fr.png",
        "AB1" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/ab1-fr.png",
        "Action" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/action-fr.png",
        "Paramount Channel" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/paramount-channel-fr.png",
        
        "CANAL+" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-fr.png",
        "CANAL+ Sport" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-sport-fr.png",
        "CANAL+ Foot" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-foot-fr.png",
        "CANAL+ Cinema" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-cinemas-fr.png",
        "CANAL+ Grand Ecran" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-grand-ecran-fr.png",
        "CANAL+ Series" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-series-fr.png",
        "CANAL+ Docs" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-docs-fr.png",
        "CANAL+ Kids" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/canal-plus-kids-fr.png",
        
        "beIN SPORTS 1" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bein-sports-1-french-fr.png",
        "beIN SPORTS 2" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bein-sports-2-french-fr.png",
        "beIN SPORTS 3" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bein-sports-3-french-fr.png",
        
        "Eurosport 1" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/eurosport-1-fr.png",
        "Eurosport 2" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/eurosport-2-fr.png",
        "RMC Sport 1" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/rmc-sport-1-fr.png",
        "RMC Sport 2" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/rmc-sport-2-fr.png",
        
        "Teva" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/teva-fr.png",
        "Paris Premiere" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/paris-premiere-fr.png",
        "Disney Channel" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/disney-channel-fr.png",
        "Nickelodeon" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/nickelodeon-fr.png",
        "Cartoon Network" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/cartoon-network-fr.png",
        "Ushuaia TV" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/ushuaia-tv-fr.png",
        "Histoire TV" to "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/histoire-tv-fr.png"
    )

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
                
                // Try API poster, then our hardcoded high-quality logos, then fallback avatar
                val poster = meta.poster?.takeIf { it.isNotBlank() }
                    ?: channelLogos[channelName]
                    ?: channelLogos.entries.firstOrNull { channelName.replace(" ", "").contains(it.key.replace(" ", ""), ignoreCase = true) }?.value
                    ?: "https://ui-avatars.com/api/?name=${channelName.replace(" ", "+")}&background=random&color=fff&size=512"

                // Encode name and poster in the URL to pass it to the load method
                val encodedData = Base64.encodeToString(channelName.toByteArray(), Base64.NO_WRAP) + "||" + Base64.encodeToString(poster.toByteArray(), Base64.NO_WRAP)
                
                plugin.newMovieSearchResponse(
                    channelName,
                    "livetv/$channelId||$encodedData",
                    TvType.Movie
                ) {
                    this.posterUrl = poster
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
        channelIdWithData: String,
        headers: Map<String, String>,
        mainUrl: String
    ): LoadResponse? {
        val parts = channelIdWithData.split("||")
        val channelId = parts[0]
        
        val passedName = parts.getOrNull(1)?.let { String(Base64.decode(it, Base64.NO_WRAP)) }
        val passedPoster = parts.getOrNull(2)?.let { String(Base64.decode(it, Base64.NO_WRAP)) }

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

            val fallbackName = channelId
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }

            val channelName = passedName ?: fallbackName

            val description = if (serverCount > 0) {
                "\uD83D\uDCE1 $serverCount source(s) disponible(s) — Chaîne en direct"
            } else {
                "\uD83D\uDCE1 Chaîne en direct"
            }

            plugin.newMovieLoadResponse(
                channelName,
                "livetv/$channelIdWithData",
                TvType.Movie,
                "livetv/$channelIdWithData" // url is required for dataUrl argument
            ) {
                this.plot = description
                this.posterUrl = passedPoster ?: "https://ui-avatars.com/api/?name=${channelName.replace(" ", "+")}&background=random"
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
        channelIdWithData: String,
        headers: Map<String, String>,
        mainUrl: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val channelId = channelIdWithData.split("||")[0]
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
