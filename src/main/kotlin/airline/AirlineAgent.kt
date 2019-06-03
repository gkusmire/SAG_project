package pl.sag.airline

import it.lamba.agents.ModernAgent
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import pl.sag.fromJSON
import pl.sag.models.Flight
import pl.sag.models.OfferRefuseResponse
import pl.sag.models.OfferRequest
import pl.sag.models.RefuseReason
import pl.sag.toJSON
import pl.sag.utils.blockingReceive
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

    private val flightsRepository = FlightsRepository().apply {
        addAll(listOf(
            Flight(id = 1, from = "New York", to = "Warsaw", seatsLeft = 100, price = 1000.0f),
            Flight(id = 2, from = "New York", to = "Washington", seatsLeft = 50, price = 200.0f),
            Flight(id = 3, from = "Warsaw", to = "Cracow", seatsLeft = 20, price = 80.0f)
        ))
    }

    override fun onCreate(args: Array<String>) {
        // Rejestracja usług agenta u agenta DF
        DFService.register(this, getDFAgentDescription())

        // Zachowanie, które polega na przyjmowaniu zapytań o ofertę kupna biletów lotniczych
        cyclic {
            val offerRequestMsg = blockingReceive(ACLMessage.CFP)
            val offerRequest = fromJSON<OfferRequest>(offerRequestMsg.content)
            log("request from: ${offerRequestMsg.sender.localName}, content = $offerRequest")

            val matchedFlight = flightsRepository.find(offerRequest.from, offerRequest.to)

            val reply = offerRequestMsg.createReply().apply {
                if (matchedFlight != null) {
                    performative = ACLMessage.PROPOSE
                    content = toJSON(matchedFlight)
                } else {
                    performative = ACLMessage.REFUSE
                    content = toJSON(OfferRefuseResponse(RefuseReason.NO_FLIGHT_FOUND))
                }
            }
            send(reply)
            log("send response to: ${offerRequestMsg.sender.localName}")
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