package kr.co.root.legolas.settings.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.co.root.legolas.R
import kr.co.root.legolas.core.designsystem.component.RootSecondaryButton
import kr.co.root.legolas.core.designsystem.component.RootSurface
import kr.co.root.legolas.core.designsystem.theme.RootColors
import kr.co.root.legolas.core.designsystem.theme.RootTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    serverUrl: String,
    errorMessage: String?,
    onBack: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showForgetConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RootColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        color = RootColors.TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = RootColors.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RootColors.Background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.pairing_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = RootColors.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pairing_settings_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = RootColors.TextSecondary,
                )
                Spacer(Modifier.height(16.dp))
                RootSurface {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RootColors.Success),
                        )
                        Text(
                            text = stringResource(R.string.connection_active),
                            style = MaterialTheme.typography.labelLarge,
                            color = RootColors.Success,
                        )
                    }
                    Text(
                        text = stringResource(R.string.connected_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RootColors.TextSecondary,
                    )
                    HorizontalDivider(color = RootColors.StrokeSubtle)
                    Text(
                        text = stringResource(R.string.server_address).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = RootColors.TextTertiary,
                    )
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RootColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RootColors.Danger,
                        )
                    }
                    RootSecondaryButton(
                        text = stringResource(R.string.forget_arwen),
                        onClick = { showForgetConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentColor = RootColors.Danger,
                    )
                }
            }
        }
    }

    if (showForgetConfirmation) {
        AlertDialog(
            onDismissRequest = { showForgetConfirmation = false },
            title = { Text(stringResource(R.string.forget_arwen)) },
            text = { Text(stringResource(R.string.forget_arwen_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgetConfirmation = false
                        onForget()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RootColors.Danger),
                ) {
                    Text(stringResource(R.string.forget_arwen))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14, widthDp = 360, heightDp = 720)
@Composable
private fun SettingsPreview() {
    RootTheme {
        SettingsScreen(
            serverUrl = "https://arwen.example.com",
            errorMessage = null,
            onBack = {},
            onForget = {},
        )
    }
}
