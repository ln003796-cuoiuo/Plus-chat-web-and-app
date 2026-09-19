package org.pluschat.tgnet.model.generated

import kotlin.UInt
import org.pluschat.tgnet.OutputSerializedData
import org.pluschat.tgnet.model.TlGen_Object
import org.pluschat.tgnet.model.TlGen_Vector

public sealed class TlGen_Null : TlGen_Object {
  public data object TL_null : TlGen_Null() {
    public const val MAGIC: UInt = 0x56730BCCU

    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
    }
  }
}
