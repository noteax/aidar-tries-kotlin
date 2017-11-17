package service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import model.Counter
import model.commands.VillageConsumptionChange
import model.commands.VillageConsumptionOutdate
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

val consumptionChangeTopic = "consumptions-change"
val outdatedConsumptionsTopic = "outdated-consumptions"

class CounterChangeService(val kafkaBootstrapServers: String) {

    val producer = createProducer()

    suspend fun newCounterValue(counter: Counter) {
        val village = getVillage(counter)

        // create consumption change record
        val change = VillageConsumptionChange(village, counter.amount)
        producer.send(ProducerRecord(consumptionChangeTopic, jacksonObjectMapper().writeValueAsString(change)))

        // create consumption outdated record
        val outdate = VillageConsumptionOutdate(village, counter.amount, counter.timestamp!! + 100000)
        producer.send(ProducerRecord(outdatedConsumptionsTopic, jacksonObjectMapper().writeValueAsString(outdate)))
    }

    suspend fun getVillage(counter: Counter): String {
        return "hahaha"
    }

    fun createProducer(): Producer<Long, String> {
        val props = Properties()
        props.put("bootstrap.servers", kafkaBootstrapServers)
        props.put("key.serializer", LongSerializer::class.qualifiedName)
        props.put("value.serializer", StringSerializer::class.qualifiedName)

        return KafkaProducer<Long, String>(props)
    }

}