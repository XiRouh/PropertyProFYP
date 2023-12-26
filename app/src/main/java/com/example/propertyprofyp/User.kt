package com.example.propertyprofyp

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val userType: String = "",
    val profileImageUrl: String? = "",
    val permissions: Map<String, Boolean> = emptyMap() // Default to an empty map
)
