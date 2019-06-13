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
import pl.sag.airline.AirlineAgent
import pl.sag.fromJSON
import pl.sag.models.*
import pl.sag.parseJsonFile
import pl.sag.toJSON
import pl.sag.utils.searchAgents

class SellerAgent : ModernAgent() {

    private val proposeReplies = mutableListOf<ACLMessage>()

    private val proposedOffers = mutableMapOf<AID, Flight>()

    private var bestOffer: Pair<AID, Flight>? = null

    override fun onCreate(args: Array<String>) {

        //TODO pobiera następne zlecenie na zakup biletów
        val setup = parseJsonFile<SellerSetup>(args[0])

        addBehaviour(SequentialBehaviour().apply {
            setup.tasks.forEach {
                addSubBehaviour(BuyTicketsBehaviour(it))
            }
        })

    }

    override fun onDestroy() = Unit

    override fun onMessageReceived(message: ACLMessage) {
        log("onMessageReceived: $message")
    }

    private fun Agent.searchAirlineAgents(): Single<List<DFAgentDescription>> {
        val searchedServiceDescription = ServiceDescription().apply {
            type = AirlineAgent.SERVICE_TYPE
        }

        return searchAgents(searchedServiceDescription)
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
                        message.apply {
                            it.forEach { addReceiver(it.name)}
                            content = toJSON(OfferRequest(from = task.from, to = task.to))
                            replyWith = messageID
                            conversationId = ticketConversationId
                        }
                        myAgent.send(message)
                    }
                    messageTemplate = MessageTemplate.MatchConversationId(ticketConversationId)

                    state = State.RESPONSE_OFFER_RECEIVE
                    myAgent.log("sending offer request (from=${task.from}, to=${task.to})")
                }
                // odbiera zapytania ofertowe
                State.RESPONSE_OFFER_RECEIVE -> {
                    // jeśli czas nie zosał przekroczony
                    if (startTime + timeout >= System.currentTimeMillis()) {
                        val msg = agent.receive(messageTemplate)
                        msg?.let { receivedMessages.add(msg)
                            myAgent.log("receive offer response from ${msg.sender.localName} (${msg.content})") }

                        if(receivedMessages.size == 1) {    //TODO liczba agentów
                            // wszyscy agenci odpowiedzieli
                            state = State.REQUEST_BUY_TO_SEND
                            receivedMessages
                                .filter { it.performative == ACLMessage.PROPOSE }

                        }
                    } else {
                        // Timeout
                        state = State.REQUEST_BUY_TO_SEND
                        receivedMessages
                            .filter { it.performative == ACLMessage.PROPOSE }
                    }
                }
                State.REQUEST_BUY_TO_SEND -> {
                    if(receivedMessages.size == 0) {
                        myAgent.log("${myAgent.localName}: No tickets to buy for param (from=${task.from}, to=$task.to)")
                        state = State.FINISHED
                    }
                    val bestOffer = receivedMessages.minBy { fromJSON<Flight>(it.content).price }
                    receivedMessages.remove(bestOffer)

                    message = ACLMessage(ACLMessage.ACCEPT_PROPOSAL).apply {
                        addReceiver(bestOffer!!.sender)
                        content = toJSON(BuyRequest(flightId = fromJSON<Flight>(bestOffer.content).id, seatsCount = task.amount))
                        conversationId = ticketConversationId
                        replyWith = messageID
                    }
                    messageTemplate = MessageTemplate.MatchConversationId(ticketConversationId)
                    myAgent.log("Send request of buying tickets to ${bestOffer!!.sender.localName}")
                    myAgent.send(message)
                    state = State.RESPONSE_BUY_RECEIVE
                }
                State.RESPONSE_BUY_RECEIVE -> {
                    val msg = myAgent.receive(messageTemplate)
                    msg?.let {
                        //if(it.performative == null) block()
                        if(it.performative == ACLMessage.INFORM) {
                            myAgent.log("Accept of buying ticket from ${msg.sender.localName} (from=${task.from}, to=${task.to})")
                            state = State.FINISHED
                        }
                        else {
                            // wybieramy następną najlepszą ofertę
                            myAgent.log("Airline ${msg.sender.localName} refuse offert of buying tickets")
                            state = State.REQUEST_BUY_TO_SEND
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