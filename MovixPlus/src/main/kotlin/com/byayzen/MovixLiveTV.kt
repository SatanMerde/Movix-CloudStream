package com.byayzen

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import android.util.Base64

object MovixLiveTV {

    private const val TAG = "MovixLiveTV"

    private val channelLogos = mapOf(
        "TF1" to "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/TF1_logo.svg/512px-TF1_logo.svg.png",
        "France 2" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/France_2_logo_2018.svg/512px-France_2_logo_2018.svg.png",
        "France 3" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/France_3_logo_2018.svg/512px-France_3_logo_2018.svg.png",
        "CANAL+" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Canal%2B_logo.svg/512px-Canal%2B_logo.svg.png",
        "France 5" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/06/France_5_logo_2018.svg/512px-France_5_logo_2018.svg.png",
        "M6" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/M6_logo.svg/512px-M6_logo.svg.png",
        "Arte" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Arte_Logo.svg/512px-Arte_Logo.svg.png",
        "C8" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/C8_logo_2016.svg/512px-C8_logo_2016.svg.png",
        "W9" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/W9_logo_2018.svg/512px-W9_logo_2018.svg.png",
        "TMC" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/TMC_logo_2016.svg/512px-TMC_logo_2016.svg.png",
        "TFX" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/TFX_logo_2018.svg/512px-TFX_logo_2018.svg.png",
        "NRJ 12" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/NRJ12_logo.svg/512px-NRJ12_logo.svg.png",
        "France 4" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/France_4_logo_2018.svg/512px-France_4_logo_2018.svg.png",
        "BFM TV" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/BFMTV_logo_2018.svg/512px-BFMTV_logo_2018.svg.png",
        "CNEWS" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/CNEWS_logo_2017.svg/512px-CNEWS_logo_2017.svg.png",
        "CSTAR" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/CStar_logo_2016.svg/512px-CStar_logo_2016.svg.png",
        "Gulli" to "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Gulli_logo_2017.svg/512px-Gulli_logo_2017.svg.png",
        "L'Équipe" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/L%27%C3%89quipe_2015.svg/512px-L%27%C3%89quipe_2015.svg.png",
        "6ter" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/6ter_logo_2018.svg/512px-6ter_logo_2018.svg.png",
        "RMC Story" to "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/RMC_Story_logo.svg/512px-RMC_Story_logo.svg.png",
        "RMC Découverte" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/RMC_D%C3%A9couverte_logo.svg/512px-RMC_D%C3%A9couverte_logo.svg.png",
        "Cherie 25" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Ch%C3%A9rie_25_logo_2015.svg/512px-Ch%C3%A9rie_25_logo_2015.svg.png",
        "RTL9" to "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/RTL9_logo_2011.svg/512px-RTL9_logo_2011.svg.png",
        "AB1" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/AB1_logo_2015.svg/512px-AB1_logo_2015.svg.png",
        "Action" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Action_logo_2015.svg/512px-Action_logo_2015.svg.png",
        "Paramount Channel" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paramount_Channel_logo.svg/512px-Paramount_Channel_logo.svg.png",
        "Le Figaro TV" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Le_Figaro_TV_logo.svg/512px-Le_Figaro_TV_logo.svg.png",
        
        "CANAL+ Sport" to "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c8/Canal%2B_Sport_logo.svg/512px-Canal%2B_Sport_logo.svg.png",
        "CANAL+ Foot" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/Canal%2B_Foot_logo.svg/512px-Canal%2B_Foot_logo.svg.png",
        "CANAL+ Cinema" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Canal%2B_Cin%C3%A9ma_logo.svg/512px-Canal%2B_Cin%C3%A9ma_logo.svg.png",
        "CANAL+ Grand Ecran" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Canal%2B_Grand_%C3%89cran_logo.svg/512px-Canal%2B_Grand_%C3%89cran_logo.svg.png",
        "CANAL+ Series" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Canal%2B_S%C3%A9ries_logo.svg/512px-Canal%2B_S%C3%A9ries_logo.svg.png",
        "CANAL+ Docs" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/76/Canal%2B_Docs_logo.svg/512px-Canal%2B_Docs_logo.svg.png",
        "CANAL+ Kids" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Canal%2B_Kids_logo.svg/512px-Canal%2B_Kids_logo.svg.png",
        
        "beIN SPORTS 1" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/BeIN_Sports_1_logo.svg/512px-BeIN_Sports_1_logo.svg.png",
        "beIN SPORTS 2" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/BeIN_Sports_2_logo.svg/512px-BeIN_Sports_2_logo.svg.png",
        "beIN SPORTS 3" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7e/BeIN_Sports_3_logo.svg/512px-BeIN_Sports_3_logo.svg.png",
        
        "Eurosport 1" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/36/Eurosport_1_logo.svg/512px-Eurosport_1_logo.svg.png",
        "Eurosport 2" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Eurosport_2_logo.svg/512px-Eurosport_2_logo.svg.png",
        "RMC Sport 1" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/RMC_Sport_1_logo.svg/512px-RMC_Sport_1_logo.svg.png",
        "RMC Sport 2" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/RMC_Sport_2_logo.svg/512px-RMC_Sport_2_logo.svg.png",
        
        "Teva" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/T%C3%A9va_logo_2016.svg/512px-T%C3%A9va_logo_2016.svg.png",
        "Paris Premiere" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6a/Paris_Premi%C3%A8re_logo.svg/512px-Paris_Premi%C3%A8re_logo.svg.png",
        "Disney Channel" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Disney_Channel_logo_2014.svg/512px-Disney_Channel_logo_2014.svg.png",
        "Nickelodeon" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/Nickelodeon_logo.svg/512px-Nickelodeon_logo.svg.png",
        "Cartoon Network" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Cartoon_Network_logo.svg/512px-Cartoon_Network_logo.svg.png"
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
                    ?: channelLogos.entries.firstOrNull { channelName.contains(it.key, ignoreCase = true) }?.value
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
