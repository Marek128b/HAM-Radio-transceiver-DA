package at.htlklu.eintest.data

import kotlinx.serialization.Serializable

@Serializable
data class FunkyInfo(
    var op: Boolean,
    var frequency: Float,
    var voltage: Float,
    var name: String,
    var call: String,
    var temperature: Float
)