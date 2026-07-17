package info.plateaukao.einkbro.data.remote

import android.net.Uri
import android.util.Base64
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Google Drive backup sync without Play Services: OAuth 2.0 Authorization-Code +
 * PKCE run in EinkBro's own WebView ([info.plateaukao.einkbro.activity.GoogleDriveAuthActivity])
 * plus direct Drive REST calls over OkHttp. The backup zip lives in the user's
 * Drive appDataFolder — a hidden app-private area of their own Drive, so there is
 * no developer-hosted backend and no new dependency.
 */
@Serializable
data class DriveAuthState(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0, // epoch seconds
    val email: String = "",
)

@Serializable
data class DriveFileMeta(val id: String, val name: String, val modifiedTime: String? = null)

/** The stored refresh token was revoked or expired; the user must sign in again. */
class DriveReauthRequiredException : Exception()

class GoogleDriveRepository : KoinComponent {
    private val config: ConfigManager by inject()
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = !BuildConfig.DRIVE_OAUTH_CLIENT_ID.startsWith("REPLACE")

    private var authState: DriveAuthState?
        get() = config.driveAuthStateJson.takeIf { it.isNotEmpty() }
            ?.let { runCatching { json.decodeFromString<DriveAuthState>(it) }.getOrNull() }
        set(value) {
            config.driveAuthStateJson = value?.let { json.encodeToString(DriveAuthState.serializer(), it) } ?: ""
        }

    val email: String? get() = authState?.email?.takeIf { it.isNotEmpty() }

    fun signOut() {
        authState = null
    }

    // MARK: - OAuth (Authorization Code + PKCE)

    /** In-flight interactive sign-in, persisted so the redirect can be completed
     *  from the browser even if the settings screen is long gone. */
    @Serializable
    private data class PendingAuth(val codeVerifier: String, val state: String)

    /** Start an interactive sign-in: remember a fresh verifier/state pair and
     *  return the consent URL to open in the browser. */
    fun beginAuth(): String {
        val pending = PendingAuth(generateRandomToken(), generateRandomToken())
        config.drivePendingAuthJson = json.encodeToString(PendingAuth.serializer(), pending)
        return authorizationUrl(pending.codeVerifier, pending.state)
    }

    /** Complete the custom-scheme redirect intercepted by the WebView: validate
     *  state, exchange the code, persist tokens. */
    suspend fun completeAuth(redirectUri: Uri): Boolean {
        val pending = config.drivePendingAuthJson.takeIf { it.isNotEmpty() }
            ?.let { runCatching { json.decodeFromString<PendingAuth>(it) }.getOrNull() }
            ?: return false
        val code = redirectUri.getQueryParameter("code") ?: return false
        if (redirectUri.getQueryParameter("state") != pending.state) return false
        return exchangeCode(code, pending.codeVerifier).also { success ->
            if (success) config.drivePendingAuthJson = ""
        }
    }

    private fun generateRandomToken(): String = ByteArray(64)
        .also { SecureRandom().nextBytes(it) }
        .let { Base64.encodeToString(it, BASE64_URL_FLAGS) }

