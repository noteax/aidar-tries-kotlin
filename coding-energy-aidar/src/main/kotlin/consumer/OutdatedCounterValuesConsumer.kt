package consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import model.commands.VillageConsumptionOutdate
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import service.outdatedConsumptionsTopic
import java.util.*

class OutdatedCounterValuesConsumer(val kafkaBootstrapServers: String) {

    fun run() = runBlocking(CommonPool) {
        val consumer = createConsumer()
        while (true) {
            val records = consumer.poll(500)

            val latestOffsets = records.partitions()
                    .map { partition -> records.records(partition) }
                    .map { partitionRecords -> async(CommonPool) { processSequentually(partitionRecords) } }
                    .mapNotNull { processingResultAsync -> processingResultAsync.await() }
                    .map { Pair(TopicPartition(outdatedConsumptionsTopic, it.first), OffsetAndMetadata(it.second)) }
                    .toMap()

            consumer.commitSync(latestOffsets)
        }
    }

    fun processSequentually(records: List<ConsumerRecord<Long, String>>): Pair<Int, Long>? {
        val processedRecordOffsets = records
                .map { record -> Pair(record, processRecord(record.value())) }
                .takeLastWhile { it.second != null }
                .map { Pair(it.first.partition(), it.first.offset()) }
                .firstOrNull()

    }

    fun processRecord(record: String): Long? {
        // TODO exception handling here
        val outdate = jacksonObjectMapper().readValue<VillageConsumptionOutdate>(record)
        val timeToWait = outdate.outdatedTime - System.currentTimeMillis()
        if (timeToWait <= 0) {
            // send to commands consumer
            return null
        }
        return timeToWait
    }

    fun createConsumer(): KafkaConsumer<Long, String> {
        val props = Properties()
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "group-id-1")
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer::class.qualifiedName)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.qualifiedName)

        val consumer = KafkaConsumer<Long, String>(props)
        consumer.subscribe(listOf(outdatedConsumptionsTopic))
        return consumer
    }

}

