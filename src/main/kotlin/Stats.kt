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

    }

    fun onBuyResponseSuccess(airline: Agent, seller: Agent, buySuccess: BuyResponseSuccess) {

    }

    fun onBuyResponseFailure(airline: Agent, seller: Agent, buyRefuse: BuyResponseRefuse) {

    }

}

data class StatsState(
    var sellerAgentCount: Int,
    var airlineAgentCount: Int
)