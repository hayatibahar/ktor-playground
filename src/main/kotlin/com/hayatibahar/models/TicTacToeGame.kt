package com.hayatibahar.models

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TicTacToeGame {

    private val state = MutableStateFlow(GameState())

    private val playerSockets = ConcurrentHashMap<Char, WebSocketSession>()

    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var delayGameJob: Job? = null

    init {
        state.onEach(::broadcast).launchIn(gameScope)
    }

    fun connectPlayer(session: WebSocketSession): Char? {
        val isPlayerX = state.value.connectedPlayers.any { it == 'X' }
        val player = if (isPlayerX) 'O' else 'X'

        state.update {
            if (state.value.connectedPlayers.contains(player)) {
                return null
            }
            if (!playerSockets.containsKey(player)) {
                playerSockets[player] = session
            }
            it.copy(
                connectedPlayers = it.connectedPlayers + player
            )
        }
        return player
    }

    fun disconnectPlayer(player: Char) {
        playerSockets.remove(player)
        state.update {
            it.copy(
                connectedPlayers = it.connectedPlayers - player
            )
        }
    }

    private suspend fun broadcast(state: GameState) {
        playerSockets.values.forEach { socket ->
            socket.send(
                Json.encodeToString(state)
            )
        }
    }

    fun finishTurn(player: Char, x: Int, y: Int) {
        if (state.value.field[y][x] != null || state.value.winningPlayer != null) {
            return
        }
        if (state.value.playerAtTurn != player) {
            return
        }

        val currentPlayer = state.value.playerAtTurn

        state.update {
            val newField = it.field.also { field ->
                field[y][x] = currentPlayer
            }
            val isBoardFull = newField.all { fields -> fields.all { field -> field != null } }
            if (isBoardFull) {
                startNewRoundDelayed()
            }
            it.copy(
                playerAtTurn = if (currentPlayer == 'X') 'O' else 'X',
                field = newField,
                isBoardFull = isBoardFull,
                winningPlayer = getWinningPlayer()?.also {
                    startNewRoundDelayed()
                }
            )
        }
    }

    private fun getWinningPlayer(): Char? {
        val field = state.value.field
        val lines = listOf(
            listOf(field[0][0], field[0][1], field[0][2]),
            listOf(field[1][0], field[1][1], field[1][2]),
            listOf(field[2][0], field[2][1], field[2][2]),

            listOf(field[0][0], field[1][0], field[2][0]),
            listOf(field[0][1], field[1][1], field[2][1]),
            listOf(field[0][2], field[1][2], field[2][2]),

            listOf(field[0][0], field[1][1], field[2][2]),
            listOf(field[0][2], field[1][1], field[2][0])
        )
        lines.forEach { line ->
            if (line.all { it == 'X' }) {
                return 'X'
            } else if (line.all { it == 'O' }) {
                return 'O'
            }
        }

        return null
    }

    private fun startNewRoundDelayed() {
        delayGameJob?.cancel()
        delayGameJob = gameScope.launch {
            delay(5000L)
            state.update {
                it.copy(
                    playerAtTurn = 'X',
                    field = GameState.emptyField(),
                    winningPlayer = null,
                    isBoardFull = false,
                )
            }
        }
    }
}