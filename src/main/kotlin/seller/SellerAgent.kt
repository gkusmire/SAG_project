package pl.sag.seller

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import it.lamba.agents.ModernAgent
import jade.core.AID
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import pl.sag.airline.AirlineAgent
import pl.sag.utils.oneShot
import pl.sag.utils.searchAgents

class SellerAgent : ModernAgent() {

    private var budget = 1000.0

    override fun onCreate(args: Array<String>) {
        searchAirlineAgents().apply {
            subscribeBy {
                log("found ${it.size} airlines")
            }
        }

        oneShot {
            searchAirlineAgents().subscribeBy {
                it.forEach {
                    val agentAID = it.name.localName
                    log("send message to $agentAID")
                    send(ACLMessage(ACLMessage.INFORM).apply {
                        content = "hey I'm $name"
                        addReceiver(AID(agentAID, AID.ISLOCALNAME))
                    })
                }
            }
        }
    }

    override fun onDestroy() = Unit

    override fun onMessageReceived(message: ACLMessage) {
        log("onMessageReceived: $message")
    }

    private fun searchAirlineAgents(): Single<List<DFAgentDescription>> {
        val searchedServiceDescription = ServiceDescription().apply {
            type = AirlineAgent.SERVICE_TYPE
        }

        return searchAgents(searchedServiceDescription)
    }

}