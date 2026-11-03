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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familymesh.core.storage.LocalProfileRepository
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinInject

@Composable
fun FamilyMeshHomeScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val profileRepository: LocalProfileRepository = koinInject()
    val profile by profileRepository.profile.collectAsState(initial = null)
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }

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

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your family space", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("No family space connected yet. Create a new space or join one with an invitation when those features are available.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), shape = MaterialTheme.shapes.small) {
                        Text("NOT CONNECTED", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Get started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("Creating a family space is coming in a future milestone.") } },
                    modifier = Modifier.fillMaxWidth()) { Text("Create a family space") }
                OutlinedButton(onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("Joining with an invitation is coming in a future milestone.") } },
                    modifier = Modifier.fillMaxWidth()) { Text("Join with an invitation") }
            }

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Privacy by design", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    InfoRow("Platform", "Android 12 and newer")
                    InfoRow("Network", "No connection established")
                    InfoRow("Encryption", "Not configured yet")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Early preview · Core features are under development",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
