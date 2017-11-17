package domain.rent.actors

data class Landlord(val id: Long,
                    val name: String,
                    val dwellings: List<Long>)