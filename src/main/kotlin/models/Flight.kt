package pl.sag.models

import java.util.*

data class Flight(
    val id: Int,
    val from: String,
    val to: String,
    val dateFrom: Date,
    val dateTo: Date,
    val seatsLeft: Int,
    val price: Float
)