package dev.erolit.reader.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dev.erolit.reader.core.net.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES/GCM key held by the Android Keystore; ciphertext lives in app-private storage.
 * The plaintext session is never written to disk, logs, backups, or Room.
 */
class KeystoreSecretStore(context: Context) : SecretStore {
    private val file = File(context.filesDir, "session.vault")

    override suspend fun load(): SessionState? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching {
            val bytes = file.readBytes()
            if (bytes.size <= IV_LENGTH) return@runCatching null
            val iv = bytes.copyOfRange(0, IV_LENGTH)
            val ciphertext = bytes.copyOfRange(IV_LENGTH, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            val plain = cipher.doFinal(ciphertext)
            AppJson.decodeFromString<SessionState>(plain.toString(Charsets.UTF_8))
        }.onFailure {
            // A corrupt or invalidated vault is treated as signed out.
            file.delete()
        }.getOrNull()
    }

    override suspend fun save(state: SessionState) = withContext(Dispatchers.IO) {
        val plain = AppJson.encodeToString(state).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plain)
        val bytes = cipher.iv + ciphertext
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { file.delete() }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "erolit_session_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
