package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FamilyRoles
import com.example.data.GROUP_CONVERSATION_ID
import com.example.data.MessageEntity
import com.example.data.dmConversationId
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: MainViewModel = viewModel()
                    when {
                        !viewModel.isAuthenticated -> LoginScreen(viewModel)
                        viewModel.activeConversationId == null -> ChatListScreen(viewModel)
                        else -> ChatScreen(viewModel)
                    }
                }
            }
        }
    }
}

// ============================================================
// Helpers
// ============================================================
private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

private fun avatarColorForRole(role: String): Color {
    if (role == GROUP_CONVERSATION_ID) return BrandBlueLight
    val idx = FamilyRoles.indexOf(role)
    return AvatarColors[idx % AvatarColors.size]
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

@Composable
private fun Avatar(label: String, color: Color, size: Int = 48) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size / 2.4).sp,
        )
    }
}

// ============================================================
// 1. Login
// ============================================================
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "\u0421\u0435\u043c\u0435\u0439\u043d\u044b\u0439 \u0447\u0430\u0442",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u0432\u043e\u0439 \u043f\u0440\u043e\u0444\u0438\u043b\u044c",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FamilyRoles.all.forEach { role ->
                val selected = viewModel.selectedRole == role.id
                Surface(
                    onClick = {
                        viewModel.selectedRole = role.id
                        viewModel.displayNameInput = role.defaultName
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Avatar(initialsOf(role.defaultName), avatarColorForRole(role.id), size = 40)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = role.defaultName,
                            fontSize = 16.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.displayNameInput,
            onValueChange = { viewModel.displayNameInput = it },
            label = { Text("\u0418\u043c\u044f") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.login() },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("\u0412\u043e\u0439\u0442\u0438", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================================
// 2. Chat list
// ============================================================
private data class ConversationPreview(
    val id: String,
    val title: String,
    val isGroup: Boolean,
    val avatarKey: String,
    val lastText: String?,
    val lastTimestamp: Long?,
    val lastSenderName: String?,
)

private fun buildConversations(
    currentRole: String,
    messages: List<MessageEntity>,
): List<ConversationPreview> {
    val list = mutableListOf<ConversationPreview>()

    val groupMsgs = messages.filter { it.conversationId == GROUP_CONVERSATION_ID }
    val lastGroup = groupMsgs.lastOrNull()
    list += ConversationPreview(
        id = GROUP_CONVERSATION_ID,
        title = "\u0421\u0435\u043c\u044c\u044f (\u043e\u0431\u0449\u0438\u0439 \u0447\u0430\u0442)",
        isGroup = true,
        avatarKey = GROUP_CONVERSATION_ID,
        lastText = lastGroup?.text,
        lastTimestamp = lastGroup?.timestamp,
        lastSenderName = lastGroup?.senderName,
    )

    FamilyRoles.all.forEach { role ->
        if (role.id == currentRole) return@forEach
        val convId = dmConversationId(currentRole, role.id)
        val msgs = messages.filter { it.conversationId == convId }
        val last = msgs.lastOrNull()
        list += ConversationPreview(
            id = convId,
            title = role.defaultName,
            isGroup = false,
            avatarKey = role.id,
            lastText = last?.text,
            lastTimestamp = last?.timestamp,
            lastSenderName = last?.senderName,
        )
    }

    // Group chat pinned first, then by most recent activity.
    return list.sortedWith(
        compareByDescending<ConversationPreview> { it.isGroup }
            .thenByDescending { it.lastTimestamp ?: 0L },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: MainViewModel) {
    val messages by viewModel.allMessages.collectAsStateWithLifecycle()
    val conversations = buildConversations(viewModel.currentUserRole, messages)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("\u0427\u0430\u0442\u044b", fontWeight = FontWeight.Bold)
                        Text(
                            text = "\u0412\u044b: ${viewModel.currentUserName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "\u0412\u044b\u0439\u0442\u0438")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(conversations, key = { it.id }) { conv ->
                ConversationRow(
                    conv = conv,
                    currentUserName = viewModel.currentUserName,
                    onClick = {
                        if (conv.isGroup) viewModel.openGroupConversation()
                        else viewModel.openDirectConversation(conv.avatarKey)
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conv: ConversationPreview,
    currentUserName: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (conv.isGroup) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColorForRole(conv.avatarKey)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = Color.White)
            }
        } else {
            Avatar(initialsOf(conv.title), avatarColorForRole(conv.avatarKey))
        }

        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conv.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val preview = conv.lastText?.let { text ->
                if (conv.isGroup && conv.lastSenderName != null) {
                    val who = if (conv.lastSenderName == currentUserName) "\u0412\u044b" else conv.lastSenderName
                    "$who: $text"
                } else {
                    text
                }
            } ?: "\u041d\u0435\u0442 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439"
            Text(
                text = preview,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        conv.lastTimestamp?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTime(it),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ============================================================
// 3. Conversation
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val convId = viewModel.activeConversationId ?: return
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val messages = remember(allMessages, convId) { allMessages.filter { it.conversationId == convId } }
    val isGroup = viewModel.activeConversationIsGroup
    val dark = isSystemInDarkTheme()
    val listState = rememberLazyListState()

    BackHandler { viewModel.closeConversation() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGroup) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(avatarColorForRole(GROUP_CONVERSATION_ID)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Group,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else {
                            Avatar(initialsOf(viewModel.activeConversationTitle), avatarColorForRole(otherRoleOf(convId, viewModel.currentUserRole)), size = 36)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(viewModel.activeConversationTitle, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeConversation() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u041d\u0430\u0437\u0430\u0434")
                    }
                },
            )
        },
        bottomBar = {
            MessageComposer(
                value = viewModel.newMessageText,
                onValueChange = { viewModel.newMessageText = it },
                onSend = { viewModel.sendMessage() },
            )
        },
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u041d\u0430\u043f\u0438\u0448\u0438\u0442\u0435 \u043f\u0435\u0440\u0432\u043e\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        isMine = msg.senderRole == viewModel.currentUserRole,
                        showSenderName = isGroup && msg.senderRole != viewModel.currentUserRole,
                        dark = dark,
                    )
                }
            }
        }
    }
}

private fun otherRoleOf(convId: String, currentRole: String): String {
    // convId format: dm_ROLEA_ROLEB
    val parts = convId.removePrefix("dm_").split("_")
    return parts.firstOrNull { it != currentRole } ?: parts.firstOrNull() ?: currentRole
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    isMine: Boolean,
    showSenderName: Boolean,
    dark: Boolean,
) {
    val bubbleColor = if (isMine) {
        if (dark) OutgoingBubbleDark else OutgoingBubbleLight
    } else {
        if (dark) IncomingBubbleDark else IncomingBubbleLight
    }
    val textColor = if (isMine) {
        if (dark) OutgoingTextDark else OutgoingTextLight
    } else {
        if (dark) IncomingTextDark else IncomingTextLight
    }
    val metaColor = textColor.copy(alpha = 0.7f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (showSenderName) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = avatarColorForRole(message.senderRole),
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(text = message.text, color = textColor, fontSize = 15.sp)
                Text(
                    text = formatTime(message.timestamp),
                    fontSize = 10.sp,
                    color = metaColor,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435") },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions.Default,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                maxLines = 5,
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c")
            }
        }
    }
}
