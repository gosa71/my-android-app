package com.example.data

/**
 * Fixed set of family members. Each member is one "user" of the messenger.
 * You log in as one of them and chat 1:1 with the others (plus a group chat).
 */
data class FamilyRole(
    val id: String,
    val defaultName: String,
)

object FamilyRoles {
    val all: List<FamilyRole> = listOf(
        FamilyRole("MAMA", "\u041c\u0430\u043c\u0430"),
        FamilyRole("PAPA", "\u041f\u0430\u043f\u0430"),
        FamilyRole("SON", "\u0421\u044b\u043d"),
        FamilyRole("DAUGHTER", "\u0414\u043e\u0447\u044c"),
        FamilyRole("GRANDMA", "\u0411\u0430\u0431\u0443\u0448\u043a\u0430"),
        FamilyRole("GRANDPA", "\u0414\u0435\u0434\u0443\u0448\u043a\u0430"),
    )

    fun byId(id: String): FamilyRole = all.firstOrNull { it.id == id } ?: all.first()

    fun indexOf(id: String): Int = all.indexOfFirst { it.id == id }.coerceAtLeast(0)
}

/** Conversation id for the shared family group chat. */
const val GROUP_CONVERSATION_ID = "group_family"

/**
 * Stable conversation id for a 1:1 chat between two members.
 * Sorted so that (A,B) and (B,A) map to the same conversation.
 * Uses only underscores so it is safe as a Firebase Realtime Database key.
 */
fun dmConversationId(roleA: String, roleB: String): String {
    val sorted = listOf(roleA, roleB).sorted()
    return "dm_${sorted[0]}_${sorted[1]}"
}
