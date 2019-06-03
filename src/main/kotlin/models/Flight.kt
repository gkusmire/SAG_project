package pl.sag.models

data class Flight(
    val id: Int,
    val from: String,
    val to: String,
    val price: Float,
    val seatsLeft: Int
)