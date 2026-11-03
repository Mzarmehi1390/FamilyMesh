package com.familymesh.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.familymesh.core.common.domain.DeviceEnrollmentRequest
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.identity.DeviceEnrollmentProofAcceptanceService
import com.familymesh.core.identity.DeviceEnrollmentProofSigner
import com.familymesh.core.identity.DeviceMessageSigner
import com.familymesh.core.identity.EnrollmentProofAcceptanceResult
import com.familymesh.core.identity.EnrollmentProofTextCodec
import java.util.UUID
import androidx.compose.ui.unit.dp
import com.familymesh.core.common.domain.FamilySpace
import com.familymesh.core.common.domain.FamilySpaceRepository
import com.familymesh.core.storage.LocalProfileRepository
import com.familymesh.core.identity.DeviceKeyManager
import com.familymesh.core.identity.DeviceKeyManagerException
import com.familymesh.core.identity.DeviceKeySecurityInfoProvider
import com.familymesh.core.identity.DeviceKeySecurityLevel
import com.familymesh.core.identity.DevicePublicIdentity
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun FamilyMeshHomeScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val profileRepository: LocalProfileRepository = koinInject()
    val familySpaceRepository: FamilySpaceRepository = koinInject()
    val deviceKeyManager: DeviceKeyManager = koinInject()
    val keySecurityInfoProvider: DeviceKeySecurityInfoProvider = koinInject()
    val messageSigner: DeviceMessageSigner = koinInject()
    val proofAcceptanceService: DeviceEnrollmentProofAcceptanceService = koinInject()
    val clipboardManager = LocalClipboardManager.current
    val profile by profileRepository.profile.collectAsState(initial = null)
    val familySpaces by familySpaceRepository.observeFamilySpaces().collectAsState(initial = emptyList())
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }
    var familyName by remember { mutableStateOf("") }
    var deviceIdentity by remember { mutableStateOf<DevicePublicIdentity?>(null) }
    var identityError by remember { mutableStateOf<String?>(null) }
    var keySecurityLevel by remember { mutableStateOf<DeviceKeySecurityLevel?>(null) }
    var isCreatingIdentity by remember { mutableStateOf(false) }
    var selectedFamilySpaceId by remember { mutableStateOf("") }
    var generatedProofText by remember { mutableStateOf("") }
    var importedProofText by remember { mutableStateOf("") }
    var proofStatus by remember { mutableStateOf<String?>(null) }
    var isProcessingProof by remember { mutableStateOf(false) }

    LaunchedEffect(familySpaces) {
        if (selectedFamilySpaceId.isBlank() && familySpaces.isNotEmpty()) {
            selectedFamilySpaceId = familySpaces.first().id
        } else if (familySpaces.none { it.id == selectedFamilySpaceId }) {
            selectedFamilySpaceId = familySpaces.firstOrNull()?.id.orEmpty()
        }
    }

    LaunchedEffect(deviceKeyManager) {
        try {
            deviceIdentity = deviceKeyManager.getPublicIdentity()
            identityError = null
            keySecurityLevel = if (deviceIdentity != null) {
                runCatching { keySecurityInfoProvider.getSecurityLevel() }.getOrNull()
            } else null
        } catch (failure: DeviceKeyManagerException) {
            identityError = when (failure) {
                is DeviceKeyManagerException.KeyInvalidated,
                is DeviceKeyManagerException.KeyMaterialMissing,
                is DeviceKeyManagerException.CorruptKeyMaterial -> "This device identity needs recovery. It will not be replaced automatically."
                else -> "Device identity storage is temporarily unavailable."
            }
        } catch (_: Exception) {
            identityError = "Could not read this device's signing identity."
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(contentPadding).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FamilyMesh", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("A private space for your family", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This device's profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(40) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Display name") },
                        singleLine = true,
                        supportingText = { Text("Stored only on this device · Up to 40 characters") }
                    )
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                runCatching { profileRepository.updateDisplayName(displayName) }
                                    .onSuccess { snackbarHostState.showSnackbar("Display name saved on this device.") }
                                    .onFailure { snackbarHostState.showSnackbar("Enter a display name first.") }
                            }
                        },
                        enabled = profile != null && displayName.isNotBlank() && displayName.trim() != profile?.displayName,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save profile") }
                    Text(
                        text = if (profile == null) "Preparing local profile…" else "Local device ID: ${profile!!.deviceId.take(8)}…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Device signing identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (deviceIdentity != null) {
                        Text("Public key ID: ${deviceIdentity!!.keyId.take(16)}…", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Key protection: " + when (keySecurityLevel) {
                                DeviceKeySecurityLevel.SOFTWARE -> "Software-backed"
                                DeviceKeySecurityLevel.TRUSTED_ENVIRONMENT -> "Trusted environment"
                                DeviceKeySecurityLevel.STRONGBOX -> "StrongBox"
                                DeviceKeySecurityLevel.UNKNOWN, null -> "Unknown"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "This proves possession of a device key only. It does not enroll this device in a family.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            identityError ?: "No signing identity has been created on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                isCreatingIdentity = true
                                coroutineScope.launch {
                                    try {
                                        deviceIdentity = deviceKeyManager.createPublicIdentity()
                                        identityError = null
                                        keySecurityLevel = runCatching {
                                            keySecurityInfoProvider.getSecurityLevel()
                                        }.getOrNull()
                                        snackbarHostState.showSnackbar("Device signing identity created locally.")
                                    } catch (failure: DeviceKeyManagerException) {
                                        identityError = when (failure) {
                                            is DeviceKeyManagerException.IdentityAlreadyExists -> "A device identity already exists. Refresh the app state before continuing."
                                            is DeviceKeyManagerException.KeyMaterialMissing,
                                            is DeviceKeyManagerException.KeyInvalidated,
                                            is DeviceKeyManagerException.CorruptKeyMaterial -> "Device identity recovery is required. No replacement was created."
                                            else -> "Could not create a device signing identity."
                                        }
                                        snackbarHostState.showSnackbar(identityError!!)
                                    } catch (_: Exception) {
                                        identityError = "Could not create a device signing identity."
                                        snackbarHostState.showSnackbar(identityError!!)
                                    } finally {
                                        isCreatingIdentity = false
                                    }
                                }
                            },
                            enabled = !isCreatingIdentity && identityError == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isCreatingIdentity) "Creating identity…" else "Create device signing identity")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Device enrollment proof", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Create a short-lived, signed MEMBER request for a family space on this device. Share the code with the family administrator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (familySpaces.isEmpty()) {
                        Text("Create a family space first. This device can only prepare a request for a locally known family-space ID.")
                    } else {
                        Text("Choose family space", style = MaterialTheme.typography.labelLarge)
                        familySpaces.forEach { space ->
                            OutlinedButton(
                                onClick = { selectedFamilySpaceId = space.id; generatedProofText = ""; proofStatus = null },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessingProof
                            ) {
                                Text(if (selectedFamilySpaceId == space.id) "✓ ${space.name}" else space.name)
                            }
                        }
                        Button(
                            onClick = {
                                val identity = deviceIdentity
                                if (identity == null) {
                                    proofStatus = "Create this device's signing identity first."
                                } else {
                                    isProcessingProof = true
                                    proofStatus = null
                                    coroutineScope.launch {
                                        try {
                                            val now = System.currentTimeMillis().coerceAtLeast(0L)
                                            val request = DeviceEnrollmentRequest(
                                                id = UUID.randomUUID().toString(),
                                                familySpaceId = selectedFamilySpaceId,
                                                deviceKeyId = identity.keyId,
                                                requestedRole = FamilyRole.MEMBER,
                                                createdAtEpochMillis = now,
                                                expiresAtEpochMillis = now + 15L * 60L * 1000L
                                            )
                                            val proof = DeviceEnrollmentProofSigner(messageSigner).sign(request, identity)
                                            generatedProofText = EnrollmentProofTextCodec.encode(proof, identity)
                                            proofStatus = "Signed request created. It expires in 15 minutes."
                                        } catch (_: Exception) {
                                            proofStatus = "Could not create the signed request. Check the device identity and try again."
                                        } finally {
                                            isProcessingProof = false
                                        }
                                    }
                                }
                            },
                            enabled = deviceIdentity != null && selectedFamilySpaceId.isNotBlank() && !isProcessingProof,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (isProcessingProof) "Creating request…" else "Create signed request") }
                    }
                    if (generatedProofText.isNotBlank()) {
                        OutlinedTextField(
                            value = generatedProofText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Share this enrollment code") },
                            minLines = 3
                        )
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(generatedProofText))
                                proofStatus = "Enrollment code copied."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Copy enrollment code") }
                    }
                    Text("Receive a request", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = importedProofText,
                        onValueChange = { importedProofText = it.take(6000) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Paste enrollment code") },
                        minLines = 3,
                        enabled = !isProcessingProof
                    )
                    OutlinedButton(
                        onClick = {
                            isProcessingProof = true
                            proofStatus = null
                            coroutineScope.launch {
                                try {
                                    val decoded = EnrollmentProofTextCodec.decode(importedProofText.trim())
                                    val localFamilyExists = familySpaces.any { it.id == decoded.proof.familySpaceId }
                                    if (!localFamilyExists) {
                                        proofStatus = "This code refers to a family space not saved on this device."
                                    } else if (decoded.proof.requestedRole != FamilyRole.MEMBER) {
                                        proofStatus = "Only MEMBER requests are accepted by this preview."
                                    } else {
                                        when (proofAcceptanceService.accept(
                                            proof = decoded.proof,
                                            identity = decoded.identity,
                                            nowEpochMillis = System.currentTimeMillis().coerceAtLeast(0L)
                                        )) {
                                            EnrollmentProofAcceptanceResult.Accepted ->
                                                proofStatus = "Valid signed request received. It is pending family-owner approval; no membership was added."
                                            EnrollmentProofAcceptanceResult.InvalidOrExpired ->
                                                proofStatus = "The signature is invalid or this request has expired."
                                            EnrollmentProofAcceptanceResult.ReplayDetected ->
                                                proofStatus = "This request code has already been used on this device."
                                            EnrollmentProofAcceptanceResult.NonceStoreUnavailable ->
                                                proofStatus = "Could not safely record this request. Please try again."
                                        }
                                    }
                                } catch (_: Exception) {
                                    proofStatus = "The enrollment code is malformed or could not be verified."
                                } finally {
                                    isProcessingProof = false
                                }
                            }
                        },
                        enabled = importedProofText.isNotBlank() && !isProcessingProof,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isProcessingProof) "Checking request…" else "Verify received request") }
                    proofStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "A valid signature proves control of the device key only. This preview does not authenticate the sender, approve membership, or synchronize replay protection between devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your family spaces", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = if (familySpaces.isEmpty()) {
                            "No family spaces are saved on this device yet."
                        } else {
                            "${familySpaces.size} family space(s) saved locally on this device."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), shape = MaterialTheme.shapes.small) {
                        Text(
                            if (familySpaces.isEmpty()) "NO LOCAL SPACE" else "LOCAL ONLY",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    familySpaces.forEach { space -> FamilySpaceRow(space) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Create a family space", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it.take(FamilySpace.MAX_NAME_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Family space name") },
                    supportingText = { Text("1–60 characters · Saved only on this device") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        coroutineScope.launch {
                            runCatching {
                                familySpaceRepository.createFamilySpace(
                                    name = familyName,
                                    createdAtEpochMillis = System.currentTimeMillis()
                                )
                            }.onSuccess {
                                familyName = ""
                                snackbarHostState.showSnackbar("Family space saved on this device.")
                            }.onFailure {
                                snackbarHostState.showSnackbar("Could not create the family space. Check its name and try again.")
                            }
                        }
                    },
                    enabled = profile != null && familyName.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create family space") }
                OutlinedButton(
                    onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("Joining with an invitation is not available yet.") } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join with an invitation") }
            }

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Privacy by design", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    InfoRow("Platform", "Android 12 and newer")
                    InfoRow("Storage", "Family spaces saved locally")
                    InfoRow("Network", "No connection established")
                    InfoRow("Encryption", "Not configured yet")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Early preview · Local family-space storage only",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FamilySpaceRow(space: FamilySpace) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(space.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text("Local ID: ${space.id.take(8)}…", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
