package com.example.wellbeing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Health permission rationale must remain accessible without onboarding or DB access. */
class HealthPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Velnumi · Health Connect", style = MaterialTheme.typography.headlineMedium)
                        Text("Your health data, on your phone", style = MaterialTheme.typography.titleLarge)
                        Text("Steps and sleep are separate, optional read permissions. Velnumi reads only the types you grant when you start a sync. It does not write to Health Connect or read heart rate, location or contacts.")
                        Text("Imported readings are encrypted locally using an Android Keystore key. They appear in your personal daily overview and Devices and data screen with source timestamps. Food intake is never changed by device activity.")
                        Text("No Health Connect data is uploaded to a server, shared with an AI provider, used for ads or sold by this Android build. It has no Internet permission. A fitness source may have its own data practices outside Velnumi.")
                        Text("Revoke access in Health Connect settings to prevent new reads. Remove saved Velnumi copies with Delete imported health data in Devices and data, or delete all local data in Profile and privacy. This does not delete source records or files you previously exported.")
                        Text("Exports are plain JSON at the location you choose. Protect those files separately. Backups and device transfer are disabled. Clearing app storage or uninstalling removes the local journal.")
                        Text("Velnumi is a wellness organizer, not diagnosis, clinical monitoring or an emergency service. Missing or old readings do not establish that you are healthy.")
                        Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Close") }
                    }
                }
            }
        }
    }
}
