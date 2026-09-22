package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.AssistantVoiceState
import com.example.ui.SweatyViewModel
import com.example.ui.components.PanicStopButton
import com.example.ui.components.QuickActionChip
import com.example.ui.components.VoiceOrb
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.VioletNeon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: SweatyViewModel) {
    val voiceState by viewModel.voiceState.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val lastReply by viewModel.lastAssistantReply.collectAsState()
    val followUpSeconds by viewModel.followUpRemainingSeconds.collectAsState()
    val isAlwaysListening by viewModel.isAlwaysListening.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Status Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Always Listening Toggle Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isAlwaysListening) CyanNeon.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                    .border(
                        1.dp,
                        if (isAlwaysListening) CyanNeon.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { viewModel.toggleAlwaysListening() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("home_always_listening_pill")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isAlwaysListening) EmeraldGlow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (isAlwaysListening) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAlwaysListening) stringResource(R.string.always_listening_active) else stringResource(R.string.always_listening_inactive),
                    color = if (isAlwaysListening) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAlwaysListening) "• Hands-free ON" else "• Tap to turn ON",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Active Follow-Up Pill
            AnimatedVisibility(visible = followUpSeconds > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyanNeon.copy(alpha = 0.15f))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Follow-up window: ${followUpSeconds}s active",
                        color = CyanNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Voice Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            VoiceOrb(
                state = voiceState,
                audioRms = audioRms,
                onClick = { viewModel.toggleVoiceListening() }
            )
        }

        // Voice State Label
        val stateText = when {
            voiceState == AssistantVoiceState.LISTENING && isAlwaysListening ->
                "Always Listening • Say \"Hey Sweaty\" or any command…"
            voiceState == AssistantVoiceState.LISTENING ->
                stringResource(R.string.state_listening)
            voiceState == AssistantVoiceState.THINKING ->
                stringResource(R.string.state_thinking)
            voiceState == AssistantVoiceState.SPEAKING ->
                stringResource(R.string.state_speaking)
            voiceState == AssistantVoiceState.ERROR ->
                stringResource(R.string.state_error)
            isAlwaysListening ->
                "Always Listening active • Say \"Hey Sweaty\""
            else ->
                stringResource(R.string.state_idle)
        }

        Text(
            text = stateText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when (voiceState) {
                AssistantVoiceState.LISTENING -> CyanNeon
                AssistantVoiceState.THINKING -> VioletNeon
                AssistantVoiceState.SPEAKING -> EmeraldGlow
                AssistantVoiceState.ERROR -> MaterialTheme.colorScheme.error
                AssistantVoiceState.IDLE -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .testTag("home_voice_state_text")
        )

        // Live Transcript or Last Assistant Output Card
        if (liveTranscript.isNotBlank() || lastReply != null) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("home_transcript_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (liveTranscript.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = liveTranscript,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (liveTranscript.isNotBlank() && lastReply != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (lastReply != null) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = EmeraldGlow,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lastReply ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Prompts Row
        Text(
            text = "Suggested Actions / দ্রুত অ্যাকশন",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionChip(
                text = stringResource(R.string.quick_remind_me),
                icon = Icons.Default.Alarm,
                onClick = { viewModel.onUserSpoke("Remind me to call Sarah tomorrow at 10 AM") }
            )
            QuickActionChip(
                text = stringResource(R.string.quick_briefing),
                icon = Icons.Default.WbSunny,
                onClick = { viewModel.playMorningBriefing() }
            )
            QuickActionChip(
                text = stringResource(R.string.quick_take_screenshot),
                icon = Icons.Default.CameraAlt,
                onClick = { viewModel.onUserSpoke("Take a screenshot") }
            )
            QuickActionChip(
                text = stringResource(R.string.quick_open_whatsapp),
                onClick = { viewModel.onUserSpoke("Open WhatsApp") }
            )
            QuickActionChip(
                text = "হোমে যাও (Go Home)",
                onClick = { viewModel.onUserSpoke("Go to home screen") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Emergency Panic Stop Button
        PanicStopButton(
            onPanicStop = { viewModel.panicStop() },
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}
