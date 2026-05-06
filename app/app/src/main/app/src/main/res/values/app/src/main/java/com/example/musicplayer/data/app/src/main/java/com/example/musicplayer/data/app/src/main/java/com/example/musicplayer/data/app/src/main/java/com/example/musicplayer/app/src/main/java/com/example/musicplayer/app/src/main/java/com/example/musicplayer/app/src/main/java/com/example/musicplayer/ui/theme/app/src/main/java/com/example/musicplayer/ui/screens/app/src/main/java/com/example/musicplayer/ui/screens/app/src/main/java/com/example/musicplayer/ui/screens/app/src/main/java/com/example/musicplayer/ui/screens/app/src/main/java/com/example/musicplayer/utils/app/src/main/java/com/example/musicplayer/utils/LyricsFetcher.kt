package com.example.musicplayer.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class LyricsFetcher {
    private val client = OkHttpClient()

    suspend fun fetchLyrics(title: String, artist: String): String? {
        return try {
            val url = "https://lrclib.net/api/get?" +
                    "artist_name=${java.net.URLEncoder.encode(artist, "UTF-8")}" +
                    "&track_name=${java.net.URLEncoder.encode(title, "UTF-8")}"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: return null)
                json.optString("syncedLyrics", null) ?: json.optString("plainLyrics", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
