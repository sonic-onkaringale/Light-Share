package client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

// Initialize Ktor HttpClient with WebSocket support
val client = HttpClient(CIO) {

    install(ContentNegotiation) {
        json()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5 * 60 * 1000 // 5 minutes
    }
    install(WebSockets) {
        pingInterval = 120.seconds
        maxFrameSize = Long.MAX_VALUE
        contentConverter= KotlinxWebsocketSerializationConverter(Json)

    }
    engine {
        maxConnectionsCount = 100
    }

}


