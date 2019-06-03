package pl.sag.utils

import jade.core.behaviours.OneShotBehaviour

fun oneShot(action: () -> Unit): OneShotBehaviour {
    return object : OneShotBehaviour() {
        override fun action() = action()
    }
}