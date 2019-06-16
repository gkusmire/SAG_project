package pl.sag

import it.lamba.addRmaAgent
import it.lamba.createNewAgent
import it.lamba.run
import jade.core.Profile
import jade.core.ProfileImpl
import jade.core.Runtime
import pl.sag.airline.AirlineAgent
import pl.sag.models.TestSetup
import pl.sag.seller.SellerAgent
import pl.sag.seller.SellersSupervisorAgent

fun main() {
    val myRuntime = Runtime.instance()

    val myProfile = ProfileImpl()
    myProfile.setParameter(Profile.MAIN_PORT, "1099")

    val mainContainer = myRuntime.createMainContainer(myProfile)

    val testCaseDir = "test_case_1"
    val testSetup = parseJsonFile<TestSetup>("$testCaseDir/test_setup.json")

    val airlinesNumber = testSetup.airlines
    val sellersNumber = testSetup.sellers

    mainContainer.apply {
        addRmaAgent()

        println("Creating $airlinesNumber AirlineAgent(s)")
        val airlineAgents = (1..airlinesNumber).map {
            createNewAgent(
                agentClass = AirlineAgent::class,
                nickname = "airline#$it",
                args = arrayOf("$testCaseDir/airline_$it.json")
            )
        }.forEach {
            it.run()
        }

        println("Creating $sellersNumber SellerAgent(s)")

        createNewAgent(
            agentClass = SellersSupervisorAgent::class,
            nickname = "sellers-supervisor",
            args = arrayOf("$sellersNumber")
        ).run()

        val sellerAgents = (1..sellersNumber).map {
            createNewAgent(
                agentClass = SellerAgent::class,
                nickname = "seller#$it",
                args = arrayOf("$testCaseDir/seller_$it.json")
            )
        }.forEach {
            it.run()
        }

        println("Starting agents...")
    }
}