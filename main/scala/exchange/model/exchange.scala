package exchange

import supertagged.TaggedType

package object model {

  object GenerationSignature extends TaggedType[Array[Byte]]
  object Value extends TaggedType[Long]
  object Nonce extends TaggedType[Long]
  object BaseTarget extends TaggedType[Long]

  type GenerationSignature = GenerationSignature.Type
  type Value = Value.Type
  type Nonce = Nonce.Type
  type BaseTarget = BaseTarget.Type

}
