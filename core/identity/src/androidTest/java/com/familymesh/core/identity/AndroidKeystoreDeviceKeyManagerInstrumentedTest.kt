package com.familymesh.core.identity

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
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
    }
}
