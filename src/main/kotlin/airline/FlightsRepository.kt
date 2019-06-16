package pl.sag.airline

import pl.sag.models.Flight
import java.util.*

class FlightsRepository {
    private val flights = mutableListOf<Flight>()

    fun find(from: String, to: String, fromDate: Date, toDate: Date): Flight? {
        return flights.firstOrNull {
            it.from == from && it.to == to &&
                    ((it.date == fromDate || it.date.after(fromDate)) && ((it.date == toDate) || it.date.before(toDate)))
                    && it.seatsLeft > 0
        }
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