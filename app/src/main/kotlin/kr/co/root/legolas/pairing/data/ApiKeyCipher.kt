package kr.co.root.legolas.pairing.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object ApiKeyCipher {
    private const val KEY_ALIAS = "legolas_pairing_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        val encrypted = Base64.decode(value, Base64.NO_WRAP)
        require(encrypted.size > IV_SIZE_BYTES)

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_SIZE_BITS, encrypted.copyOfRange(0, IV_SIZE_BYTES)),
            )
        }
        return cipher.doFinal(encrypted.copyOfRange(IV_SIZE_BYTES, encrypted.size))
            .toString(StandardCharsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }
}
