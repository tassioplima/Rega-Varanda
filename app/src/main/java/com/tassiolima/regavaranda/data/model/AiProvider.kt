package com.tassiolima.regavaranda.data.model

enum class AiProvider(val label: String, val keyHint: String, val consoleUrl: String) {
    ANTHROPIC(
        label = "Anthropic (Claude)",
        keyHint = "Chave da API Anthropic",
        consoleUrl = "console.anthropic.com"
    ),
    GEMINI(
        label = "Google (Gemini)",
        keyHint = "Chave da API Google Gemini",
        consoleUrl = "aistudio.google.com/apikey"
    )
}
