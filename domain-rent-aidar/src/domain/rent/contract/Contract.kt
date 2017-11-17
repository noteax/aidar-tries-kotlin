package domain.rent.contract

data class Contract(val id: Long,
                    val landLord: Long,
                    val tenant: Long,
                    val dwellings: List<Long>)