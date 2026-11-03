package com.familymesh.core.identity

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.UnrecoverableKeyException
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.security.auth.x500.X500Principal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android Keystore implementation for the proposed P-256 device signing key.
 *
 * The private key is never returned from this class. Public identity metadata
 * is derived from the Keystore certificate and checked against a local marker.
 */
class AndroidKeystoreDeviceKeyManager(
    context: Context,
) : DeviceKeyManager, DeviceMessageSigner, DeviceKeySecurityInfoProvider {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun getPublicIdentity(): DevicePublicIdentity? = withContext(Dispatchers.IO) {
        val keyStore = loadKeyStore()
        readIdentity(keyStore)
    }

    override suspend fun createPublicIdentity(): DevicePublicIdentity = withContext(Dispatchers.IO) {
        val keyStore = loadKeyStore()
        val marker = preferences.getString(PREFERENCE_KEY_ID, null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            throw DeviceKeyManagerException.IdentityAlreadyExists()
        }
        if (marker != null) {
            throw DeviceKeyManagerException.KeyMaterialMissing()
        }

        try {
            val now = System.currentTimeMillis().coerceAtLeast(1L)
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE,
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setCertificateSubject(X500Principal(CERTIFICATE_SUBJECT))
                .setCertificateSerialNumber(BigInteger.valueOf(now))
                .setCertificateNotBefore(Date(now))
                .setCertificateNotAfter(Date(CERTIFICATE_NOT_AFTER))
                .build()
            generator.initialize(spec)
            generator.generateKeyPair()

            val identity = readIdentity(keyStore, requireMarker = false)
                ?: throw DeviceKeyManagerException.KeyMaterialMissing()
            val saved = preferences.edit()
                .putString(PREFERENCE_KEY_ID, identity.keyId)
                .commit()
            if (!saved) {
                runCatching { keyStore.deleteEntry(KEY_ALIAS) }
                throw DeviceKeyManagerException.KeyStoreUnavailable()
            }
            identity
        } catch (failure: DeviceKeyManagerException) {
            throw failure
        } catch (failure: GeneralSecurityException) {
            runCatching { keyStore.deleteEntry(KEY_ALIAS) }
            throw DeviceKeyManagerException.UnsupportedKeyConfiguration(failure)
        } catch (failure: Exception) {
            runCatching { keyStore.deleteEntry(KEY_ALIAS) }
            throw DeviceKeyManagerException.KeyStoreUnavailable(failure)
        }
    }

    override suspend fun sign(message: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val keyStore = loadKeyStore()
        val identity = readIdentity(keyStore)
            ?: throw DeviceKeyManagerException.KeyMaterialMissing()
        if (identity.publicKeyBytes.isEmpty()) {
            throw DeviceKeyManagerException.CorruptKeyMaterial()
        }
        val privateKey = getPrivateKey(keyStore)
        try {
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initSign(privateKey)
                update(message)
                sign()
            }
        } catch (failure: KeyPermanentlyInvalidatedException) {
            throw DeviceKeyManagerException.KeyInvalidated(failure)
        } catch (failure: GeneralSecurityException) {
            throw DeviceKeyManagerException.KeyInvalidated(failure)
        }
    }

    override suspend fun getSecurityLevel(): DeviceKeySecurityLevel = withContext(Dispatchers.IO) {
        val keyStore = loadKeyStore()
        readIdentity(keyStore) ?: throw DeviceKeyManagerException.KeyMaterialMissing()
        val privateKey = getPrivateKey(keyStore)
        try {
            val keyInfo = KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEYSTORE)
                .getKeySpec(privateKey, KeyInfo::class.java)
            when (keyInfo.securityLevel) {
                KeyProperties.SECURITY_LEVEL_SOFTWARE -> DeviceKeySecurityLevel.SOFTWARE
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> DeviceKeySecurityLevel.TRUSTED_ENVIRONMENT
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> DeviceKeySecurityLevel.STRONGBOX
                else -> DeviceKeySecurityLevel.UNKNOWN
            }
        } catch (failure: GeneralSecurityException) {
            throw DeviceKeyManagerException.KeyStoreUnavailable(failure)
        }
    }

    private fun readIdentity(
        keyStore: KeyStore,
        requireMarker: Boolean = true,
    ): DevicePublicIdentity? {
        val marker = preferences.getString(PREFERENCE_KEY_ID, null)
        val aliasExists = keyStore.containsAlias(KEY_ALIAS)

        if (!aliasExists) {
            if (marker != null) throw DeviceKeyManagerException.KeyMaterialMissing()
            return null
        }
        if (requireMarker && marker == null) {
            throw DeviceKeyManagerException.CorruptKeyMaterial()
        }

        val certificate = keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
            ?: throw DeviceKeyManagerException.KeyMaterialMissing()
        val publicKey = certificate.publicKey
        if (!publicKey.algorithm.equals(KeyProperties.KEY_ALGORITHM_EC, ignoreCase = true)) {
            throw DeviceKeyManagerException.CorruptKeyMaterial()
        }

        val keyId = DeviceKeyFingerprint.fromEncodedPublicKey(publicKey.encoded)
        if (marker != null && marker != keyId) {
            throw DeviceKeyManagerException.CorruptKeyMaterial()
        }

        // initSign checks whether the private key is still usable without
        // exporting it or signing an application message.
        val privateKey = getPrivateKey(keyStore)
        try {
            Signature.getInstance(SIGNATURE_ALGORITHM).initSign(privateKey)
        } catch (failure: KeyPermanentlyInvalidatedException) {
            throw DeviceKeyManagerException.KeyInvalidated(failure)
        } catch (failure: GeneralSecurityException) {
            throw DeviceKeyManagerException.KeyInvalidated(failure)
        }

        return DevicePublicIdentity(
            keyId = keyId,
            publicKeyBytes = publicKey.encoded,
            createdAtEpochMillis = certificate.notBefore.time.coerceAtLeast(0L),
        )
    }

    private fun getPrivateKey(keyStore: KeyStore): PrivateKey {
        return try {
            keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
                ?: throw DeviceKeyManagerException.KeyMaterialMissing()
        } catch (failure: KeyPermanentlyInvalidatedException) {
            throw DeviceKeyManagerException.KeyInvalidated(failure)
        } catch (failure: UnrecoverableKeyException) {
            throw DeviceKeyManagerException.KeyInvalidated(failure)
        } catch (failure: DeviceKeyManagerException) {
            throw failure
        } catch (failure: Exception) {
            throw DeviceKeyManagerException.KeyStoreUnavailable(failure)
        }
    }

    private fun loadKeyStore(): KeyStore = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    } catch (failure: Exception) {
        throw DeviceKeyManagerException.KeyStoreUnavailable(failure)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "familymesh.device.signing.v1"
        const val PREFERENCES_NAME = "familymesh_device_identity"
        const val PREFERENCE_KEY_ID = "device_key_id_v1"
        const val CURVE = "secp256r1"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val CERTIFICATE_SUBJECT = "CN=FamilyMesh Device Signing Key"
        const val CERTIFICATE_NOT_AFTER = 4_102_444_800_000L // 2100-01-01 UTC
    }
}
