package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.SweatyViewModel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.VioletNeon
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SweatyViewModel) {
    val prefs = viewModel.securePrefs
    val scrollState = rememberScrollState()

    var selectedLang by remember { mutableStateOf(prefs.language) }
    var selectedProvider by remember { mutableStateOf(prefs.selectedProvider) }
    var geminiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var openAiKey by remember { mutableStateOf(prefs.openAiApiKey) }
    var grokKey by remember { mutableStateOf(prefs.grokApiKey) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    var showGrokKey by remember { mutableStateOf(false) }

    var followUpEnabled by remember { mutableStateOf(prefs.activeFollowUpEnabled) }
    var speechRate by remember { mutableFloatStateOf(prefs.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(prefs.speechPitch) }

    var saveFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // 1. Language Preference
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "auto" to stringResource(R.string.lang_auto),
                        "en" to stringResource(R.string.lang_english),
                        "bn" to stringResource(R.string.lang_bengali)
                    ).forEach { (code, label) ->
                        val isSel = selectedLang == code
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedLang = code
                                prefs.language = code
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // 2. AI Provider Selection
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_ai_provider), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "gemini" to "Google Gemini (Flash)",
                        "openai" to "OpenAI (GPT-4o)",
                        "grok" to "xAI Grok-2"
                    ).forEach { (pKey, pLabel) ->
                        val isSel = selectedProvider == pKey
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedProvider = pKey
                                prefs.selectedProvider = pKey
                            },
                            label = { Text(pLabel, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 3. API Keys (Android Keystore Encrypted)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_api_keys), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                // Gemini Key
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text(stringResource(R.string.settings_gemini_key)) },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                            Icon(
                                imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_key_input")
                )

                // OpenAI Key
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = { openAiKey = it },
                    label = { Text(stringResource(R.string.settings_openai_key)) },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = if (showOpenAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                            Icon(
                                imageVector = if (showOpenAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("openai_key_input")
                )

                // Grok Key
                OutlinedTextField(
                    value = grokKey,
                    onValueChange = { grokKey = it },
                    label = { Text(stringResource(R.string.settings_grok_key)) },
                    placeholder = { Text("xai-...") },
                    singleLine = true,
                    visualTransformation = if (showGrokKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showGrokKey = !showGrokKey }) {
                            Icon(
                                imageVector = if (showGrokKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("grok_key_input")
                )

                Button(
                    onClick = {
                        prefs.geminiApiKey = geminiKey
                        prefs.openAiApiKey = openAiKey
                        prefs.grokApiKey = grokKey
                        saveFeedback = "API Keys saved securely in Android Keystore!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End).testTag("save_keys_btn")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_save))
                }

                if (saveFeedback != null) {
                    Text(
                        text = saveFeedback ?: "",
                        color = EmeraldGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Voice & Interaction Settings
        val isAlwaysListening by viewModel.isAlwaysListening.collectAsState()
        val wakeWordOnly by viewModel.wakeWordOnly.collectAsState()

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_voice_speed), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                // Always listening mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_always_listening), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(stringResource(R.string.settings_always_listening_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isAlwaysListening,
                        onCheckedChange = {
                            viewModel.setAlwaysListening(it)
                        },
                        modifier = Modifier.testTag("settings_always_listening_switch")
                    )
                }

                // Wake word only switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_wake_word_only), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(stringResource(R.string.settings_wake_word_only_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = wakeWordOnly,
                        onCheckedChange = {
                            viewModel.setWakeWordOnly(it)
                        },
                        modifier = Modifier.testTag("settings_wake_word_only_switch")
                    )
                }

                // Follow up window switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_followup_window), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Keep mic listening 30s after assistant response", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = followUpEnabled,
                        onCheckedChange = {
                            followUpEnabled = it
                            prefs.activeFollowUpEnabled = it
                        }
                    )
                }

                // Speech Rate
                Column {
                    Text("Speech Rate: ${String.format(Locale.US, "%.1fx", speechRate)}", fontSize = 13.sp)
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            prefs.speechRate = it
                        },
                        valueRange = 0.6f..1.6f,
                        steps = 5
                    )
                }

                // Speech Pitch
                Column {
                    Text("Speech Pitch: ${String.format(Locale.US, "%.1fx", speechPitch)}", fontSize = 13.sp)
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            prefs.speechPitch = it
                        },
                        valueRange = 0.6f..1.4f,
                        steps = 4
                    )
                }
            }
        }

        // 5. Privacy & Zero-Telemetry Badge
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.settings_privacy_badge), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = "Encrypted at rest with AES-256-GCM Keystore. Database, memories, logs, and device control stay strictly on-device.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
