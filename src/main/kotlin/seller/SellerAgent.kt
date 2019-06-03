package pl.sag.seller

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import it.lamba.agents.ModernAgent
import jade.core.AID
import jade.core.behaviours.OneShotBehaviour
import jade.core.behaviours.SequentialBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import pl.sag.airline.AirlineAgent
import pl.sag.models.OfferRequest
import pl.sag.toJSON
import pl.sag.utils.blockingReceiveReply
import pl.sag.utils.oneShot
import pl.sag.utils.searchAgents

class SellerAgent : ModernAgent() {

    private var budget = 1000.0

    override fun onCreate(args: Array<String>) {
        searchAirlineAgents().subscribeBy {
            val sequence = SequentialBehaviour().apply {
                val request = ACLMessage(ACLMessage.CFP).apply {
                    it.forEach { addReceiver(it.name) }
                    content = toJSON(OfferRequest(from = "Warsaw", to = "Warsaw"))
                    replyWith = "OfferRequest-${System.currentTimeMillis()}"
                }

                addSubBehaviour(oneShot {
                    send(request)
                })
                addSubBehaviour(TimeoutReceiverBehaviour(
                    template = MessageTemplate.MatchInReplyTo(request.replyWith),
                    timeout = 5000,
                    onFinished = { messages ->
                        messages.forEach {
                            when (it.performative) {
                                ACLMessage.PROPOSE -> log("propose from ${it.sender.localName}")
                                ACLMessage.REFUSE -> log("refuse from ${it.sender.localName}")
                            }
                        }
                    }
                ))
            }
            addBehaviour(sequence)
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