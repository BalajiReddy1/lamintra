package com.lamintra.cli

/**
 * A deliberately minimal JSON parser.
 *
 * WHY THIS EXISTS INSTEAD OF kotlinx.serialization:
 * component.json and .lamintra/config.json are small, flat, fixed-shape
 * documents. Pulling in a serialization library (and its compiler plugin)
 * for that is unnecessary weight - more Gradle dependencies, slower first
 * build, and one more thing that can break on a user's machine with an
 * unusual Gradle setup. This parser is ~100 lines, has zero dependencies,
 * and covers exactly what our manifests need: objects, arrays, strings,
 * booleans. Not a general-purpose JSON library - deliberately not trying
 * to be one.
 */
sealed class JsonValue {
    data class JsonObject(val entries: Map<String, JsonValue>) : JsonValue()
    data class JsonArray(val items: List<JsonValue>) : JsonValue()
    data class JsonString(val value: String) : JsonValue()
    data class JsonBool(val value: Boolean) : JsonValue()
    object JsonNull : JsonValue()

    operator fun get(key: String): JsonValue? =
        (this as? JsonObject)?.entries?.get(key)

    fun asStringOrNull(): String? = (this as? JsonString)?.value

    fun asString(): String =
        asStringOrNull() ?: error("Expected a JSON string but got: $this")

    fun asStringList(): List<String> =
        (this as? JsonArray)?.items?.map { it.asString() }
            ?: error("Expected a JSON array of strings but got: $this")

    fun asBool(default: Boolean = false): Boolean =
        (this as? JsonBool)?.value ?: default
}

object MiniJson {
    fun parse(input: String): JsonValue {
        val parser = Parser(input)
        val result = parser.parseValue()
        parser.skipWhitespace()
        require(parser.pos == input.length) {
            "Unexpected trailing content at position ${parser.pos}"
        }
        return result
    }

    /**
     * How deeply objects and arrays may nest.
     *
     * This parser is recursive descent, so nesting depth is stack depth. Until
     * 2026-08-21 there was no limit, and a document of 50,000 open brackets -
     * from a hostile registry, or simply a corrupted local config.json -
     * produced a `StackOverflowError` and a 1,023-frame Java stack trace in the
     * user's terminal. `StackOverflowError` is an `Error`, not an `Exception`,
     * so nothing in the CLI caught it.
     *
     * 64 is far beyond anything a real manifest needs: the deepest document
     * this tool reads is `.lamintra/config.json`, which nests twice.
     */
    private const val MAX_DEPTH = 64

    private class Parser(val input: String) {
        var pos = 0
        private var depth = 0

        fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) pos++
        }

        fun <T> nested(block: () -> T): T {
            if (++depth > MAX_DEPTH) {
                error(
                    "JSON is nested more than $MAX_DEPTH levels deep at position $pos. " +
                        "This file is almost certainly corrupted rather than merely wrong."
                )
            }
            try {
                return block()
            } finally {
                depth--
            }
        }

        fun expect(char: Char) {
            require(pos < input.length && input[pos] == char) {
                "Expected '$char' at position $pos but found " +
                    (if (pos < input.length) "'${input[pos]}'" else "end of input")
            }
            pos++
        }

        fun parseValue(): JsonValue {
            skipWhitespace()
            return when {
                pos >= input.length -> error("Unexpected end of input")
                input[pos] == '{' -> nested { parseObject() }
                input[pos] == '[' -> nested { parseArray() }
                input[pos] == '"' -> JsonValue.JsonString(parseStringLiteral())
                input.startsWith("true", pos) -> { pos += 4; JsonValue.JsonBool(true) }
                input.startsWith("false", pos) -> { pos += 5; JsonValue.JsonBool(false) }
                input.startsWith("null", pos) -> { pos += 4; JsonValue.JsonNull }
                else -> parseNumberAsString()
            }
        }

        fun parseObject(): JsonValue.JsonObject {
            expect('{')
            val entries = mutableMapOf<String, JsonValue>()
            skipWhitespace()
            if (pos < input.length && input[pos] == '}') { pos++; return JsonValue.JsonObject(entries) }
            while (true) {
                skipWhitespace()
                val key = parseStringLiteral()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                entries[key] = value
                skipWhitespace()
                if (pos < input.length && input[pos] == ',') { pos++; continue }
                break
            }
            skipWhitespace()
            expect('}')
            return JsonValue.JsonObject(entries)
        }

        fun parseArray(): JsonValue.JsonArray {
            expect('[')
            val items = mutableListOf<JsonValue>()
            skipWhitespace()
            if (pos < input.length && input[pos] == ']') { pos++; return JsonValue.JsonArray(items) }
            while (true) {
                items.add(parseValue())
                skipWhitespace()
                if (pos < input.length && input[pos] == ',') { pos++; continue }
                break
            }
            skipWhitespace()
            expect(']')
            return JsonValue.JsonArray(items)
        }

        fun parseStringLiteral(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length && input[pos] != '"') {
                val c = input[pos]
                if (c == '\\' && pos + 1 < input.length) {
                    when (input[pos + 1]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        // \uXXXX fell through to the `else` branch until
                        // 2026-08-21, which appended the literal 'u' and left
                        // the four hex digits to be read as ordinary text - so
                        // "A" became "u0041" rather than "A". Silent, and
                        // it would land mangled characters in an installed
                        // source file rather than failing.
                        'u' -> {
                            val hex = input.substring(
                                (pos + 2).coerceAtMost(input.length),
                                (pos + 6).coerceAtMost(input.length)
                            )
                            require(hex.length == 4 && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                                "Invalid \\u escape at position $pos: expected four hex digits."
                            }
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> sb.append(input[pos + 1])
                    }
                    pos += 2
                } else {
                    sb.append(c)
                    pos++
                }
            }
            expect('"')
            return sb.toString()
        }

        fun parseNumberAsString(): JsonValue.JsonString {
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] in "-+.eE")) pos++
            return JsonValue.JsonString(input.substring(start, pos))
        }
    }
}
