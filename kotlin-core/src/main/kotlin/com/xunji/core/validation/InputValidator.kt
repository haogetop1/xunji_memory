package com.xunji.core.validation

class FieldValidationException(val field: String, message: String) : IllegalArgumentException(message)

object InputValidator {
    fun deckName(raw: String): String {
        val name = raw.trim()
        check("name", name, 80, required = true)
        return name
    }

    fun cardContent(front: String, back: String, example: String) {
        check("front", front, 2_000, required = true)
        check("back", back, 10_000, required = true)
        check("example", example, 4_000, required = false)
    }

    private fun check(field: String, text: String, limit: Int, required: Boolean) {
        if (required && text.isBlank()) throw FieldValidationException(field, "请填写此项")
        if (text.codePointCount(0, text.length) > limit) {
            throw FieldValidationException(field, "最多填写 $limit 个字符")
        }
    }
}
