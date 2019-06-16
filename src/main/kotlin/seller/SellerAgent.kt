package pl.sag.seller

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import it.lamba.agents.ModernAgent
import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.Behaviour
import jade.core.behaviours.SequentialBehaviour
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import pl.sag.Stats
import pl.sag.airline.AirlineAgent
import pl.sag.fromJSON
import pl.sag.models.*
import pl.sag.parseJsonFile
import pl.sag.toJSON
import pl.sag.utils.oneShot
import pl.sag.utils.searchAgents

class SellerAgent : ModernAgent() {

    override fun onCreate(args: Array<String>) {
        Stats.INSTANCE.registerSeller(this)

        val setup = parseJsonFile<SellerSetup>(args[0])

        addBehaviour(SequentialBehaviour().apply {
            setup.tasks.forEach {
                addSubBehaviour(BuyTicketsBehaviour(it))
            }
            addSubBehaviour(oneShot {
                val msg = ACLMessage(ACLMessage.CONFIRM).apply {
                    content = "FINISHED"
                    addReceiver(AID().apply { localName = "sellers-supervisor" })
                }
                send(msg)
            })
        })

    }

    override fun onDestroy() = Unit

    override fun onMessageReceived(message: ACLMessage) {
        log("onMessageReceived: $message")
    }

    private class BuyTicketsBehaviour(private val task: SellerTask) : Behaviour() {
        private var state: State = State.REQUEST_OFFER_TO_SEND
        private val messageID = "Offer request-${System.currentTimeMillis()}"
        private var message = ACLMessage(ACLMessage.CFP)
        private val ticketConversationId = "ticket_buying"
        private var messageTemplate: MessageTemplate? = null
        private var startTime = System.currentTimeMillis()
        private val timeout = 5000
        private val receivedMessages = mutableListOf<ACLMessage>()
        private var numberOfAirlinesAgents: Int = 0

        private fun Agent.searchAirlineAgents(): Single<List<DFAgentDescription>> {
            val searchedServiceDescription = ServiceDescription().apply {
                type = AirlineAgent.SERVICE_TYPE
            }

            return searchAgents(searchedServiceDescription)
        }

        private fun Agent.log(msg: String) {
            (this as ModernAgent).log(msg)
        }

        override fun action() {
            when (state) {
                // rozsyła zapytanie ofertowe do linii lotniczych
                State.REQUEST_OFFER_TO_SEND -> {
                    myAgent.searchAirlineAgents().subscribeBy {
                        numberOfAirlinesAgents = it.size
                        message.apply {
                            it.forEach { addReceiver(it.name) }
                            content = toJSON(
                                OfferRequest(
                                    from = task.from,
                                    to = task.to,
                                    fromDate = task.dateFrom,
                                    toDate = task.dateTo
                                )
                            )
                            replyWith = messageID
                            conversationId = ticketConversationId
                        }
                        myAgent.send(message)
                    }
                    messageTemplate = MessageTemplate.MatchConversationId(ticketConversationId)

                    state = State.RESPONSE_OFFER_RECEIVE
//                    myAgent.log("sending offer request (from=${task.from}, to=${task.to})")
                }
                // odbiera zapytania ofertowe
                State.RESPONSE_OFFER_RECEIVE -> {
                    // czekamy na odpowiedzi od wszystkich agentów
                        val msg = agent.receive(messageTemplate)
                        msg?.let {
                            receivedMessages.add(msg)
//                            myAgent.log("receive offer response from ${msg.sender.localName} (${msg.content})")

                            if (receivedMessages.size == numberOfAirlinesAgents) {
                                // wszyscy agenci odpowiedzieli
                                receivedMessages
                                    .removeIf { it.performative == ACLMessage.REFUSE }
                                state = State.REQUEST_BUY_TO_SEND
                            }
                        }
                }
                State.REQUEST_BUY_TO_SEND -> {
                    if (receivedMessages.size == 0) {
//                        myAgent.log("${myAgent.localName}: No tickets to buy for param (from=${task.from}, to=$task.to)")
                        state = State.FINISHED
                    } else {
                        val bestOffer = receivedMessages.minBy { fromJSON<Flight>(it.content).price }
                        bestOffer?.apply {
                            receivedMessages.remove(this)

                            val request = BuyRequest(
                                flightId = fromJSON<Flight>(bestOffer.content).id,
                                seatsCount = task.amount
                            )
                            message = createReply().apply {
                                performative = ACLMessage.ACCEPT_PROPOSAL
                                content = toJSON(request)
                            }

                            messageTemplate = MessageTemplate.MatchConversationId(ticketConversationId)
                            myAgent.log("Send request of buying tickets to ${sender.localName}")


                            myAgent.send(message)
                            state = State.RESPONSE_BUY_RECEIVE
                        }

                    }
                }
                State.RESPONSE_BUY_RECEIVE -> {
                    val msg = myAgent.receive(messageTemplate)
                    msg?.let {
                        if (it.performative == ACLMessage.AGREE) {
                            myAgent.log("Accept of buying ticket from ${msg.sender.localName} (from=${task.from}, to=${task.to})")
                            Stats.INSTANCE.onBuyResponseSuccess(buySuccess = fromJSON(it.content))

                            state = State.FINISHED
                        } else {
                            if (it.performative == ACLMessage.FAILURE) {
                                // wybieramy następną najlepszą ofertę
                                myAgent.log("Airline ${msg.sender.localName} refuse offert of buying tickets")
                                Stats.INSTANCE.onBuyResponseFailure(buyRefuse = fromJSON(it.content))

                                state = State.REQUEST_BUY_TO_SEND
                            }
                        }
                    }
                }
            }
        }

        override fun done() = state == State.FINISHED

        enum class State {
            REQUEST_OFFER_TO_SEND, RESPONSE_OFFER_RECEIVE, REQUEST_BUY_TO_SEND, RESPONSE_BUY_RECEIVE, FINISHED
        }

    }

}