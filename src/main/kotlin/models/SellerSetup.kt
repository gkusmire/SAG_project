package pl.sag.models

data class SellerSetup(
    val budget: Float,
    val from: String,
    val to: String,
    val dateFrom: String,
    val dateTo: String,
    val amount: Int
)