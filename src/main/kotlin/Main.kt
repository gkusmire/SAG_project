package pl.sag

import it.lamba.addRmaAgent
import it.lamba.createNewAgent
import it.lamba.run
import jade.core.Profile
import jade.core.ProfileImpl
import jade.core.Runtime
import pl.sag.airline.AirlineAgent
import pl.sag.models.*
import pl.sag.seller.SellerAgent
import pl.sag.seller.SellersSupervisorAgent
import java.util.*

fun main(args: Array<String>) {
//    genTest(args[0])
    simulation()
}

fun simulation() {
    val myRuntime = Runtime.instance()

    val myProfile = ProfileImpl()
    myProfile.setParameter(Profile.MAIN_PORT, "1099")

    val mainContainer = myRuntime.createMainContainer(myProfile)

    val testCaseDir = "test_case_3"
    val testSetup = parseJsonFile<TestSetup>("$testCaseDir/test_setup.json")

    val airlinesNumber = testSetup.airlines
    val sellersNumber = testSetup.sellers

    mainContainer.apply {
        addRmaAgent()

        println("Creating $airlinesNumber AirlineAgent(s)")
        (1..airlinesNumber).map {
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

        (1..sellersNumber).map {
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

fun genTest(dirPath: String) {
    // airline
    val date1 = Calendar.getInstance()
    date1.set(2019, 6, 3)

    for (i in 1..1000) {
        val airlineSetup = AirlineSetup(
            listOf(
                Flight(i * 2 - 1, "Warsaw", "New York", date1.time, 1, 50.0f),
                Flight(i * 2, "Warsaw", "Budapest", date1.time, 10, 20.0f)

            )
        )
        saveToJsonFile("$dirPath/airline_$i.json", airlineSetup)
    }
    // seller
    val sellerTasks = (1..1000).map {
        val dateFrom = Calendar.getInstance().apply {
            set(2019, 6, 2)
        }
        val dateTo = Calendar.getInstance().apply {
            set(2019, 6, 5)
        }

            SellerTask(100.0f, "Warsaw", "New York", dateFrom.time, dateTo.time, 1)
    }
    val sellerSetup = SellerSetup(sellerTasks)
    saveToJsonFile("$dirPath/seller_1.json", sellerSetup)

}