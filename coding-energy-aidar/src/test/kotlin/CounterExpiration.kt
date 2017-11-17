import org.junit.Test

class CounterExpiration {

    val client = getClient()

    @Test
    fun testExpiration() {
        client.setVillageForCounter(TestCounterInfo(0, "Village0"))
        client.setVillageForCounter(TestCounterInfo(1, "Village0"))
        client.setVillageForCounter(TestCounterInfo(2, "Village0"))

        client.doCounterCallback(TestCounterValue(0, System.currentTimeMillis(), 15.25))
        client.doCounterCallback(TestCounterValue(1, System.currentTimeMillis(), 17.35))
        client.doCounterCallback(TestCounterValue(1, System.currentTimeMillis(), 18.38))
        client.doCounterCallback(TestCounterValue(1, System.currentTimeMillis(), 19.35))
        client.doCounterCallback(TestCounterValue(2, System.currentTimeMillis(), 30.20))

        // assert that values are corrected in some range

        val report = client.getVillagesReport()

        val target = 15.25 + 17.35 + 18.38 + 19.35 + 30.20
        println(target)
        println(report)

    }

}