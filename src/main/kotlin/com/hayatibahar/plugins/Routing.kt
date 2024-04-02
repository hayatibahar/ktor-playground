package com.hayatibahar.plugins

import com.hayatibahar.models.TicTacToeGame
import com.hayatibahar.socket
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(game: TicTacToeGame) {
    routing {
        socket(game)
    }
}