    private fun authorizationUrl(codeVerifier: String, state: String): String {
        val challenge = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray())
            .let { Base64.encodeToString(it, BASE64_URL_FLAGS) }
        return AUTH_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", BuildConfig.DRIVE_OAUTH_CLIENT_ID)
            .addQueryParameter("redirect_uri", BuildConfig.DRIVE_OAUTH_REDIRECT)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", "$SCOPE openid email")
            .addQueryParameter("code_challenge", challenge)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("state", state)
            // offline + consent so Google returns a refresh token we can renew
            // silently; select_account so a signed-out user can pick a different
            // account even though the WebView still holds Google session cookies
            .addQueryParameter("access_type", "offline")
            .addQueryParameter("prompt", "consent select_account")
            .build()
            .toString()
    }

    /** Exchange the redirect's auth code for tokens and persist them. */
    private suspend fun exchangeCode(code: String, codeVerifier: String): Boolean = withContext(IO) {
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("code_verifier", codeVerifier)
            .add("client_id", BuildConfig.DRIVE_OAUTH_CLIENT_ID)
            .add("redirect_uri", BuildConfig.DRIVE_OAUTH_REDIRECT)
            .build()
        val request = Request.Builder().url(TOKEN_ENDPOINT).post(form).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext false
            if (!response.isSuccessful) return@withContext false
            val token = runCatching { json.decodeFromString<TokenResponse>(body) }.getOrNull()
                ?: return@withContext false
            if (token.accessToken.isEmpty() || token.refreshToken.isEmpty()) return@withContext false
            authState = DriveAuthState(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                expiresAt = nowSeconds() + token.expiresIn,
                email = token.idToken?.let { emailFromIdToken(it) }.orEmpty(),
            )
            true
        }
    }

    /** A valid access token, refreshed through the stored refresh token when
     *  expired. Throws [DriveReauthRequiredException] (after clearing the dead
     *  session) when the refresh token itself is no longer valid. */
    private suspend fun validAccessToken(): String {
        val state = authState ?: throw DriveReauthRequiredException()
        if (state.expiresAt - nowSeconds() > TOKEN_EXPIRY_MARGIN_SECONDS) {
            return state.accessToken
        }
        return refreshAccessToken()
    }

    private suspend fun refreshAccessToken(): String = withContext(IO) {
        val state = authState ?: throw DriveReauthRequiredException()
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", state.refreshToken)
            .add("client_id", BuildConfig.DRIVE_OAUTH_CLIENT_ID)
            .build()
        val request = Request.Builder().url(TOKEN_ENDPOINT).post(form).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // invalid_grant means the refresh token was revoked or expired —
                // permanent, so drop the session; other failures are transient.
                if (body.contains("invalid_grant")) {
                    signOut()
                    throw DriveReauthRequiredException()
                }
                error("Google token refresh failed: HTTP ${response.code}")
            }
            val token = json.decodeFromString<TokenResponse>(body)
            authState = state.copy(
                accessToken = token.accessToken,
                expiresAt = nowSeconds() + token.expiresIn,
            )
            token.accessToken
        }
    }

    // MARK: - Drive REST (appDataFolder)

    private class DriveHttpException(val code: Int) : Exception("Drive request failed: HTTP $code")

    /** Run [block] with a valid token; on 401 (token revoked server-side while
     *  still locally unexpired) force one refresh and retry. */
    private suspend fun <T> withAccessToken(block: (String) -> T): T = withContext(IO) {
        try {
            block(validAccessToken())
        } catch (e: DriveHttpException) {
            if (e.code != 401) throw e
            block(refreshAccessToken())
        }
    }

    /** Metadata of the backup zip in the appDataFolder, or null when none exists. */
    suspend fun getRemoteBackup(): DriveFileMeta? = withAccessToken { token ->
        val url = "$DRIVE_FILES_ENDPOINT?spaces=appDataFolder&fields=files(id,name,modifiedTime)&pageSize=100"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveHttpException(response.code)
            json.decodeFromString<FileList>(response.body!!.string())
                .files.firstOrNull { it.name == BACKUP_FILE_NAME }
        }
    }

    /** Stream the backup zip into [destination]. */
    suspend fun downloadBackup(fileId: String, destination: File): Unit = withAccessToken { token ->
        val request = Request.Builder()
            .url("$DRIVE_FILES_ENDPOINT/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveHttpException(response.code)
            destination.outputStream().use { response.body!!.byteStream().copyTo(it) }
        }
    }

    /** Create the backup file (when [existingId] is null) or replace its content,
     *  streaming from [file] rather than buffering the zip in memory. */
    suspend fun uploadBackup(file: File, existingId: String?): Unit = withAccessToken { token ->
        val media = file.asRequestBody("application/zip".toMediaType())
        val request = if (existingId == null) {
            val metadata = """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"]}"""
            val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
                .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addPart(media)
                .build()
            Request.Builder()
                .url("$DRIVE_UPLOAD_ENDPOINT?uploadType=multipart")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
        } else {
            Request.Builder()
                .url("$DRIVE_UPLOAD_ENDPOINT/$existingId?uploadType=media")
                .header("Authorization", "Bearer $token")
                .patch(media)
                .build()
        }
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveHttpException(response.code)
        }
    }

    /** Pull the `email` claim out of the id_token JWT for the settings UI. */
    private fun emailFromIdToken(idToken: String): String? = runCatching {
        val payload = idToken.split(".")[1]
        val bytes = Base64.decode(payload, BASE64_URL_FLAGS)
        Json.parseToJsonElement(String(bytes)).jsonObject["email"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    @Serializable
    private data class TokenResponse(
        @SerialName("access_token") val accessToken: String = "",
        @SerialName("refresh_token") val refreshToken: String = "",
        @SerialName("expires_in") val expiresIn: Long = 0,
        @SerialName("id_token") val idToken: String? = null,
    )

    @Serializable
    private data class FileList(val files: List<DriveFileMeta> = emptyList())

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val BACKUP_FILE_NAME = "einkbro-backup.zip"

        private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        private const val DRIVE_FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files"

        private const val TOKEN_EXPIRY_MARGIN_SECONDS = 60L
        private const val BASE64_URL_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    }
}
