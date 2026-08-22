package com.lookbuy.app.presentation.viewmodels

/** Estados de conversa do LookBuy; distintos do estado técnico da sessão DAT. */
enum class AssistantState {
    INACTIVE,
    LISTENING_FOR_WAKE_WORD,
    LISTENING_FOR_COMMAND,
    PROCESSING,
    AWAITING_CLARIFICATION,
    SPEAKING,
}
