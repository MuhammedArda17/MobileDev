package com.example.mobiledev.data.model

enum class UserRole(val value: String) {
    CUSTOMER("customer"),
    MERCHANT("merchant");

    companion object {
        fun fromValue(value: String?): UserRole? = entries.firstOrNull { it.value == value }
    }
}

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.CUSTOMER.value
)