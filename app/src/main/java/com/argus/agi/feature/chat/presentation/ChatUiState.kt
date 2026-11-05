package com.argus.agi.feature.chat.presentation

/**
 * Immutable presentation state for the Chat feature.
 *
 * This state represents the currently displayed conversation projection,
 * not durable conversation history or model state.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false
)
