package pl.sag.airline

import it.lamba.agents.ModernAgent
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import pl.sag.fromJSON
import pl.sag.models.*
import pl.sag.parseJsonFile
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

    private val flightsRepository = FlightsRepository()

    override fun onCreate(args: Array<String>) {
        // Wczytywanie pliku wejsciowego
        parseJsonFile<AirlineSetup>(args[0]).apply {
            flightsRepository.addAll(flights)
        }

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

                    log("send propose to: ${offerRequestMsg.sender.localName}")
                } else {
                    performative = ACLMessage.REFUSE
                    content = toJSON(OfferRefuseResponse(RefuseReason.NO_FLIGHT_FOUND))

                    log("send refuse to: ${offerRequestMsg.sender.localName}")
                }
            }
            send(reply)

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