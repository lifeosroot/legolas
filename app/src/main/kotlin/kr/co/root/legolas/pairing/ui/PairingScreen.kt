package kr.co.root.legolas.pairing.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.root.legolas.R
import kr.co.root.legolas.core.designsystem.component.RootPrimaryButton
import kr.co.root.legolas.core.designsystem.component.RootSecondaryButton
import kr.co.root.legolas.core.designsystem.theme.RootColors
import kr.co.root.legolas.core.designsystem.theme.RootTheme

@Composable
fun PairingScreen(
    state: PairingUiState,
    onScan: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = RootColors.Background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> LoadingContent()
                state.serverUrl == null -> UnpairedContent(state.errorMessage, onScan)
                else -> PairedContent(state.serverUrl, state.errorMessage, onForget)
            }
        }
    }
}

@Composable
private fun UnpairedContent(errorMessage: String?, onScan: () -> Unit) {
    PairingColumn {
        AppMark()
        Spacer(Modifier.height(48.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusLabel(
                text = stringResource(R.string.ready_to_pair),
                color = RootColors.Accent,
                pulsing = true,
            )
            Text(
                text = stringResource(R.string.pairing_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = RootColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.pairing_description),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = RootColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (errorMessage != null) ErrorMessage(errorMessage)
            Spacer(Modifier.height(16.dp))
            RootPrimaryButton(
                text = stringResource(R.string.scan_qr),
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.pairing_security_note),
            style = MaterialTheme.typography.bodySmall,
            color = RootColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PairedContent(
    serverUrl: String,
    errorMessage: String?,
    onForget: () -> Unit,
) {
    PairingColumn {
        AppMark()
        Spacer(Modifier.height(48.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusLabel(
                text = stringResource(R.string.connection_active),
                color = RootColors.Success,
            )
            Text(
                text = stringResource(R.string.connected_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = RootColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.connected_description),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = RootColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            ServerAddress(serverUrl)
            if (errorMessage != null) ErrorMessage(errorMessage)
            Spacer(Modifier.height(16.dp))
            RootSecondaryButton(
                text = stringResource(R.string.forget_arwen),
                onClick = onForget,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.connected_security_note),
            style = MaterialTheme.typography.bodySmall,
            color = RootColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingContent() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = RootColors.Accent,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.loading_pairing),
            style = MaterialTheme.typography.bodyMedium,
            color = RootColors.TextSecondary,
        )
    }
}

@Composable
private fun AppMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                RootColors.Accent.copy(alpha = 0.22f),
                                RootColors.Accent.copy(alpha = 0.06f),
                                androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
            Image(
                painter = painterResource(R.drawable.legolas_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(28.dp)),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 2.sp),
            color = RootColors.TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.arwen_client),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            color = RootColors.TextTertiary,
        )
    }
}

@Composable
private fun StatusLabel(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color = color, pulsing = pulsing)
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun StatusDot(
    color: androidx.compose.ui.graphics.Color,
    pulsing: Boolean,
) {
    if (!pulsing) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "status pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "status pulse progress",
    )

    Box(
        modifier = Modifier.size(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = 1f + progress * 1.4f
                    scaleY = 1f + progress * 1.4f
                    alpha = 1f - progress
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.42f)),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun ServerAddress(serverUrl: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(color = RootColors.StrokeSubtle)
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = RootColors.Danger,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PairingColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .padding(vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = { content() },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14, widthDp = 360, heightDp = 720)
@Composable
private fun UnpairedPreview() {
    RootTheme {
        PairingScreen(
            state = PairingUiState(isLoading = false),
            onScan = {},
            onForget = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14, widthDp = 720, heightDp = 720)
@Composable
private fun PairedPreview() {
    RootTheme {
        PairingScreen(
            state = PairingUiState(
                isLoading = false,
                serverUrl = "https://arwen.example.com",
            ),
            onScan = {},
            onForget = {},
        )
    }
}
