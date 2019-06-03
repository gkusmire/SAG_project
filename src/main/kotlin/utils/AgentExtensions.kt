package pl.sag.utils

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate

fun Agent.searchAgents(service: ServiceDescription): Single<List<DFAgentDescription>> {
    return Single.fromCallable {
        val searchedAgentDescription = DFAgentDescription().apply {
            addServices(service)
        }

        DFService.search(this, searchedAgentDescription).asList()
    }.subscribeOn(Schedulers.io())
}

fun Agent.cyclic(action: () -> Unit) {
    val cyclicBehaviour = object : CyclicBehaviour() {
        override fun action() = action()
    }
    addBehaviour(cyclicBehaviour)
}

fun Agent.blockingReceive(type: Int): ACLMessage {
    return blockingReceive(MessageTemplate.MatchPerformative(type))
}

fun Agent.blockingReceiveReply(request: ACLMessage): ACLMessage {
    return blockingReceive(
        MessageTemplate.MatchInReplyTo(request.replyWith)
    )
}