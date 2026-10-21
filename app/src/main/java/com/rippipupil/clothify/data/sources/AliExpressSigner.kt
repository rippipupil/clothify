package com.rippipupil.clothify.data.sources

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Firma de la Open Platform de AliExpress: HMAC-SHA256 de "nombre+valor" ordenados por nombre. */
object AliExpressSigner {
    fun sign(params: Map<String, String>, secret: String): String {
        val payload = params.toSortedMap().entries.joinToString("") { (k, v) -> k + v }
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(), "HmacSHA256")) }
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02X".format(it) }
    }
}
