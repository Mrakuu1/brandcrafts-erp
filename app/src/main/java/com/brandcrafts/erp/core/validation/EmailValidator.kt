package com.brandcrafts.erp.core.validation

object EmailValidator {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValid(value: String): Boolean = emailPattern.matches(value.trim())
}
