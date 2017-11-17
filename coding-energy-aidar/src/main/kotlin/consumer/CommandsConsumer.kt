package consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import model.commands.VillageConsumptionChange
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import service.CounterValuesService
import service.consumptionChangeTopic
import java.util.*

class CommandsConsumer(val kafkaBootstrapServers: String, val counterValuesService: CounterValuesService) {

    fun run() = runBlocking(CommonPool) {
        val consumer = createConsumer()
        while (true) {
            val records = consumer.poll(500)

            val latestOffsets = records.partitions()
                    .map { partition -> records.records(partition) }
                    .map { partitionRecords -> async(CommonPool) { processSequentually(partitionRecords) } }
                    .mapNotNull { processingResultAsync -> processingResultAsync.await() }
                    .map { Pair(TopicPartition(consumptionChangeTopic, it.first), OffsetAndMetadata(it.second)) }
                    .toMap()

            consumer.commitSync(latestOffsets)
        }
    }

    fun processSequentually(records: List<ConsumerRecord<Long, String>>): Pair<Int, Long>? {
        return records
                .map { record -> processRecord(record.value()); record }
                .map { Pair(it.partition(), it.offset()) }
                .lastOrNull()
    }

    fun processRecord(record: String) {
        // TODO exception handling here
        val change = jacksonObjectMapper().readValue<VillageConsumptionChange>(record)
        counterValuesService.change(change.villageName, change.amount)
    }

    fun createConsumer(): KafkaConsumer<Long, String> {
        val props = Properties()
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "group-id-1")
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer::class.qualifiedName)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.qualifiedName)

        val consumer = KafkaConsumer<Long, String>(props)
        consumer.subscribe(listOf(consumptionChangeTopic))
        return consumer
    }

}

