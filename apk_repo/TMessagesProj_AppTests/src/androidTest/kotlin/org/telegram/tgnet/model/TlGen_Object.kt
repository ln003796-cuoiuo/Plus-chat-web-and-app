package org.pluschat.tgnet.model

import org.pluschat.tgnet.OutputSerializedData

public interface TlGen_Object {
    fun serializeToStream(stream: OutputSerializedData)
}