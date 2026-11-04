package com.argus.agi.feature.chat.presentation

import android.net.Uri
import java.util.UUID

/**
 * A message rendered in the chat feature.
 *
 * This is presentation state only; it does not represent persisted conversation history.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val thinkReasoning: String? = null,
    val attachedFileName: String? = null,
    val attachedImageUri: Uri? = null,
    val timestamp: Long = System.currentTimeMillis()
)
