package com.example.api

import android.util.Base64
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GmailRest {

    const val REDIRECT_URI = "com.example.jaggedgem:/oauth2redirect"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun newVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun authUrl(clientId: String, verifier: String): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "https://accounts.google.com/o/oauth2/v2/auth" +
            "?response_type=code" +
            "&access_type=offline" +
            "&prompt=consent" +
            "&client_id=" + enc(clientId) +
            "&redirect_uri=" + enc(REDIRECT_URI) +
            "&scope=" + enc("openid email profile https://mail.google.com/") +
            "&code_challenge=" + enc(challenge) +
            "&code_challenge_method=S256"
    }

    class GoogleTokens(val accessToken: String, val refreshToken: String?, val expiresInSeconds: Long)

    fun exchangeCode(clientId: String, clientSecret: String, code: String, verifier: String): GoogleTokens {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code_verifier", verifier)
            .add("redirect_uri", REDIRECT_URI)
            .build()
        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Google token error ${response.code}: ${responseBody.take(300)}")
            val json = JSONObject(responseBody)
            return GoogleTokens(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").ifBlank { null },
                expiresInSeconds = json.optLong("expires_in", 3600)
            )
        }
    }

    fun refreshAccessToken(clientId: String, clientSecret: String, refreshToken: String): String {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .build()
        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Google refresh error ${response.code}: ${responseBody.take(300)}")
            return JSONObject(responseBody).getString("access_token")
        }
    }

    fun fetchGmailAddress(accessToken: String): String {
        val request = Request.Builder()
            .url("https://gmail.googleapis.com/gmail/v1/users/me/profile")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Gmail profile error ${response.code}: ${responseBody.take(300)}")
            return JSONObject(responseBody).optString("emailAddress")
        }
    }

    fun loadThreads(accessToken: String, max: Int = 25): List<EmailInbox.EmailThread> {
        val listRequest = Request.Builder()
            .url("https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=$max")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val ids = mutableListOf<String>()
        client.newCall(listRequest).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Gmail list error ${response.code}")
            val messages = JSONObject(response.body?.string().orEmpty()).optJSONArray("messages") ?: return emptyList()
            for (i in 0 until messages.length()) {
                ids.add(messages.getJSONObject(i).getString("id"))
            }
        }
        val threads = mutableListOf<EmailInbox.EmailThread>()
        val fmt = java.text.SimpleDateFormat("d MMM yyyy HH:mm", java.util.Locale.getDefault())
        val mailFmt = javax.mail.internet.MailDateFormat()
        for (id in ids) {
            try {
                val msg = JSONObject(fetchMessage(accessToken, id))
                val payload = msg.optJSONObject("payload") ?: JSONObject()
                val snippet = msg.optString("snippet").take(90)
                val headers = payload.optJSONArray("headers") ?: org.json.JSONArray()
                var fromName = ""
                var fromAddress = ""
                var subject = "(no subject)"
                var sentDate = 0L
                for (i in 0 until headers.length()) {
                    val h = headers.getJSONObject(i)
                    when (h.optString("name").lowercase()) {
                        "from" -> {
                            val value = h.optString("value")
                            val m = Regex("(.*)<(.+)>").find(value)
                            if (m != null) {
                                fromName = m.groupValues[1].trim(' ', '"', '\'').ifBlank { m.groupValues[2].trim() }
                                fromAddress = m.groupValues[2].trim()
                            } else {
                                fromName = value.trim(); fromAddress = value.trim()
                            }
                        }
                        "subject" -> subject = h.optString("value").ifBlank { "(no subject)" }
                        "date" -> sentDate = try { mailFmt.parse(h.optString("value"))?.time ?: 0L } catch (e: Exception) { 0L }
                    }
                }
                val body = decodeBody(payload)
                val dateText = if (sentDate > 0) fmt.format(java.util.Date(sentDate)) else ""
                threads.add(
                    EmailInbox.EmailThread(
                        from = fromName.ifBlank { "Unknown sender" },
                        fromAddress = fromAddress,
                        subject = subject,
                        sentDate = sentDate,
                        snippet = snippet.ifBlank { body.replace(Regex("\\s+"), " ").take(90) },
                        text = "Email from $fromName <$fromAddress>\nSubject: $subject\nDate: $dateText\n\n${body.trim().take(12000)}"
                    )
                )
            } catch (e: Exception) {
            }
        }
        return threads
    }

    private fun fetchMessage(accessToken: String, id: String): String {
        val request = Request.Builder()
            .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=full")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Gmail message error ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun decodeBody(payload: JSONObject): String {
        val mime = payload.optString("mimeType")
        if (mime.startsWith("text/plain")) {
            return decodeB64(payload.optJSONObject("body")?.optString("data") ?: "")
        }
        val parts = payload.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (p.optString("mimeType").startsWith("text/plain")) {
                sb.append(decodeBody(p))
            }
        }
        if (sb.isBlank()) {
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (p.optString("mimeType").startsWith("multipart")) {
                    sb.append(decodeBody(p))
                }
            }
        }
        return sb.toString()
    }

    private fun decodeB64(data: String): String {
        if (data.isBlank()) return ""
        return try {
            String(Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
