package kr.co.root.legolas.location.data

data class LocationPlace(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)
