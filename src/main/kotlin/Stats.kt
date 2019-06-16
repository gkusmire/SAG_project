package pl.sag

import jade.core.Agent
import pl.sag.models.BuyRequest
import pl.sag.models.BuyResponseRefuse
import pl.sag.models.BuyResponseSuccess
import pl.sag.models.OfferRequest


class Stats private constructor() {
    companion object {
        val INSTANCE by lazy { Stats() }
    }

    val ticketsStats = mutableMapOf<Int, TicketsStats>()

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

    fun onBuyRequest(seller: Agent, airline: Agent, buyRequest: BuyRequest) {
        synchronized(ticketsStats) {
            ticketsStats[buyRequest.flightId] = ticketsStats[buyRequest.flightId].let {
                return@let if (it == null) {
                    TicketsStats(wantedToBuy = 1, actuallyBought = 0, buyRefused = 0)
                } else {
                    it.copy(wantedToBuy = it.wantedToBuy + 1)
                }
            }
        }
    }

    fun onBuyResponseSuccess(airline: Agent, seller: Agent, buySuccess: BuyResponseSuccess) {
        synchronized(ticketsStats) {
            ticketsStats[buySuccess.flightId] = ticketsStats[buySuccess.flightId].let {
                return@let if (it == null) {
                    TicketsStats(wantedToBuy = 0, actuallyBought = 1, buyRefused = 0)
                } else {
                    it.copy(actuallyBought = it.actuallyBought + 1)
                }
            }
        }
    }

    fun onBuyResponseFailure(airline: Agent, seller: Agent, buyRefuse: BuyResponseRefuse) {
        synchronized(ticketsStats) {
            ticketsStats[buyRefuse.flightId] = ticketsStats[buyRefuse.flightId].let {
                return@let if (it == null) {
                    TicketsStats(wantedToBuy = 0, actuallyBought = 0, buyRefused = 1)
                } else {
                    it.copy(buyRefused = it.buyRefused + 1)
                }
            }
        }
    }

}

data class StatsState(
    var sellerAgentCount: Int,
    var airlineAgentCount: Int
)

data class TicketsStats(
    val wantedToBuy: Int,
    val actuallyBought: Int,
    val buyRefused: Int
)