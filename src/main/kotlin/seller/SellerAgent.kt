package pl.sag.seller

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import it.lamba.agents.ModernAgent
import jade.core.AID
import jade.core.behaviours.Behaviour
import jade.core.behaviours.OneShotBehaviour
import jade.core.behaviours.SequentialBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import pl.sag.Stats
import pl.sag.airline.AirlineAgent
import pl.sag.fromJSON
import pl.sag.models.Flight
import pl.sag.models.OfferRequest
import pl.sag.models.SellerSetup
import pl.sag.parseJsonFile
import pl.sag.toJSON
import pl.sag.utils.blockingReceiveReply
import pl.sag.utils.oneShot
import pl.sag.utils.searchAgents

class SellerAgent : ModernAgent() {

    private val proposeReplies = mutableListOf<ACLMessage>()

    private val proposedOffers = mutableMapOf<AID, Flight>()

    private var bestOffer: Pair<AID, Flight>? = null

    private lateinit var setup: SellerSetup

    override fun onCreate(args: Array<String>) {
        setup = parseJsonFile(args[0])

        searchAirlineAgents().subscribeBy {
            val sequence = SequentialBehaviour().apply {

                val request = ACLMessage(ACLMessage.CFP).apply {
                    it.forEach { addReceiver(it.name) }
                    content = toJSON(OfferRequest(from = setup.from, to = setup.to))
                    replyWith = "OfferRequest-${System.currentTimeMillis()}"
                }
                // wyslanie requesta
                addSubBehaviour(oneShot {
                    send(request)

                    log("${Stats.INSTANCE}")
                })

                // odebranie odpowiedzi do zapytania o oferte od linii lotniczych
                addSubBehaviour(TimeoutReceiverBehaviour(
                    template = MessageTemplate.MatchInReplyTo(request.replyWith),
                    timeout = 5000,
                    maxMessageCount = it.size,
                    onFinished = { messages ->
                        messages.forEach {
                            proposeReplies.clear()
                            proposeReplies.add(it)
                        }
                    }
                ))

                addSubBehaviour(oneShot {
                    // parsowanie odpowiedzi do zapytania o ofertę
                    val proposedOffers = proposeReplies
                        .filter { it.performative == ACLMessage.PROPOSE }
                        .groupBy { it.sender }
                        .mapValues { fromJSON<Flight>(it.value.first().content) }

                    proposedOffers.toSortedMap()

                    // wybór najlepszej oferty (na podstawie ceny)
                    val bestOffer = proposedOffers.minBy { it.value.price }?.toPair()

                    bestOffer?.let {
                        log("Best offer from ${it.first.localName} = ${it.second}")
                    }
                })


                addSubBehaviour(oneShot {

                })

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

    private class BuyBehaviour(private val offers: Map<AID, Flight>) : Behaviour() {
        private var state: State = State.REQUEST_BUY

        override fun action() {
            when (state) {
                State.REQUEST_BUY -> {
                    if (offers.isEmpty()) {
                        state = State.FINISHED
                        (agent as ModernAgent).log("FINISHED")
                    } else {
                        val offer = offers
                    }
                }
            }
        }

        override fun done() = state == State.FINISHED

        enum class State {
            REQUEST_BUY, RESPONSE_BUY, FINISHED
        }

    }

}