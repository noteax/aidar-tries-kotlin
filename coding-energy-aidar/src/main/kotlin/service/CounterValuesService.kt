package service

import model.Village
import java.util.concurrent.ConcurrentHashMap

class CounterValuesService {

    val villageValues = ConcurrentHashMap<String, Double>()

    suspend fun getValue(counterId: Long): Double {
        return 12.2
    }

    suspend fun getVillageConsumtion(villageName: String): Double {
        return 123.2
    }

    fun change(villageName: String, change: Double) {
        val previous = villageValues.computeIfAbsent(villageName, { _ -> 0.0 })
        villageValues.put(villageName, previous + change)
    }

    suspend fun getVillages(): List<Village> {
        return villageValues.map { Village(it.key, it.value) }
    }


}