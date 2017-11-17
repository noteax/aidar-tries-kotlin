package domain.rent.dwellings

data class Apartment(override val id: Long, val building: Building, val number: String) : Dwelling