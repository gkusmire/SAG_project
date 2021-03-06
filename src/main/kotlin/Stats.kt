package pl.sag

import jade.core.Agent
import pl.sag.models.BuyRequest
import pl.sag.models.BuyResponseRefuse
import pl.sag.models.BuyResponseSuccess
import pl.sag.models.OfferRequest
import java.util.*


class Stats private constructor() {
    companion object {
        val INSTANCE by lazy { Stats() }
    }

    val ticketsStats = Collections.synchronizedMap(mutableMapOf<Int, TicketsStats>())

    var statsState = StatsState(
        sellerAgentCount = 0,
        airlineAgentCount = 0
    )

    fun registerAirline(airline: Agent) {
        statsState.apply {
            airlineAgentCount += 1
        }
    }

    fun registerSeller(seller: Agent) {
        statsState.apply {
            sellerAgentCount += 1
        }
    }

    fun onOfferRequest(seller: Agent, offerRequest: OfferRequest) {

    }

    fun onBuyRequest(buyRequest: BuyRequest) {
        ticketsStats[buyRequest.flightId] = ticketsStats[buyRequest.flightId].let {
            return@let if (it == null) {
                TicketsStats(requestedToBuy = 1, actuallyBought = 0, buyRefused = 0)
            } else {
                it.copy(requestedToBuy = it.requestedToBuy + 1)
            }
        }
    }

    fun onBuyResponseSuccess(buySuccess: BuyResponseSuccess) {
        ticketsStats[buySuccess.flightId] = ticketsStats[buySuccess.flightId].let {
            return@let if (it == null) {
                TicketsStats(requestedToBuy = 0, actuallyBought = 1, buyRefused = 0)
            } else {
                it.copy(actuallyBought = it.actuallyBought + 1)
            }
        }
    }

    fun onBuyResponseFailure(buyRefuse: BuyResponseRefuse) {
        ticketsStats[buyRefuse.flightId] = ticketsStats[buyRefuse.flightId].let {
            return@let if (it == null) {
                TicketsStats(requestedToBuy = 0, actuallyBought = 0, buyRefused = 1)
            } else {
                it.copy(buyRefused = it.buyRefused + 1)
            }
        }
    }

    fun printStats() {
        println("=====Stats=====")
        println(ticketsStats)
    }

}

data class StatsState(
    var sellerAgentCount: Int,
    var airlineAgentCount: Int
)

data class TicketsStats(
    val requestedToBuy: Int,
    val actuallyBought: Int,
    val buyRefused: Int
)