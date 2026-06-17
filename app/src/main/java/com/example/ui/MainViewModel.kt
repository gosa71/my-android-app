package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FamilyDatabase
import com.example.data.FamilyRoles
import com.example.data.FirebaseClientFactory
import com.example.data.FirebaseDatabaseApi
import com.example.data.FirebaseMessage
import com.example.data.GROUP_CONVERSATION_ID
import com.example.data.MessageEntity
import com.example.data.dmConversationId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FamilyDatabase.getDatabase(application)
    private val dao = db.dao

    // --- Identity (who am I in the family) ---
    var isAuthenticated by mutableStateOf(false)
        private set
    var currentUserRole by mutableStateOf("MAMA")
        private set
    var currentUserName by mutableStateOf("")
        private set

    // Login form state
    var selectedRole by mutableStateOf(FamilyRoles.all.first().id)
    var displayNameInput by mutableStateOf("")

    // --- Cloud sync (optional Firebase Realtime Database) ---
    // Leave the URL blank to stay fully local/offline. Point it at your own
    // Firebase project to get real cross-device messaging.
    var firebaseDatabaseUrl by mutableStateOf("https://jijiku-da618-default-rtdb.firebaseio.com/")
    var isCloudSyncEnabled by mutableStateOf(false)
        private set
    var syncStatusText by mutableStateOf("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0440\u0435\u0436\u0438\u043c (\u043e\u0444\u043b\u0430\u0439\u043d)")
        private set
    private var dbApi: FirebaseDatabaseApi? = null

    // --- Messages (all conversations) ---
    val allMessages: StateFlow<List<MessageEntity>> = dao.getAllMessagesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // --- Active conversation / composer ---
    var activeConversationId by mutableStateOf<String?>(null)
        private set
    var activeConversationTitle by mutableStateOf("")
        private set
    var activeConversationIsGroup by mutableStateOf(false)
        private set
    var newMessageText by mutableStateOf("")

    init {
        viewModelScope.launch {
            configureCloud(firebaseDatabaseUrl)
            startSyncPoller()
        }
    }

    // ---------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------
    fun login() {
        val role = selectedRole
        currentUserRole = role
        currentUserName = displayNameInput.trim().ifBlank { FamilyRoles.byId(role).defaultName }
        isAuthenticated = true
    }

    fun logout() {
        isAuthenticated = false
        activeConversationId = null
        newMessageText = ""
    }

    // ---------------------------------------------------------------
    // Conversation navigation
    // ---------------------------------------------------------------
    fun openGroupConversation() {
        activeConversationId = GROUP_CONVERSATION_ID
        activeConversationTitle = "\u0421\u0435\u043c\u044c\u044f (\u043e\u0431\u0449\u0438\u0439 \u0447\u0430\u0442)"
        activeConversationIsGroup = true
        newMessageText = ""
    }

    fun openDirectConversation(otherRole: String) {
        activeConversationId = dmConversationId(currentUserRole, otherRole)
        activeConversationTitle = FamilyRoles.byId(otherRole).defaultName
        activeConversationIsGroup = false
        newMessageText = ""
    }

    fun closeConversation() {
        activeConversationId = null
        newMessageText = ""
    }

    fun messagesFor(conversationId: String, all: List<MessageEntity>): List<MessageEntity> =
        all.filter { it.conversationId == conversationId }

    // ---------------------------------------------------------------
    // Sending
    // ---------------------------------------------------------------
    fun sendMessage() {
        val convId = activeConversationId ?: return
        val text = newMessageText.trim()
        if (text.isEmpty()) return

        val message = MessageEntity(
            id = generateMessageId(),
            conversationId = convId,
            senderId = currentUserRole,
            senderName = currentUserName,
            senderRole = currentUserRole,
            text = text,
        )
        newMessageText = ""

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMessage(message)
            dbApi?.let { api ->
                try {
                    api.putMessage(convId, message.id, null, message.toFirebase())
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to sync message to cloud", e)
                }
            }
        }
    }

    fun clearActiveConversation() {
        val convId = activeConversationId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearConversation(convId)
        }
    }

    private fun generateMessageId(): String =
        "${currentUserRole}_${System.currentTimeMillis()}_${Random.nextInt(1_000_000)}"

    private fun MessageEntity.toFirebase() = FirebaseMessage(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        senderRole = senderRole,
        text = text,
        timestamp = timestamp,
    )

    // ---------------------------------------------------------------
    // Cloud sync
    // ---------------------------------------------------------------
    fun configureCloud(dbUrl: String) {
        firebaseDatabaseUrl = dbUrl
        if (dbUrl.isBlank()) {
            dbApi = null
            isCloudSyncEnabled = false
            syncStatusText = "\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0440\u0435\u0436\u0438\u043c (\u043e\u0444\u043b\u0430\u0439\u043d)"
            return
        }
        dbApi = FirebaseClientFactory.createDatabaseApi(dbUrl)
        isCloudSyncEnabled = dbApi != null
        syncStatusText = if (isCloudSyncEnabled) {
            "\u041e\u0431\u043b\u0430\u0447\u043d\u0430\u044f \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0430\u0446\u0438\u044f \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u0430"
        } else {
            "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f"
        }
    }

    fun disableCloud() {
        dbApi = null
        isCloudSyncEnabled = false
        syncStatusText = "\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0440\u0435\u0436\u0438\u043c (\u043e\u0444\u043b\u0430\u0439\u043d)"
    }

    private fun startSyncPoller() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5000)
                val api = dbApi ?: continue
                if (!isCloudSyncEnabled) continue
                try {
                    val tree = api.getAllMessages(null) ?: continue
                    tree.forEach { (conversationId, messages) ->
                        messages.forEach { (msgId, fbMsg) ->
                            val entity = MessageEntity(
                                id = fbMsg.id.ifBlank { msgId },
                                conversationId = fbMsg.conversationId.ifBlank { conversationId },
                                senderId = fbMsg.senderId,
                                senderName = fbMsg.senderName,
                                senderRole = fbMsg.senderRole,
                                text = fbMsg.text,
                                timestamp = fbMsg.timestamp,
                            )
                            dao.insertMessage(entity)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Periodic sync failed", e)
                }
            }
        }
    }
}
