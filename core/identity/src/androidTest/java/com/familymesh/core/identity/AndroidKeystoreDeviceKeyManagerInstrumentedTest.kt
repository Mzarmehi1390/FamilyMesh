package com.familymesh.core.identity

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigInteger
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreDeviceKeyManagerInstrumentedTest {
    private lateinit var context: Context
    private lateinit var manager: AndroidKeystoreDeviceKeyManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        clearIdentityState()
        manager = AndroidKeystoreDeviceKeyManager(context)
    }

    @After
    fun tearDown() {
        clearIdentityState()
    }

    @Test
    fun unprovisionedInstallation_hasNoIdentityAndDoesNotCreateAlias() = runBlocking {
        assertNull(manager.getPublicIdentity())
        assertFalse(keyStore().containsAlias(KEY_ALIAS))
    }

    @Test
    fun createIdentity_persistsStablePublicIdentityAndNonExportablePrivateKey() = runBlocking {
        val created = manager.createPublicIdentity()
        val loaded = manager.getPublicIdentity()

        assertEquals(created, loaded)
        assertTrue(keyStore().containsAlias(KEY_ALIAS))
        val privateKey = keyStore().getKey(KEY_ALIAS, null) as? PrivateKey
        assertNotNull(privateKey)
        assertNull("Android Keystore private key must not be exportable", privateKey!!.encoded)
    }

    @Test
    fun sign_usesKeystorePrivateKeyAndVerifiesAgainstPublicIdentity() = runBlocking {
        val identity = manager.createPublicIdentity()
        val message = "FamilyMesh Keystore instrumentation".toByteArray()
        val signatureBytes = manager.sign(message)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(identity.publicKeyBytes)
        )
        val verifier = Signature.getInstance("SHA256withECDSA")

        verifier.initVerify(publicKey)
        verifier.update(message)

        assertTrue(verifier.verify(signatureBytes))
        val privateKey = keyStore().getKey(KEY_ALIAS, null) as? PrivateKey
        assertNotNull(privateKey)
        assertNull("Signing must not make private key bytes exportable", privateKey!!.encoded)
    }

    @Test
    fun createIdentity_refusesToReplaceExistingKey() = runBlocking {
        val original = manager.createPublicIdentity()
        val failure = runCatching { manager.createPublicIdentity() }.exceptionOrNull()

        assertTrue(
            "Expected IdentityAlreadyExists, got $failure",
            failure is DeviceKeyManagerException.IdentityAlreadyExists,
        )
        assertEquals(original, manager.getPublicIdentity())
    }

    @Test
    fun missingKeystoreAlias_afterProvisioningRequiresRecovery() = runBlocking {
        manager.createPublicIdentity()
        keyStore().deleteEntry(KEY_ALIAS)

        val failure = runCatching { manager.getPublicIdentity() }.exceptionOrNull()

        assertTrue(
            "A persisted marker with a missing alias must fail closed",
            failure is DeviceKeyManagerException.KeyMaterialMissing,
        )
        val createFailure = runCatching { manager.createPublicIdentity() }.exceptionOrNull()
        assertTrue(
            "Missing key material must not trigger silent key replacement",
            createFailure is DeviceKeyManagerException.KeyMaterialMissing,
        )
        assertFalse(keyStore().containsAlias(KEY_ALIAS))
    }

    @Test
    fun missingPreferenceMarker_withExistingAliasFailsClosed() = runBlocking {
        manager.createPublicIdentity()
        val saved = preferences().edit().remove(PREFERENCE_KEY_ID).commit()
        assertTrue(saved)

        val failure = runCatching { manager.getPublicIdentity() }.exceptionOrNull()

        assertTrue(
            "An existing alias without its marker is inconsistent state",
            failure is DeviceKeyManagerException.CorruptKeyMaterial,
        )
    }

    @Test
    fun mismatchedPreferenceMarker_failsClosed() = runBlocking {
        manager.createPublicIdentity()
        val saved = preferences().edit()
            .putString(PREFERENCE_KEY_ID, "not-the-current-key")
            .commit()
        assertTrue(saved)

        val failure = runCatching { manager.getPublicIdentity() }.exceptionOrNull()

        assertTrue(
            "A mismatched key fingerprint marker must be rejected",
            failure is DeviceKeyManagerException.CorruptKeyMaterial,
        )
    }

    @Test
    fun securityLevel_reportsRecognizedKeystoreProtectionWithoutAssumingHardware() = runBlocking {
        manager.createPublicIdentity()

        val level = manager.getSecurityLevel()

        assertTrue(
            "Android Keystore should report software, TEE, or StrongBox protection",
            level == DeviceKeySecurityLevel.SOFTWARE ||
                level == DeviceKeySecurityLevel.TRUSTED_ENVIRONMENT ||
                level == DeviceKeySecurityLevel.STRONGBOX,
        )
    }

    @Test
    fun keystoreGeneratedPublicKey_isAcceptedBySharedP256Parser() = runBlocking {
        val identity = manager.createPublicIdentity()

        assertNotNull(P256PublicKeyParser.decode(identity.publicKeyBytes))
    }

    @Test
    fun sharedP256Parser_rejectsOffCurvePointOnAndroidProvider() = runBlocking {
        val identity = manager.createPublicIdentity()
        val invalidPoint = identity.publicKeyBytes.withUncompressedPoint(
            BigInteger.ONE,
            BigInteger.ONE,
        )

        assertNull(P256PublicKeyParser.decode(invalidPoint))
    }

    @Test
    fun sharedP256Parser_rejectsOutOfFieldCoordinateOnAndroidProvider() = runBlocking {
        val identity = manager.createPublicIdentity()
        val invalidPoint = identity.publicKeyBytes.withUncompressedPoint(
            P256_FIELD_PRIME,
            BigInteger.ONE,
        )

        assertNull(P256PublicKeyParser.decode(invalidPoint))
    }

    @Test
    fun sharedP256Parser_rejectsTrailingBytesOnAndroidProvider() = runBlocking {
        val encoded = manager.createPublicIdentity().publicKeyBytes
        assertNull(P256PublicKeyParser.decode(encoded + byteArrayOf(0x00)))
    }

    @Test
    fun sharedP256Parser_rejectsNonMinimalDerLengthOnAndroidProvider() = runBlocking {
        val encoded = manager.createPublicIdentity().publicKeyBytes
        val contentLength = encoded[1].toInt() and 0xff
        assertTrue("P-256 SPKI fixture should use a short-form DER length", contentLength < 128)
        val nonCanonical = byteArrayOf(encoded[0], 0x81.toByte(), contentLength.toByte()) +
            encoded.copyOfRange(2, encoded.size)

        assertNull(P256PublicKeyParser.decode(nonCanonical))
    }

    @Test
    fun concurrentIdentityCreationAcrossManagerInstances_hasSingleWinner() = runBlocking {
        val secondManager = AndroidKeystoreDeviceKeyManager(context)
        val start = CompletableDeferred<Unit>()
        val attempts = coroutineScope {
            val pending = listOf(manager, secondManager).map { candidate ->
                async(Dispatchers.IO) {
                    start.await()
                    runCatching { candidate.createPublicIdentity() }
                }
            }
            start.complete(Unit)
            pending.awaitAll()
        }

        assertEquals(1, attempts.count { it.isSuccess })
        assertEquals(
            1,
            attempts.count {
                it.exceptionOrNull() is DeviceKeyManagerException.IdentityAlreadyExists
            },
        )
        val winner = attempts.single { it.isSuccess }.getOrThrow()
        assertEquals(winner, manager.getPublicIdentity())
        assertEquals(winner, secondManager.getPublicIdentity())
    }

    @Test
    fun readDuringProvisioning_neverObservesPartiallyWrittenIdentityMarker() = runBlocking {
        val secondManager = AndroidKeystoreDeviceKeyManager(context)
        val start = CompletableDeferred<Unit>()
        val creation = async(Dispatchers.IO) {
            start.await()
            manager.createPublicIdentity()
        }
        val observation = async(Dispatchers.IO) {
            start.await()
            runCatching { secondManager.getPublicIdentity() }
        }
        start.complete(Unit)

        val created = creation.await()
        val observed = observation.await().getOrThrow()
        assertTrue(observed == null || observed == created)
        assertEquals(created, secondManager.getPublicIdentity())
    }

    @Test
    fun sharedP256Parser_rejectsPointAtInfinityOnAndroidProvider() {
        val encoded = p256SubjectPublicKeyInfo(byteArrayOf(0x00))
        assertNull(P256PublicKeyParser.decode(encoded))
    }

    private fun p256SubjectPublicKeyInfo(point: ByteArray): ByteArray {
        val algorithmIdentifier = hexBytes("301306072a8648ce3d020106082a8648ce3d030107")
        val bitString = der(0x03, byteArrayOf(0x00) + point)
        return der(0x30, algorithmIdentifier + bitString)
    }

    private fun hexBytes(value: String): ByteArray =
        ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }

    private fun der(tag: Int, content: ByteArray): ByteArray =
        byteArrayOf(tag.toByte()) + derLength(content.size) + content

    private fun derLength(length: Int): ByteArray {
        if (length < 128) return byteArrayOf(length.toByte())
        val byteCount = (32 - Integer.numberOfLeadingZeros(length) + 7) / 8
        val encoded = ByteArray(byteCount) { index ->
            (length ushr ((byteCount - index - 1) * 8)).toByte()
        }
        return byteArrayOf((0x80 or byteCount).toByte()) + encoded
    }

    private fun ByteArray.withUncompressedPoint(x: BigInteger, y: BigInteger): ByteArray {
        val pointOffset = size - UNCOMPRESSED_POINT_SIZE
        assertEquals(UNCOMPRESSED_POINT_PREFIX, this[pointOffset].toInt() and 0xff)
        return copyOf().apply {
            writeCoordinate(x, pointOffset + 1)
            writeCoordinate(y, pointOffset + 1 + COORDINATE_SIZE)
        }
    }

    private fun ByteArray.writeCoordinate(value: BigInteger, offset: Int) {
        require(value.signum() >= 0)
        val encoded = value.toByteArray()
        val unsigned = if (encoded.size == COORDINATE_SIZE + 1 && encoded[0] == 0.toByte()) {
            encoded.copyOfRange(1, encoded.size)
        } else {
            encoded
        }
        require(unsigned.size <= COORDINATE_SIZE)
        fill(0.toByte(), offset, offset + COORDINATE_SIZE)
        System.arraycopy(
            unsigned,
            0,
            this,
            offset + COORDINATE_SIZE - unsigned.size,
            unsigned.size,
        )
    }

    private fun preferences() =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun clearIdentityState() {
        val store = keyStore()
        if (store.containsAlias(KEY_ALIAS)) {
            store.deleteEntry(KEY_ALIAS)
        }
        check(preferences().edit().clear().commit()) {
            "Could not clear the instrumentation identity marker"
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "familymesh.device.signing.v1"
        const val PREFERENCES_NAME = "familymesh_device_identity"
        const val PREFERENCE_KEY_ID = "device_key_id_v1"
        const val UNCOMPRESSED_POINT_PREFIX = 0x04
        const val UNCOMPRESSED_POINT_SIZE = 65
        const val COORDINATE_SIZE = 32
        val P256_FIELD_PRIME = BigInteger(
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",
            16,
        )
    }
}
