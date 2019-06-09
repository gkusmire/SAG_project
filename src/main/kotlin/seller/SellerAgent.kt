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

    private var sellerState: Int = 0

    private var request: ACLMessage = ACLMessage(ACLMessage.CFP)

    override fun onCreate(args: Array<String>) {
        setup = parseJsonFile(args[0])

        searchAirlineAgents().subscribeBy {
            val sequence = SequentialBehaviour().apply {

                when(sellerState) {

                    //Pobieramy nowe "zlecenie" na bilety i wysyłamy requesty do Agentów linii lotniczych
                    0 -> {
                        val offerRequest = getNextOfferRequest()
                        request = ACLMessage(ACLMessage.CFP).apply {
                            it.forEach { addReceiver(it.name) }
                            content = toJSON(offerRequest)
                            replyWith = "OfferRequest-${System.currentTimeMillis()}"
                        }
                        // wyslanie requesta
                        addSubBehaviour(oneShot {
                            send(request)
                        })
                        sellerState = 1
                    }

                    // odebranie odpowiedzi do zapytania o oferte od linii lotniczych
                    // jeśli nie otrzymano odpowiedzi - wróć do 0.
                    1 -> {
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
                 //       sellerState = 2
                 //   }

                    //Wybieramy najlepszą ofertę i wysyłamy żądanie zakupu biletów
                    //Jeśli znaleziono oferty z biletami - wybierz najlepszą i zarezerwuj bilety
                    //Jeśli nie znaleziono oferty z zadanym biletem - trudno, wracamy do punktu 0.
                  //  2 -> {
                        addSubBehaviour(oneShot {
                            // parsowanie odpowiedzi do zapytania o ofertę
                            val proposedOffers = proposeReplies
                                .filter { it.performative == ACLMessage.PROPOSE }
                                .groupBy { it.sender }
                                .mapValues { fromJSON<Flight>(it.value.first().content) }

                            // wybór najlepszej oferty (na podstawie ceny)
                            val bestOffer = proposedOffers.minBy { it.value.price }?.toPair()

                            bestOffer?.let {
                                log("Best offer from ${it.first.localName} = ${it.second}")
                            }

                        })
                    }

                    //Odbieramy info, czy udało się zakupić bilet
                    //Jeśli nie, to wracamy do 2.
                    //Jeśli tak, to do 0.
                    3 -> {
                        addSubBehaviour(oneShot {

                        })
                    }

                }

            }
            addBehaviour(sequence)
        }
    }

    private fun getNextOfferRequest(): OfferRequest {
        return OfferRequest(from = setup.from, to = setup.to)
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