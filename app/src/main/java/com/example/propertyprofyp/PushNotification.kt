package com.example.propertyprofyp

data class PushNotification(
    val data: Announcement,
    val registration_ids: List<String> // Use this for multiple tokens
)