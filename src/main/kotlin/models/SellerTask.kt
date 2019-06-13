package pl.sag.models

data class SellerTask(
    val budget: Float,
    val from: String,
    val to: String,
    val dateFrom: String,
    val dateTo: String,
    val amount: Int
)

data class SellerSetup(
    val tasks: List<SellerTask>
)