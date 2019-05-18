package pl.sag.utils

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription

fun Agent.searchAgents(service: ServiceDescription): Single<List<DFAgentDescription>> {
    return Single.fromCallable {
        val searchedAgentDescription = DFAgentDescription().apply {
            addServices(service)
        }

        DFService.search(this, searchedAgentDescription).asList()
    }.subscribeOn(Schedulers.io())
}

fun Agent.oneShot(action: () -> Unit) {
    val oneShotBehaviour = object : OneShotBehaviour() {
        override fun action() = action()
    }
    addBehaviour(oneShotBehaviour)
}

fun Agent.cyclic(action: () -> Unit) {
    val cyclicBehaviour = object : CyclicBehaviour() {
        override fun action() = action()
    }
    addBehaviour(cyclicBehaviour)
}