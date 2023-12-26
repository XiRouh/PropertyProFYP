package com.example.propertyprofyp

data class Purchase(
    val bookingId: String = "",
    val userId: String = "",
    val staffId: String = "",
    val projectName: String = "",
    val projectId: String = "",
    val price: Double = 0.0,
    var loanDSR: Double = 0.0
)