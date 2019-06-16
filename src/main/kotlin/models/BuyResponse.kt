package pl.sag.models


data class BuyResponseSuccess(
    val flightId: Int
)

enum class RefuseReason {
    NO_SUCH_FLIGHT, TOO_FEW_SEATS
}

data class BuyResponseRefuse(
    val flightId: Int,
    val reason: RefuseReason
)