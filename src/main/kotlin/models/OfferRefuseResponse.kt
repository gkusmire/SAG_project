package pl.sag.models

enum class RefuseReason {
    NO_FLIGHT_FOUND
}

data class OfferRefuseResponse(val reason: RefuseReason)