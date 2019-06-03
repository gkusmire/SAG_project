package pl.sag.airline

import pl.sag.models.BuyRequest
import pl.sag.models.BuyResponse
import pl.sag.models.Flight

class FlightsRepository {
    private val flights = mutableListOf<Flight>()

    fun find(from: String, to: String): Flight {
        return flights.first { it.from == from && it.to == to }
    }

//    fun buy(buyRequest: BuyRequest): Response<BuyResponse> {
//        val flight = findById(buyRequest.flightId)
//
//        return if (flight.seatsLeft == 0) {
//            ErrorResponse("NO_SEATS_AVAILABLE")
//        } else if (flight.seatsLeft < buyRequest.seatsCount) {
//            ErrorResponse("NO_SUCH_SEATS")
//        } else {
//            flights.removeIf { it.id == buyRequest.flightId }
//            flights.add(flight.copy(seatsLeft = flight.seatsLeft - buyRequest.seatsCount))
//
//            BuyResponse(flightId = buyRequest.flightId, se = buyRequest.seatsCount)
//                .asSuccessResponse()
//        }
//    }

    fun addAll(flights: List<Flight>) = this.flights.addAll(flights)

    private fun findById(id: Int) = flights.first { it.id == id }
}