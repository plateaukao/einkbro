package info.plateaukao.einkbro.service

import android.util.Log
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent

class InstapaperRepository : KoinComponent {
    
    private val client = OkHttpClient()
    
    suspend fun addUrl(
        url: String,
        username: String,
        password: String,
        title: String? = null
    ): InstapaperResult {
        return withContext(IO) {
            try {
                val formBuilder = FormBody.Builder()
                    .add("url", url)
                
                title?.let { formBuilder.add("title", it) }
                
                val request = Request.Builder()
                    .url(ADD_URL_ENDPOINT)
                    .post(formBuilder.build())
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Authorization", createBasicAuth(username, password))
                    .build()
                
                client.newCall(request).execute().use { response ->
                    when (response.code) {
                        200, 201 -> {
                            Log.d(TAG, "Successfully added URL to Instapaper")
                            InstapaperResult.Success("URL added successfully")
                        }
                        400 -> {
                            Log.e(TAG, "Bad request - invalid URL or parameters")
                            InstapaperResult.Error("Invalid URL or parameters")
                        }
                        403 -> {
                            Log.e(TAG, "Invalid username or password")
                            InstapaperResult.Error("Invalid username or password")
                        }
                        500 -> {
                            Log.e(TAG, "Instapaper service error")
                            InstapaperResult.Error("Service temporarily unavailable")
                        }
                        else -> {
                            Log.e(TAG, "Unexpected response code: ${response.code}")
                            InstapaperResult.Error("Unexpected error occurred")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding URL to Instapaper", e)
                InstapaperResult.Error("Network error: ${e.message}")
            }
        }
    }
    
    suspend fun authenticate(username: String, password: String): InstapaperResult {
        return withContext(IO) {
            try {
                val request = Request.Builder()
                    .url(AUTHENTICATE_ENDPOINT)
                    .post(FormBody.Builder().build())
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Authorization", createBasicAuth(username, password))
                    .build()
                
                client.newCall(request).execute().use { response ->
                    when (response.code) {
                        200 -> {
                            Log.d(TAG, "Authentication successful")
                            InstapaperResult.Success("Authentication successful")
                        }
                        403 -> {
                            Log.e(TAG, "Invalid username or password")
                            InstapaperResult.Error("Invalid username or password")
                        }
                        else -> {
                            Log.e(TAG, "Authentication failed with code: ${response.code}")
                            InstapaperResult.Error("Authentication failed")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during authentication", e)
                InstapaperResult.Error("Network error: ${e.message}")
            }
        }
    }
    
    private fun createBasicAuth(username: String, password: String): String {
        val credentials = "$username:$password"
        val encodedCredentials = android.util.Base64.encodeToString(
            credentials.toByteArray(),
            android.util.Base64.NO_WRAP
        )
        return "Basic $encodedCredentials"
    }
    
    companion object {
        private const val TAG = "InstapaperRepository"
        private const val BASE_URL = "https://www.instapaper.com/api"
        private const val ADD_URL_ENDPOINT = "$BASE_URL/add"
        private const val AUTHENTICATE_ENDPOINT = "$BASE_URL/oauth/access_token"
        private const val USER_AGENT = "EinkBro/1.0"
    }
}

sealed class InstapaperResult {
    data class Success(val message: String) : InstapaperResult()
    data class Error(val message: String) : InstapaperResult()
}