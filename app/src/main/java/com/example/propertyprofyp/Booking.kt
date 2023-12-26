package com.example.propertyprofyp

data class Booking(
    val id: String = "",
    val userId: String = "",
    val staffId: String = "",
    val date: String = "",
    val time: String = "",
    val projectName: String = "",
    val address: String = "",
    val status: String = "Pending"
)