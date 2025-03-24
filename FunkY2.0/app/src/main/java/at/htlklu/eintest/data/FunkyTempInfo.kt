package at.htlklu.eintest.data

import at.htlklu.eintest.MainActivity
import kotlinx.serialization.Serializable

@Serializable
data class FunkyTempInfo(
    var op: Boolean,
    val temperature: Float
)
