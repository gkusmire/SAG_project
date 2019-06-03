package pl.sag.models

data class BuyResponse(
    val flightId: Int,
    val price: Int,
    val seatsLeft: Int
)