package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/**
 * Optional cloud sync over Firebase Realtime Database (REST).
 * Messages are stored grouped by conversation:
 *
 *   messages/{conversationId}/{messageId} = FirebaseMessage
 *
 * This keeps every 1:1 chat and the family group chat in a separate branch,
 * so devices only ever merge messages into the right conversation.
 */
object FirebaseClientFactory {
    fun createDatabaseApi(customBaseUrl: String): FirebaseDatabaseApi? {
        val baseUrl = when {
            customBaseUrl.isBlank() -> return null
            customBaseUrl.startsWith("http") ->
                if (customBaseUrl.endsWith("/")) customBaseUrl else "$customBaseUrl/"
            else -> "https://$customBaseUrl.firebaseio.com/"
        }

        return try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(FirebaseDatabaseApi::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseClientFactory", "Failed to create database client from URL: $customBaseUrl", e)
            null
        }
    }
}

interface FirebaseDatabaseApi {
    /** Returns the whole message tree: { conversationId -> { messageId -> message } }. */
    @GET("messages.json")
    suspend fun getAllMessages(
        @Query("auth") auth: String?,
    ): Map<String, Map<String, FirebaseMessage>>?

    /** Upserts a single message into its conversation branch. */
    @PUT("messages/{conversationId}/{messageId}.json")
    suspend fun putMessage(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String,
        @Query("auth") auth: String?,
        @Body message: FirebaseMessage,
    ): FirebaseMessage
}

/** Network representation of a chat message. Defaults make all fields optional when parsing. */
data class FirebaseMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
)
