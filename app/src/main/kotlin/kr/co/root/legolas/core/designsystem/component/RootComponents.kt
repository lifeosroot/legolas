package kr.co.root.legolas.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.co.root.legolas.core.designsystem.theme.RootColors

private val ButtonShape = RoundedCornerShape(10.dp)

@Composable
fun RootPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RootColors.Accent,
            contentColor = RootColors.TextPrimary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun RootSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        border = BorderStroke(1.dp, RootColors.Stroke),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = RootColors.Accent),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
