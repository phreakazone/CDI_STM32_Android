package id.ns200.cdir7.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.ui.theme.BorderSubtle
import id.ns200.cdir7.ui.theme.MotecOrange
import id.ns200.cdir7.ui.theme.SurfacePanel
import id.ns200.cdir7.ui.theme.TextMuted

/**
 * Komponen Tombol Kotak Tegas Semi-Transparan Bergaya Instrumentasi Balap MoTeC M1
 */
@Composable
fun MotecButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MotecOrange,
    enabled: Boolean = true,
    height: Dp = 32.dp,
    fontSize: TextUnit = 10.sp,
    shape: Shape = RoundedCornerShape(3.dp),
    badge: String? = null,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
) {
    val finalMod = if (testTag != null) modifier.height(height).testTag(testTag) else modifier.height(height)
    OutlinedButton(
        onClick = onClick,
        modifier = finalMod,
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, if (enabled) color else BorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) color.copy(alpha = 0.14f) else SurfacePanel.copy(alpha = 0.3f),
            contentColor = if (enabled) color else TextMuted,
            disabledContentColor = TextMuted
        ),
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (enabled) color else TextMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false
        )
        if (badge != null) {
            Spacer(modifier = Modifier.width(4.dp))
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(2.dp),
                color = if (enabled) color.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f),
                border = BorderStroke(0.5.dp, if (enabled) color.copy(alpha = 0.5f) else BorderSubtle)
            ) {
                Text(
                    text = badge,
                    fontSize = (fontSize.value * 0.8f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (enabled) color else TextMuted,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
    }
}
