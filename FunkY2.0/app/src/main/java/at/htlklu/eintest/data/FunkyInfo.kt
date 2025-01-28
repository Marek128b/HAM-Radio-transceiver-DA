package at.htlklu.eintest.data

import kotlinx.serialization.Serializable

@Serializable
data class FunkyInfo(
    val getFrequency: Boolean,
    val getVoltage: Boolean,
    val getName: Boolean,
    val getCall: Boolean,
    val getTemperature: Boolean
)
