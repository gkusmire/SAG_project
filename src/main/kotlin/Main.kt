package pl.sag

import it.lamba.addRmaAgent
import it.lamba.createNewAgent
import it.lamba.run
import jade.core.Profile
import jade.core.ProfileImpl
import jade.core.Runtime
import pl.sag.airline.AirlineAgent
import pl.sag.models.Destinations
import pl.sag.seller.SellerAgent

fun main() {
    val myRuntime = Runtime.instance()

    val myProfile = ProfileImpl()
    myProfile.setParameter(Profile.MAIN_PORT, "1099")

    val mainContainer = myRuntime.createMainContainer(myProfile)
    val destinations = parseJsonFile<Destinations>("destinations.json")
    println("main: $destinations")

    mainContainer.apply {
        addRmaAgent()

        for (i in 1..4) {
            createNewAgent(
                agentClass = AirlineAgent::class,
                nickname = "airline#$i",
                args = destinations?.destinations?.toTypedArray()
            ).run()
        }
        createNewAgent(
            agentClass = SellerAgent::class,
            nickname = "seller",
            args = destinations?.destinations?.toTypedArray()
        ).run()
    }
}