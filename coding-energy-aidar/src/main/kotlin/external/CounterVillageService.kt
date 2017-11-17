package external

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.concurrent.ConcurrentHashMap


fun main(args: Array<String>) {
    data class AppConfig(val port: Int)

    val appConfig = jacksonObjectMapper().readValue<AppConfig>(args[0])

    val configurationMap = ConcurrentHashMap<Long, String>()

    val server = embeddedServer(Netty, appConfig.port) {
        install(DefaultHeaders)
        install(Compression)
        install(CallLogging)
        install(ContentNegotiation) {
            jackson {
                configure(SerializationFeature.INDENT_OUTPUT, true)
                registerModule(JavaTimeModule())
            }
        }
        routing {
            data class CounterInfo(val id: Long, val village: String)

            get("/counter") {
                val id = call.parameters["id"]!!.toLong()
                call.respond(CounterInfo(id, configurationMap[id]!!))
            }
            post("/counter") {
                val counterInfo = call.receive<CounterInfo>()
                configurationMap.put(counterInfo.id, counterInfo.village)
                call.respond(HttpStatusCode(200, "Lets do that"))
            }
        }
    }
    server.start(wait = true)
}
