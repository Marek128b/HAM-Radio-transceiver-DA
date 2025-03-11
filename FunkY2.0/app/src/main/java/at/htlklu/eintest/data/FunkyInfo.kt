package at.htlklu.eintest.data

import kotlinx.serialization.Serializable

@Serializable
data class FunkyInfo(
    var op: Boolean,
    val frequency: Float,
    val voltage: Float,
    val name: String,
    val call: String,
    val temperature: Float
)
