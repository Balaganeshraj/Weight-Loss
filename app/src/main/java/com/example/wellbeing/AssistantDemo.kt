package com.example.wellbeing

/** Deliberately limited, deterministic demo. Never sends text to a service or reads the journal. */
object AssistantDemo {
    enum class Topic { PLAN, SLEEP, FOOD, MOVE, MEDICAL, URGENT, GENERAL }
    fun topic(message: String): Topic {
        val text = message.lowercase(java.util.Locale.ROOT)
        fun has(vararg terms: String) = terms.any { text.contains(it) }
        return when {
            has("emergency", "chest pain", "suicid", "kill myself", "can't breathe", "cannot breathe", "overdose", "அவசர", "மார்பு வலி", "தற்கொலை", "மூச்சு") -> Topic.URGENT
            has("medicin", "medicat", "dose", "diagnos", "treatment", "pregnan", "diabet", "pain", "symptom", "மருந்த", "நோய்", "சிகிச்சை", "வலி", "கர்ப்ப") -> Topic.MEDICAL
            has("sleep", "rest", "தூக்க", "ஓய்வ") -> Topic.SLEEP
            has("food", "meal", "diet", "உணவ") -> Topic.FOOD
            has("walk", "move", "exercise", "workout", "உடற்பயிற்சி", "நடக்க") -> Topic.MOVE
            has("plan", "task", "focus", "திட்ட", "செயல்") -> Topic.PLAN
            else -> Topic.GENERAL
        }
    }
}
