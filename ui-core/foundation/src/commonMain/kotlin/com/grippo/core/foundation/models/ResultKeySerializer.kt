package com.grippo.core.foundation.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object ResultKeySerializer : KSerializer<ResultKey<*>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ResultKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ResultKey<*>) {
        encoder.encodeString(value.key)
    }

    override fun deserialize(decoder: Decoder): ResultKey<*> = ResultKey<BaseResult>(decoder.decodeString())
}
