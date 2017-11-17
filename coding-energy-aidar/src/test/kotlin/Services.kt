import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.assertj.core.api.Assertions.*

data class TestCounterValue(val id: Long, val timestamp: Long, val amount: Double)
data class TestVillagesReport(val villages: List<TestVillageReport>)
data class TestVillageReport(val name: String, val amount: Double)

data class TestCounterInfo(val id: Long, val village: String)

fun getClient(): Client {
    val applicationAddress = System.getProperty("applciationAddress", "localhost:8080")
    val externalServiceAddress = System.getProperty("externalServiceAddress", "localhost:8081")
    return Client(applicationAddress, externalServiceAddress)
}

class Client(val applicationAddress: String, val externalServiceAddress: String) {
    var client = OkHttpClient()

    val jsonHeader = "application/json; charset=utf-8"

    fun setVillageForCounter(counterInfo: TestCounterInfo) {
        val body = RequestBody.create(MediaType.parse(jsonHeader), jacksonObjectMapper().writeValueAsString(counterInfo))
        val execution = client.newCall(
                Request.Builder()
                        .url("http://$externalServiceAddress/counter")
                        .post(body)
                        .build()
        ).execute()
        assertThat(execution.isSuccessful)
                .withFailMessage("Execution failed $execution")
                .isTrue()
    }

    fun doCounterCallback(counterValue: TestCounterValue) {
        val body = RequestBody.create(MediaType.parse(jsonHeader), jacksonObjectMapper().writeValueAsString(counterValue))
        val execution = client.newCall(
                Request.Builder()
                        .url("http://$applicationAddress/counter_callback")
                        .post(body)
                        .build()
        ).execute()
        assertThat(execution.isSuccessful)
                .withFailMessage("Execution failed $execution")
                .isTrue()
    }

    fun getVillagesReport(): TestVillagesReport {
        val execution = client.newCall(
                Request.Builder()
                        .addHeader("Accept", jsonHeader)
                        .url("http://$applicationAddress/consumption_report?duration=24h")
                        .get()
                        .build()
        ).execute()
        assertThat(execution.isSuccessful)
                .withFailMessage("Execution failed $execution")
                .isTrue()
        return jacksonObjectMapper().readValue(execution.body()!!.string())
    }

}