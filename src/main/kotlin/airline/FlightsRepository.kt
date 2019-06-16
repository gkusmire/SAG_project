package pl.sag.airline

import pl.sag.models.Flight

class FlightsRepository {
    private val flights = mutableListOf<Flight>()

    fun find(from: String, to: String): Flight? {
        return flights.firstOrNull { it.from == from && it.to == to }
    }

    fun addAll(flights: List<Flight>) = this.flights.addAll(flights)

    fun findById(id: Int) = flights.firstOrNull { it.id == id }

    fun buyTickets(flightId: Int, seatsCount: Int): Flight? {
        flights.apply {
            val flight = find { it.id == flightId }

            return flight?.let {
                if (it.seatsLeft >= seatsCount) {
                    flights.remove(it)
                    flights.add(it.copy(seatsLeft = it.seatsLeft - seatsCount))

                    it
                } else {
                    null
                }
            }
        }
    }
}