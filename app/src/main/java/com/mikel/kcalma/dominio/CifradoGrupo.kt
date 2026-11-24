package com.mikel.kcalma.dominio

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado de las cifras que se comparten con un grupo de amigos. La clave sale del código del grupo, así que lo que
 * se publica en el repositorio (que es público) solo lo puede leer, y solo lo puede falsificar, quien tiene el código.
 */
object CifradoGrupo {
    /** Sin O, 0, I, 1 ni L: no se confunden al dictarlo o copiarlo. */
    private const val LETRAS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val PREFIJO = "KCAL"

    /** Un código nuevo, tipo KCAL-7Q2M-X9PD-4HRT (12 caracteres al azar, unos 59 bits). */
    fun codigoNuevo(azar: SecureRandom = SecureRandom()): String =
        PREFIJO + "-" + (1..3).joinToString("-") { (1..4).map { LETRAS[azar.nextInt(LETRAS.length)] }.joinToString("") }

    /** El código sin guiones ni espacios y en mayúsculas, o null si no tiene la forma de un código de Kcalma. */
    fun normalizar(codigo: String): String? {
        val limpio = codigo.uppercase().filter { it.isLetterOrDigit() }
        return limpio.takeIf { it.startsWith(PREFIJO) && it.length == PREFIJO.length + 12 && it.drop(PREFIJO.length).all { c -> c in LETRAS } }
    }

    /** Como se enseña: KCAL-XXXX-XXXX-XXXX. */
    fun bonito(normalizado: String): String = normalizado.chunked(4).joinToString("-")

    /** Identificador público del grupo (nombre de su archivo en el repositorio). No permite deducir el código. */
    fun id(normalizado: String): String =
        MessageDigest.getInstance("SHA-256").digest("kcalma-grupo:$normalizado".toByteArray()).take(8).joinToString("") { "%02x".format(it) }

    private fun clave(normalizado: String): SecretKeySpec {
        val spec = PBEKeySpec(normalizado.toCharArray(), "kcalma:${id(normalizado)}".toByteArray(), 20_000, 256)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }

    /** Clave ya derivada, para no repetir el cálculo lento con cada miembro. */
    class Llave internal constructor(internal val clave: SecretKeySpec)

    fun llave(normalizado: String) = Llave(clave(normalizado))

    fun cifrar(llave: Llave, texto: String, azar: SecureRandom = SecureRandom()): String {
        val iv = ByteArray(12).also { azar.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, llave.clave, GCMParameterSpec(128, iv)) }
        return Base64.getEncoder().encodeToString(iv + c.doFinal(texto.toByteArray()))
    }

    /** null si no se puede descifrar: otro código, datos rotos o manipulados. */
    fun descifrar(llave: Llave, cifrado: String): String? = runCatching {
        val bytes = Base64.getDecoder().decode(cifrado)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, llave.clave, GCMParameterSpec(128, bytes, 0, 12)) }
        String(c.doFinal(bytes, 12, bytes.size - 12))
    }.getOrNull()
}
