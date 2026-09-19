package org.pluschat.tgnet.model.generated

import kotlin.Long
import kotlin.String
import kotlin.UInt
import kotlin.collections.List
import org.pluschat.tgnet.OutputSerializedData
import org.pluschat.tgnet.model.TlGen_Object
import org.pluschat.tgnet.model.TlGen_Vector

public sealed class TlGen_StickerKeyword : TlGen_Object {
  public data class TL_stickerKeyword(
    public val document_id: Long,
    public val keyword: List<String>,
  ) : TlGen_StickerKeyword() {
    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt64(document_id)
      TlGen_Vector.serializeString(stream, keyword)
    }

    public companion object {
      public const val MAGIC: UInt = 0xFCFEB29CU
    }
  }
}
