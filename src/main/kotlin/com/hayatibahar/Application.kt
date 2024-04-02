package com.hayatibahar

import com.hayatibahar.models.TicTacToeGame
import com.hayatibahar.plugins.configureMonitoring
import com.hayatibahar.plugins.configureRouting
import com.hayatibahar.plugins.configureSerialization
import com.hayatibahar.plugins.configureSockets
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val game = TicTacToeGame()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureRouting(game)
}
