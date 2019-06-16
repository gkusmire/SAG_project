package pl.sag.seller

import it.lamba.agents.ModernAgent
import jade.core.behaviours.Behaviour
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import pl.sag.Stats
import pl.sag.utils.cyclic

class SellersSupervisorAgent : ModernAgent() {

    var sellersCount = 0
    var sellersFinishedCount = 0

    override fun onCreate(args: Array<String>) {
        sellersCount = args[0].toInt()
        log("Created, sellersCount = $sellersCount")

        addBehaviour(object : Behaviour() {
            override fun action() {
                val messageTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                    MessageTemplate.MatchContent("FINISHED")
                )
                val msg = blockingReceive(messageTemplate)
                if (msg != null) {
                    sellersFinishedCount += 1
                }
                if (sellersCount == sellersFinishedCount) {
                    Stats.INSTANCE.printStats()
                }
            }

            override fun done() = sellersCount == sellersFinishedCount

        })
    }

    override fun onDestroy() = Unit

    override fun onMessageReceived(message: ACLMessage) = Unit

}