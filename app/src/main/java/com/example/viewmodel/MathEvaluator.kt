package com.example.viewmodel

import kotlin.math.pow

object MathEvaluator {
    fun evaluateIfExpression(input: String): String? {
        val trimmed = input.trim()
        val expr = when {
            trimmed.startsWith("=") -> trimmed.drop(1).trim()
            trimmed.any { it in "+-*/^" } && trimmed.all { it.isDigit() || it in "+-*/^(). " } -> trimmed
            else -> return null
        }
        if (expr.isBlank()) return null
        val result = runCatching { Parser(expr).parse() }.getOrNull() ?: return null
        if (result.isNaN() || result.isInfinite()) return null
        return if (result % 1.0 == 0.0 && result in -1e12..1e12) {
            result.toLong().toString()
        } else {
            String.format("%.4f", result).trimEnd('0').trimEnd('.')
        }
    }

    private class Parser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: ${ch.toChar()}")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                x = when {
                    eat('+'.code) -> x + parseTerm()
                    eat('-'.code) -> x - parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                x = when {
                    eat('*'.code) -> x * parseFactor()
                    eat('/'.code) -> x / parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw RuntimeException("Missing ')'")
            } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected char")
            }
            if (eat('^'.code)) x = x.pow(parseFactor())
            return x
        }
    }
}
