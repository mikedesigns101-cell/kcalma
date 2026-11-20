package com.mikel.kcalma.datos

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda la clave de API del usuario cifrada con una clave AES del Android Keystore
 * (que nunca sale del chip seguro del móvil). La clave no va en copias de seguridad.
 */
class Claves(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("claves", Context.MODE_PRIVATE)

    fun gemini(): String? = prefs.getString("gemini", null)?.let { runCatching { descifrar(it) }.getOrNull() }

    fun guardarGemini(clave: String) = prefs.edit().putString("gemini", cifrar(clave.trim())).apply()

    fun borrarGemini() = prefs.edit().remove("gemini").apply()

    private fun llave(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    private fun cifrar(texto: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, llave()) }
        val datos = c.iv + c.doFinal(texto.toByteArray())
        return Base64.encodeToString(datos, Base64.NO_WRAP)
    }

    private fun descifrar(guardado: String): String {
        val datos = Base64.decode(guardado, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, datos, 0, 12))
        return String(c.doFinal(datos, 12, datos.size - 12))
    }

    private companion object { const val ALIAS = "kcalma_claves" }
}
