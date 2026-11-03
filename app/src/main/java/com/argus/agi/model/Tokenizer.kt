package com.argus.agi.model

import java.util.concurrent.ConcurrentHashMap

class Tokenizer {

    companion object {
        const val PAD_TOKEN = "<PAD>"
        const val UNK_TOKEN = "<UNK>"
        const val BOS_TOKEN = "<BOS>"
        const val EOS_TOKEN = "<EOS>"
        const val SYSTEM_TOKEN = "<SYSTEM>"
        const val USER_TOKEN = "<USER>"
        const val ASSISTANT_TOKEN = "<ASSISTANT>"
        const val THINK_START = "<THINK>"
        const val THINK_END = "</THINK>"

        const val PAD_ID = 0
        const val UNK_ID = 1
        const val BOS_ID = 2
        const val EOS_ID = 3
        const val SYSTEM_ID = 4
        const val USER_ID = 5
        const val ASSISTANT_ID = 6
        const val THINK_START_ID = 7
        const val THINK_END_ID = 8
    }

    private val tokenToId = ConcurrentHashMap<String, Int>()
    private val idToToken = ConcurrentHashMap<Int, String>()

    init {
        // Initialize base special tokens
        registerToken(PAD_TOKEN, PAD_ID)
        registerToken(UNK_TOKEN, UNK_ID)
        registerToken(BOS_TOKEN, BOS_ID)
        registerToken(EOS_TOKEN, EOS_ID)
        registerToken(SYSTEM_TOKEN, SYSTEM_ID)
        registerToken(USER_TOKEN, USER_ID)
        registerToken(ASSISTANT_TOKEN, ASSISTANT_ID)
        registerToken(THINK_START, THINK_START_ID)
        registerToken(THINK_END, THINK_END_ID)

        // Seed basic printable ASCII characters (letters, digits, punctuation, space, newline)
        var nextId = 9
        val baseChars = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,!?:;-_+*=/\\@#$%^&()[]{}|<>\"'~\n\r\t"
        for (c in baseChars) {
            val s = c.toString()
            if (!tokenToId.containsKey(s)) {
                registerToken(s, nextId++)
            }
        }

        // Seed common frequent word fragments for Indonesian & English
        val seedWords = listOf(
            "yang", "dan", "di", "ini", "itu", "ke", "dari", "untuk", "pada", "adalah", "dengan",
            "saya", "kamu", "bisa", "ada", "tidak", "apa", "bagaimana", "argus", "agi", "model",
            "training", "data", "layar", "kamera", "the", "and", "is", "in", "to", "of", "for"
        )
        for (w in seedWords) {
            if (!tokenToId.containsKey(w)) {
                registerToken(w, nextId++)
            }
        }
    }

    val vocabSize: Int
        get() = tokenToId.size

    @Synchronized
    fun registerToken(token: String, id: Int = tokenToId.size): Int {
        val existing = tokenToId[token]
        if (existing != null) return existing
        tokenToId[token] = id
        idToToken[id] = token
        return id
    }

    /**
     * Splits text into subwords/characters and encodes to token IDs.
     * Expands vocabulary dynamically if autoRegister is true.
     */
    fun encode(text: String, autoRegister: Boolean = true): List<Int> {
        val result = mutableListOf<Int>()
        var i = 0
        val n = text.length

        while (i < n) {
            // Check for special tags first
            val remaining = text.substring(i)
            val specialMatch = listOf(
                THINK_START, THINK_END, SYSTEM_TOKEN, USER_TOKEN, ASSISTANT_TOKEN, BOS_TOKEN, EOS_TOKEN
            ).firstOrNull { remaining.startsWith(it) }

            if (specialMatch != null) {
                result.add(tokenToId[specialMatch] ?: UNK_ID)
                i += specialMatch.length
                continue
            }

            // Greedy matching against known subwords/words
            var matched = false
            val maxLen = minOf(16, n - i)
            for (len in maxLen downTo 2) {
                val candidate = text.substring(i, i + len)
                val id = tokenToId[candidate]
                if (id != null) {
                    result.add(id)
                    i += len
                    matched = true
                    break
                }
            }

            if (!matched) {
                val charStr = text[i].toString()
                var id = tokenToId[charStr]
                if (id == null) {
                    id = if (autoRegister) {
                        registerToken(charStr)
                    } else {
                        UNK_ID
                    }
                }
                result.add(id)
                i++
            }
        }
        return result
    }

    fun decode(tokenIds: List<Int>): String {
        val sb = StringBuilder()
        for (id in tokenIds) {
            val token = idToToken[id] ?: ""
            if (token != PAD_TOKEN && token != BOS_TOKEN && token != EOS_TOKEN) {
                sb.append(token)
            }
        }
        return sb.toString()
    }
}
