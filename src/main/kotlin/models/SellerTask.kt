package pl.sag.models

import java.util.*

data class SellerTask(
    val budget: Float,
    val from: String,
    val to: String,
    val dateFrom: Date,
    val dateTo: Date,
    val amount: Int
)

data class SellerSetup(
    val tasks: List<SellerTask>
)