package pl.sag.airline

import it.lamba.agents.ModernAgent
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import pl.sag.Stats
import pl.sag.fromJSON
import pl.sag.models.*
import pl.sag.parseJsonFile
import pl.sag.toJSON
import pl.sag.utils.cyclic


class AirlineAgent : ModernAgent() {
    companion object {
        const val SERVICE_TYPE = "TICKETS_SERVICE"
        const val SERVICE_NAME = "AIRLINE_TICKETS_SERVICE"

        private fun getDFAgentDescription(): DFAgentDescription {
            val serviceDescription = ServiceDescription().apply {
                type = SERVICE_TYPE
                name = SERVICE_NAME
            }
            return DFAgentDescription().apply {
                addServices(serviceDescription)
            }
        }
    }

    private val flightsRepository = FlightsRepository()

    override fun onCreate(args: Array<String>) {
        Stats.INSTANCE.registerAirline(this)

        // Wczytywanie pliku wejsciowego
        parseJsonFile<AirlineSetup>(args[0]).apply {
            flightsRepository.addAll(flights)
        }

        // Rejestracja usług agenta u agenta DF
        DFService.register(this, getDFAgentDescription())

        // Drugi typ requestu - żądanie kupna biletu
        cyclic {
            receive(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL))?.let {
                val buyRequest = fromJSON<BuyRequest>(it.content)
                log("Buy request: ${it.sender.localName}, content = $buyRequest")
                Stats.INSTANCE.onBuyRequest(buyRequest = buyRequest)

                val flight = flightsRepository.findById(buyRequest.flightId)

                val reply = it.createReply().apply {
                    if (flight != null) {
                        if (flight.seatsLeft >= buyRequest.seatsCount) {
                            flightsRepository.buyTickets(buyRequest.flightId, buyRequest.seatsCount)

                            performative = ACLMessage.AGREE
                            content = toJSON(
                                BuyResponseSuccess(
                                    flightId = buyRequest.flightId
                                )
                            )

                        } else {
                            performative = ACLMessage.FAILURE
                            content = toJSON(
                                BuyResponseRefuse(
                                    flightId = buyRequest.flightId,
                                    reason = RefuseReason.TOO_FEW_SEATS
                                )
                            )
                        }
                    } else {
                        performative = ACLMessage.FAILURE
                        content = toJSON(
                            BuyResponseRefuse(
                                flightId = buyRequest.flightId,
                                reason = RefuseReason.NO_SUCH_FLIGHT
                            )
                        )
                    }
                }
                log("Send ${reply.content} to ${it.sender.localName}")
                send(reply)
            }

        }

        // Cykliczna obsługa requestów
        cyclic {
            receive(MessageTemplate.MatchPerformative(ACLMessage.CFP))?.let {
                val offerRequest = fromJSON<OfferRequest>(it.content)
                log("Offer request: ${it.sender.localName}, content = $offerRequest")

                val matchedFlight = flightsRepository.find(
                    offerRequest.from,
                    offerRequest.to,
                    offerRequest.fromDate,
                    offerRequest.toDate
                )

                val reply = it.createReply().apply {
                    if (matchedFlight != null) {
                        performative = ACLMessage.PROPOSE
                        content = toJSON(matchedFlight)

                        log("send propose to: ${it.sender.localName}")
                    } else {
                        performative = ACLMessage.REFUSE
                        log("send refuse to: ${it.sender.localName}")
                    }
                }
                send(reply)
            }
        }
    }

    override fun onDestroy() {
        DFService.deregister(this, getDFAgentDescription())
        log("unregister $SERVICE_NAME")
    }

    override fun onMessageReceived(message: ACLMessage) {
        log("onMessageReceived: $message")
    }
}