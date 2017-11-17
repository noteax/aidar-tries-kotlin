import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import consumer.CommandsConsumer
import consumer.OutdatedCounterValuesConsumer
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
import model.Counter
import model.Village
import service.CounterChangeService
import service.CounterValuesService
import kotlin.concurrent.fixedRateTimer

fun main(args: Array<String>) {
    data class AppConfig(val port: Int, val counterVillageServiceAddress: String, val kafkaBootstrapServers: String)

    val appConfig = jacksonObjectMapper().readValue<AppConfig>(args[0])

    val counterChange = CounterChangeService(appConfig.kafkaBootstrapServers)
    val counterValuesService = CounterValuesService()

    val commandsConsumer = CommandsConsumer(appConfig.kafkaBootstrapServers, counterValuesService)
    val outdatedCounterValuesConsumer = OutdatedCounterValuesConsumer(appConfig.kafkaBootstrapServers)

    fixedRateTimer("counter-change-consumer", true, 0, 3000,
            { commandsConsumer.run() })
    fixedRateTimer("outdated-counter-consumer", true, 0, 3000,
            { outdatedCounterValuesConsumer.run() })

    // start http server
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
            post("/counter_callback") {
                val counter = call.receive<Counter>()
                counterChange.newCounterValue(counter)
                call.respond(HttpStatusCode(200, "Value accepted"))
            }
            get("/consumption_report") {
                data class ConsumptionReport(val villages: List<Village>)

                val duration = call.parameters["duration"]
                if (duration != "24h") {
                    throw RuntimeException("Could report for $duration duration")
                }
                call.respond(ConsumptionReport(counterValuesService.getVillages()))
            }
        }
    }
    server.start(wait = true)
}