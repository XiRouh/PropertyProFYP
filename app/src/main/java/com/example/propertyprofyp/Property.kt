package com.example.propertyprofyp

data class Property(
    //val panelBank: List<String> = emptyList(),
    //val facilities: List<String> = emptyList()
    // Include other fields as necessary
    val id: String = "",
    val propertyType: String = "",
    val tenure: String = "",
    val area: String = "",
    val location: String = "",
    val buildUp: Double = 0.0,
    val price: Double = 0.0,
    val description: String = "",
    val projectName: String = "",
    val developer: String = "",
    val completionDate: String = "",
    val maintenanceFee: Double = 0.0,
    val packageInfo: String = "",
    val coverImage: String = "",
    val images: Map<String, String> = mapOf(),
    val videos: Map<String, String> = mapOf()
) {
    fun getAllMediaUrls(): List<String> {
        val mediaUrls = ArrayList<String>()
        mediaUrls.addAll(images.values)
        mediaUrls.addAll(videos.values)
        return mediaUrls
    }
}



