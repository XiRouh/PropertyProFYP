package com.example.propertyprofyp

data class Message(
    val message: String,
    val id: String,
    val time: String,
    val property: Property? = null
)