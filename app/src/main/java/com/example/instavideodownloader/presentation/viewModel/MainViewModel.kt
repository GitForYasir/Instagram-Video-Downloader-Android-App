package com.example.instavideodownloader.presentation.viewModel

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.example.instavideodownloader.abstraction.DownloadUiStates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainViewModel : ViewModel() {

    companion object {
        const val BASE_URL =
            "https://script.google.com/macros/s/AKfycbyHQ9twIO2CKf4TFsPDHrB2JARDK66yVDlUrmPukDefHnWsAAnRuFjE8DR1R-qU-nC9/"
    }


    private val _uiStates = MutableStateFlow(DownloadUiStates())
    val uiStates: StateFlow<DownloadUiStates> = _uiStates.asStateFlow()

    suspend fun downloadVideo(videoUrl: String, context: Context) {
        try {
            _uiStates.value = DownloadUiStates(isLoading = true)

            if (!isValidInstaUrl(videoUrl)) {
                _uiStates.value = DownloadUiStates(
                    message = "Please paste a valid url.",
                    error = true
                )
            }

            // get video download url from api
            val craftedUrl = getVideoDownloadUrl(videoUrl)
            if (craftedUrl != null) {

                // download start using download manager
                startDownload(craftedUrl, context)
                _uiStates.value = DownloadUiStates(
                    message = "Download started!, check your notification",
                    error = false
                )
            } else {
                _uiStates.value = DownloadUiStates(
                    message = "Failed to get download URL. Please check the Instagram URL and try again.",
                    error = true
                )
            }
        } catch (e: Exception) {
            _uiStates.value = DownloadUiStates(
                message = "${e.message}: unknown error",
                error = true
            )
        }
    }

    private fun isValidInstaUrl(url: String): Boolean {
        return url.contains("instagram.com") && (url.contains("/p/")) || url.contains("/reel/") || url.contains(
            "/tv/"
        )
    }


    private suspend fun getVideoDownloadUrl(instagramUrl: String): String? {

        return withContext(Dispatchers.IO) {
            try {
                val encodeUrl = URLEncoder.encode(instagramUrl, "UTF-8")
                val apiUrl = "${BASE_URL}exec?u=$encodeUrl"
                val url = URL(apiUrl)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30000
                connection.readTimeout = 30000


                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    return@withContext parseDownloadUrl(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("instadown", "getVideoDownloadUrl Exception: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseDownloadUrl(response: String): String? {
        return try {

            val jsonObject = JSONObject(response)

            when {
                jsonObject.has("downloadUrl") -> jsonObject.getString("downloadUrl")
                jsonObject.has("url") -> jsonObject.getString("url")
                jsonObject.has("link") -> jsonObject.getString("link")
                jsonObject.has("sd") -> jsonObject.getString("sd")
                jsonObject.has("hd") -> jsonObject.getString("hd")
                jsonObject.has("data") -> {
                    when (val data = jsonObject.get("data")) {
                        is JSONObject -> data.optString("url", null)
                        is JSONArray -> {
                            if (data.length() > 0) {
                                (data.get(0) as? JSONObject)?.optString("url", null)
                            } else null
                        }

                        else -> null
                    }
                }

                else -> null
            }

        } catch (e: Exception) {
            Log.e("instadown", "parseDownloadUrl Exception: ${e.message}")
            null
        }
    }


    private fun startDownload(downloadUrl: String, context: Context) {
        try {
            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(downloadUrl.toUri())

            // Set download destination
            val filename = "instagram_video_${System.currentTimeMillis()}.mp4"
            /*request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)*/

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "instagram_video_${System.currentTimeMillis()}.mp4")
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    request.setDestinationUri(uri)
                }
            } else {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }


            // Set request properties
            request.setTitle("Instagram Video Download")
            request.setDescription("Downloading Instagram video...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)

            // Add headers
            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")

            // Start download
            downloadManager.enqueue(request)

        } catch (e: Exception) {

        }
    }

    suspend fun getPreviewUrl(instaVideoUrl: String, callBack: (String?) -> Unit) {
        try {
            if (!isValidInstaUrl(instaVideoUrl)) {
                callBack(null)
            }
            val videoUrl = getVideoDownloadUrl(instaVideoUrl)
            callBack(videoUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            callBack(null)
        }
    }


}