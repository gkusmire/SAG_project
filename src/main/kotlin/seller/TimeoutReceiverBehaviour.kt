package pl.sag.seller

import jade.core.behaviours.SimpleBehaviour
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate

/**
 * @param template Template of messages, that should be received by Receiver
 * @param timeout After [timeout] the receiver stops receiving message
 * @param maxMessageCount Maximum number of messages that should be received by Receiver.
 *                        If equals to -1, the receiver receives messages until the timeout elapsed.
 * @param onFinished Callback with received messages list
 */
class TimeoutReceiverBehaviour(
    private val template: MessageTemplate? = null,
    private val timeout: Long,
    private val maxMessageCount: Int = -1,
    private val onFinished: (List<ACLMessage>) -> Unit
) : SimpleBehaviour() {
    private val receivedMessages = mutableListOf<ACLMessage>()

    private var startTime: Long = 0
    private var isFinished: Boolean = false

    override fun onStart() {
        startTime = System.currentTimeMillis()
    }

    override fun action() {
        if (startTime + timeout >= System.currentTimeMillis()) {
            if (maxMessageCount == -1 || receivedMessages.size < maxMessageCount) {

                val msg = template?.let {
                    agent.receive(it)
                } ?: agent.receive()

                msg?.also { receivedMessages.add(it) }
            } else {
                isFinished = true
                onFinished(receivedMessages)
                return
            }
        } else {
            // Timeout
            isFinished = true
            onFinished(receivedMessages)
        }
    }

    private fun receiveMessage() {
        val msg = template?.let { agent.receive(it) } ?: agent.receive()
    }

    override fun done() = isFinished

}