package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PinConnection
import com.example.model.WiringStep
import com.example.model.adaptToPlatform
import com.example.ui.theme.*
import id.ns200.cdir7.McuPlatform

/**
 * StepWiringVisualCanvas:
 * Interactive visual circuit canvas showing realistic components and live pin-to-pin wiring traces.
 * When wires are connected/toggled, they dynamically light up with neon flow and moving current pulses.
 */
@Composable
fun StepWiringVisualCanvas(
  step: WiringStep,
  connections: List<PinConnection>,
  checkedConnections: Set<String>,
  onToggleConnection: (String, String) -> Unit,
  platform: McuPlatform = McuPlatform.STM32WB55,
  modifier: Modifier = Modifier
) {
  val horizontalScroll = rememberScrollState()
  var selectedFilterConnId by remember(step.id) { mutableStateOf<String?>(null) }

  val pulseAnim = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by pulseAnim.animateFloat(
    initialValue = 0.55f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(700, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "wireGlow"
  )

  val electronAnim by pulseAnim.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "electronFlow"
  )

  val checkedCount = connections.count { checkedConnections.contains("${step.id}_${it.id}") }
  val isAllChecked = connections.isNotEmpty() && checkedCount == connections.size

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F17)),
    border = BorderStroke(
      1.5.dp,
      if (isAllChecked) SafetyGreen.copy(alpha = 0.8f) else Color(0xFF1E2D40)
    )
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Top status bar with wire filter chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (isAllChecked) SafetyGreen else SparkAmber)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "CANVAS WIRING PIN-KE-PIN",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (isAllChecked) SafetyGreen.copy(alpha = 0.15f) else Color(0xFF162232),
          border = BorderStroke(1.dp, if (isAllChecked) SafetyGreen else OutlineDark)
        ) {
          Text(
            text = "$checkedCount/${connections.size} KABEL TERPASANG",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isAllChecked) SafetyGreen else SparkAmber,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Perfboard Substrate with Realistic Components & Live Pin-to-Pin Wires
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF070B12),
        border = BorderStroke(1.dp, Color(0xFF152030)),
        modifier = Modifier
          .fillMaxWidth()
          .height(230.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScroll)
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            // Render the realistic electronic components of this step
            StepSpecificRealisticLayout(
              step = step,
              connections = connections,
              checkedConnections = checkedConnections,
              platform = platform
            )

            // Pin-to-Pin Interactive Wires Panel
            Column(
              modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp),
              verticalArrangement = Arrangement.Center
            ) {
              Text(
                text = "KONEKSI KABEL LANGKAH INI:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondaryDark
              )

              Spacer(modifier = Modifier.height(6.dp))

              Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                connections.forEach { conn ->
                  val isWired = checkedConnections.contains("${step.id}_${conn.id}")
                  InteractivePinWireNode(
                    connection = conn,
                    isWired = isWired,
                    pulseAlpha = pulseAlpha,
                    electronProgress = electronAnim,
                    onToggle = { onToggleConnection(step.id, conn.id) }
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Interactive Pin Wire Node:
 * Represents a physical wire span between Node A and Node B on the canvas.
 * User can tap the wire or terminal to connect/disconnect live!
 */
@Composable
private fun InteractivePinWireNode(
  connection: PinConnection,
  isWired: Boolean,
  pulseAlpha: Float,
  electronProgress: Float,
  onToggle: () -> Unit
) {
  val wireColor = Color(connection.wireColorHex)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isWired) Color(0xFF0F1E19) else Color(0xFF111722))
      .border(
        1.dp,
        if (isWired) SafetyGreen.copy(alpha = 0.6f) else Color(0xFF223044),
        RoundedCornerShape(10.dp)
      )
      .clickable { onToggle() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    // Top Row: Pin A -> Pin B
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = SparkAmber.copy(alpha = 0.2f),
        border = BorderStroke(0.8.dp, SparkAmber.copy(alpha = 0.6f))
      ) {
        Text(
          text = connection.fromNode.take(14),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = SparkAmber,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
      }

      Icon(
        imageVector = Icons.Default.ArrowForward,
        contentDescription = null,
        tint = if (isWired) SafetyGreen else TextTertiaryDark,
        modifier = Modifier.size(12.dp)
      )

      Surface(
        shape = RoundedCornerShape(4.dp),
        color = SafetyGreen.copy(alpha = 0.2f),
        border = BorderStroke(0.8.dp, SafetyGreen.copy(alpha = 0.6f))
      ) {
        Text(
          text = connection.toNode.take(14),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = SafetyGreen,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Physical Wire Line Canvas (glowing with flowing particles when active)
    Box(
      modifier = Modifier
        .width(130.dp)
        .height(28.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val centerY = size.height / 2
        val startX = 14f
        val endX = size.width - 14f

        // Copper annular ring pads at ends
        drawCircle(
          color = Color(0xFFB87333), // Copper pad
          radius = 7f,
          center = Offset(startX, centerY),
          style = Stroke(width = 3f)
        )
        drawCircle(
          color = Color(0xFFB87333),
          radius = 7f,
          center = Offset(endX, centerY),
          style = Stroke(width = 3f)
        )

        if (isWired) {
          // Solder fillets
          drawCircle(color = Color(0xFFE2E8F0), radius = 5f, center = Offset(startX, centerY))
          drawCircle(color = Color(0xFFE2E8F0), radius = 5f, center = Offset(endX, centerY))

          // Glow Halo
          drawLine(
            color = wireColor.copy(alpha = pulseAlpha * 0.45f),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 10f,
            cap = StrokeCap.Round
          )

          // Solid Wire
          drawLine(
            color = wireColor,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
          )

          // Flowing electron spark dot
          val electronX = startX + (endX - startX) * electronProgress
          drawCircle(
            color = Color.White,
            radius = 3.5f,
            center = Offset(electronX, centerY)
          )
        } else {
          // Unconnected dashed wire trace
          drawLine(
            color = wireColor.copy(alpha = 0.35f),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
          )
        }
      }

      // Center Status Badge
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isWired) SafetyGreen else Color(0xFF1E2D40),
        border = BorderStroke(0.8.dp, if (isWired) SafetyGreen else OutlineDark)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isWired) Icons.Default.Check else Icons.Default.Add,
            contentDescription = null,
            tint = if (isWired) Color.Black else ElectricCyan,
            modifier = Modifier.size(10.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = if (isWired) "TERSAMBUNG" else "SAMBUNGKAN",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = if (isWired) Color.Black else TextPrimaryDark
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Wire label & spec
    Text(
      text = connection.wireLabel,
      fontSize = 8.sp,
      color = if (isWired) TextPrimaryDark else TextTertiaryDark,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * StepWiringChecklist:
 * Clean, proportional, readable Pin-to-Pin connection checklist.
 * Eliminates sprawling text and excessive scrolling, fitting ergonomically in view.
 */
@Composable
fun StepWiringChecklist(
  step: WiringStep,
  connections: List<PinConnection>,
  checkedConnections: Set<String>,
  onToggleConnection: (String, String) -> Unit,
  onCheckAllConnections: (String, List<String>, Boolean) -> Unit,
  onOpenVerification: () -> Unit,
  modifier: Modifier = Modifier
) {
  val checkedCount = connections.count { checkedConnections.contains("${step.id}_${it.id}") }
  val isAllChecked = connections.isNotEmpty() && checkedCount == connections.size

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Header Bar with Quick Action
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = Color(0xFF0F1722),
      border = BorderStroke(1.dp, Color(0xFF1E2D40)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PlaylistAddCheck,
            contentDescription = null,
            tint = SparkAmber,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "CHECKLIST TITIK SAMBUNG",
              style = MaterialTheme.typography.labelSmall,
              color = SparkAmber,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "$checkedCount dari ${connections.size} kabel tersambung",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = if (isAllChecked) SafetyGreen else TextSecondaryDark
            )
          }
        }

        FilledTonalButton(
          onClick = {
            val allIds = connections.map { it.id }
            onCheckAllConnections(step.id, allIds, !isAllChecked)
          },
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isAllChecked) Color(0xFF263238) else Color(0xFF162D3D),
            contentColor = if (isAllChecked) TextPrimaryDark else ElectricCyan
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Icon(
            imageVector = if (isAllChecked) Icons.Default.Close else Icons.Default.DoneAll,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isAllChecked) "Lepas Semua" else "Sambung Semua",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // List of Connection Cards - Wide, Comfortable, Spacious Layout
    connections.forEachIndexed { index, conn ->
      val isConnChecked = checkedConnections.contains("${step.id}_${conn.id}")
      val wireColor = Color(conn.wireColorHex)

      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isConnChecked) Color(0xFF0A1B14) else Color(0xFF101724)
        ),
        border = BorderStroke(
          1.5.dp,
          if (isConnChecked) SafetyGreen.copy(alpha = 0.8f) else Color(0xFF223247)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleConnection(step.id, conn.id) }
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Row 1: Header (Number Badge + Wire Label + Color Stripe + Badges)
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              // Wire Index Circle
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(if (isConnChecked) SafetyGreen else Color(0xFF1E2D40)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${index + 1}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isConnChecked) Color.Black else TextPrimaryDark
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              // Wire Color Swatch Bar
              Box(
                modifier = Modifier
                  .width(6.dp)
                  .height(18.dp)
                  .clip(RoundedCornerShape(3.dp))
                  .background(wireColor)
              )

              Spacer(modifier = Modifier.width(8.dp))

              // Wire Label Title
              Text(
                text = conn.wireLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                color = if (isConnChecked) TextPrimaryDark else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Badges on Right
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              if (conn.isHighVoltage) {
                Surface(
                  color = HighVoltageRed.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(1.dp, HighVoltageRed)
                ) {
                  Text(
                    text = "⚡ 285V HV",
                    fontSize = 9.sp,
                    color = HighVoltageRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isConnChecked) SafetyGreen.copy(alpha = 0.2f) else Color(0xFF182333),
                border = BorderStroke(1.dp, if (isConnChecked) SafetyGreen else OutlineDark)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                  Icon(
                    imageVector = if (isConnChecked) Icons.Default.Check else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isConnChecked) SafetyGreen else TextTertiaryDark,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = if (isConnChecked) "TERPASANG" else "BELUM",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isConnChecked) SafetyGreen else TextSecondaryDark
                  )
                }
              }
            }
          }

          // Row 2: Spacious From -> To Routing Box
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0A0F17),
            border = BorderStroke(1.dp, Color(0xFF182436)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              // From Node
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "DARI TITIK",
                  fontSize = 9.sp,
                  color = SparkAmber,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SparkAmber.copy(alpha = 0.15f),
                  border = BorderStroke(0.8.dp, SparkAmber.copy(alpha = 0.6f))
                ) {
                  Text(
                    text = conn.fromNode,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SparkAmber,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }

              // Route Arrow
              Box(
                modifier = Modifier
                  .padding(horizontal = 8.dp)
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF141F2D)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowForward,
                  contentDescription = null,
                  tint = wireColor,
                  modifier = Modifier.size(16.dp)
                )
              }

              // To Node
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
              ) {
                Text(
                  text = "MENUJU TITIK",
                  fontSize = 9.sp,
                  color = SafetyGreen,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SafetyGreen.copy(alpha = 0.15f),
                  border = BorderStroke(0.8.dp, SafetyGreen.copy(alpha = 0.6f))
                ) {
                  Text(
                    text = conn.toNode,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SafetyGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }
            }
          }

          // Row 3: Solder Tip Banner (Dedicated, Easy to Read)
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF121B27),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = SparkAmber,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Tips Solder: ${conn.solderTip}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = TextSecondaryDark
              )
            }
          }

          // Row 4: Full-Width Clear Touch-Target Button
          Button(
            onClick = { onToggleConnection(step.id, conn.id) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isConnChecked) SafetyGreen else Color(0xFF192A3D),
              contentColor = if (isConnChecked) Color.Black else ElectricCyan
            ),
            border = if (isConnChecked) null else BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
          ) {
            Icon(
              imageVector = if (isConnChecked) Icons.Default.CheckCircle else Icons.Default.Cable,
              contentDescription = null,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isConnChecked) "Kabel Sudah Tersambung (Ketuk untuk Lepas)" else "Tandai Kabel Tersambung",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Completion notification banner if all wires are checked
    if (isAllChecked) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF00381C),
        border = BorderStroke(1.dp, SafetyGreen),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = SafetyGreen,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Semua Kabel Selesai Disolder!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SafetyGreen
              )
              Text(
                text = "Langkah selanjutnya: Ukur multimeter & verifikasi",
                fontSize = 10.sp,
                color = Color(0xFFA5D6A7)
              )
            }
          }

          Button(
            onClick = onOpenVerification,
            colors = ButtonDefaults.buttonColors(
              containerColor = SafetyGreen,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Text("Verifikasi Sekarang", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * StepWiringDiagramCanvas (Unified Wrapper):
 * Renders both canvas and checklist for backward compatibility.
 */
@Composable
fun StepWiringDiagramCanvas(
  step: WiringStep,
  checkedConnections: Set<String>,
  onToggleConnection: (String, String) -> Unit,
  onCheckAllConnections: (String, List<String>, Boolean) -> Unit,
  platform: McuPlatform = McuPlatform.STM32WB55,
  modifier: Modifier = Modifier
) {
  val connections = remember(step.id, platform) {
    if (step.pinConnections.isNotEmpty()) {
      step.pinConnections
    } else {
      generateDefaultConnectionsForStep(step, platform)
    }
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    StepWiringVisualCanvas(
      step = step,
      connections = connections,
      checkedConnections = checkedConnections,
      onToggleConnection = onToggleConnection,
      platform = platform
    )

    StepWiringChecklist(
      step = step,
      connections = connections,
      checkedConnections = checkedConnections,
      onToggleConnection = onToggleConnection,
      onCheckAllConnections = onCheckAllConnections,
      onOpenVerification = {}
    )
  }
}

/**
 * Renders the realistic visual component placement according to current step context.
 */
@Composable
private fun StepSpecificRealisticLayout(
  step: WiringStep,
  connections: List<PinConnection>,
  checkedConnections: Set<String>,
  platform: McuPlatform = McuPlatform.STM32WB55
) {
  val isEsp = platform == McuPlatform.ESP32_WROOM
  Row(
    modifier = Modifier.fillMaxHeight(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    when {
      // Step 1.1: J1 Harness to GND_STAR
      step.id == "step_1_1" -> {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("SOKET HARNESS J1", fontSize = 8.sp, color = SparkAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.padding(4.dp)
          ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Pin 11 (Hitam-Kuning)", fontSize = 9.sp, color = SafetyGreen, fontWeight = FontWeight.Bold)
              Text("Massa Utama Motor", fontSize = 7.sp, color = TextSecondaryDark)
            }
          }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("PCB LOGIC 7x9 cm", fontSize = 8.sp, color = ElectricCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F2618),
            border = BorderStroke(1.5.dp, SafetyGreen),
            modifier = Modifier.padding(4.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("GND_STAR BUS", fontSize = 10.sp, color = SafetyGreen, fontWeight = FontWeight.Black)
              Text("Pusat Bintang Massa PCB", fontSize = 8.sp, color = Color(0xFF81C784))
            }
          }
        }
      }

      // Step 1.2: 12V Input Protection (FMAIN, DREV SB560, TVS, L_IN, C_IN)
      step.id == "step_1_2" -> {
        RealisticFuse(ref = "FMAIN", rating = "5A", color = Color(0xFFFB8C00))
        RealisticDiode(ref = "DREV", partName = "SB560", anodeLabel = "J1.5 (12V)", cathodeLabel = "VIN_PROT")
        RealisticResistor(ref = "L_IN", valueText = "47uH 5A", colorBands = listOf(Color(0xFFFDD835), Color(0xFF7E57C2), Color.Black, Color(0xFFFFD54F)))
        RealisticElectrolyticCap(ref = "C_IN", spec = "470uF 35V")
      }

      // Step 1.3: LM2596 Step-Down Module
      step.id == "step_1_3" -> {
        RealisticFuse(ref = "FLOGIC", rating = "1A", color = Color(0xFF42A5F5))

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF0D47A1),
          border = BorderStroke(1.5.dp, Color(0xFF42A5F5)),
          modifier = Modifier
            .width(120.dp)
            .height(90.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("LM2596 STEP-DOWN", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Black)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("IN+: 12V\nIN-: GND", fontSize = 7.sp, color = Color(0xFFBBDEFB), fontFamily = FontFamily.Monospace)
              Text("OUT+: 5.00V\nOUT-: GND", fontSize = 7.sp, color = SafetyGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Text("Trimpot Set: 5.00V", fontSize = 7.sp, color = SparkAmber, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Step 1.4: MCU Board Mount & Power (ESP32 vs STM32)
      step.id == "step_1_4" -> {
        if (isEsp) {
          RealisticEsp32Board(
            activePins = setOf("VIN", "GND", "3V3"),
            highlightedPin = "VIN"
          )
        } else {
          RealisticWeActBoard(
            activePins = setOf("G", "5V", "3V3", "H_BOTTOM.1", "H_BOTTOM.2", "H_TOP.1", "H_TOP.3"),
            highlightedPin = "H_BOTTOM.2"
          )
        }
      }

      // Stage 2: Sensor & Comparator Steps (e.g. LM339 / PC817, TPS, TEMP, VBAT)
      step.id == "step_2_1" || step.id == "step_2_2" || step.id == "step_2_3" || step.id == "step_2_4" || step.id == "step_2_6" || step.id == "step_2_7" || step.id == "step_2_8" -> {
        if (isEsp) {
          RealisticEsp32Board(
            activePins = when (step.id) {
              "step_2_1" -> setOf("33", "GND", "VIN") // VBAT via GPIO33
              "step_2_2" -> setOf("16", "17", "GND", "3V3") // OEM Center/Side taps
              "step_2_3" -> setOf("VN", "GND") // Temp ADC1_CH3 GPIO39(VN)
              "step_2_4" -> setOf("VP", "34", "3V3", "GND") // TPS + TPS reference
              "step_2_6" -> setOf("13", "GND") // Fan relay GPIO13
              "step_2_7" -> setOf("27", "GND") // Strobe timing GPIO27
              else -> setOf("VIN", "GND", "3V3")
            },
            highlightedPin = when (step.id) {
              "step_2_1" -> "33"
              "step_2_2" -> "16"
              "step_2_3" -> "VN"
              "step_2_4" -> "VP"
              "step_2_6" -> "13"
              "step_2_7" -> "27"
              else -> null
            }
          )
        } else {
          RealisticWeActBoard(
            activePins = when (step.id) {
              "step_2_1" -> setOf("B0", "H_TOP.14", "3V3", "G")
              "step_2_2" -> setOf("B3", "B4", "H_TOP.8", "H_TOP.9", "3V3")
              "step_2_3" -> setOf("A4", "H_BOTTOM.13", "3V3", "G")
              "step_2_4" -> setOf("A3", "A5", "H_BOTTOM.12", "H_BOTTOM.14")
              "step_2_6" -> setOf("B5", "H_TOP.7", "G")
              "step_2_7" -> setOf("B9", "H_BOTTOM.6", "G")
              else -> setOf("G", "3V3")
            },
            highlightedPin = step.targetPin
          )
        }

        when (step.id) {
          "step_2_1" -> {
            RealisticResistor(ref = "R_BAT1", valueText = "100k", colorBands = listOf(Color(0xFF795548), Color.Black, Color(0xFFFDD835), Color(0xFFFFD54F)))
            RealisticResistor(ref = "R_BAT2", valueText = "22k", colorBands = listOf(Color.Red, Color.Red, Color(0xFFFF8F00), Color(0xFFFFD54F)))
          }
          "step_2_2" -> {
            if (isEsp) {
              RealisticIcChip(
                ref = "2x OPTO",
                partNumber = "PC817",
                pinCount = 4,
                activePins = setOf(1, 2, 3, 4),
                pinLabels = mapOf(1 to "OEM C", 2 to "OEM S", 3 to "GPIO16", 4 to "GPIO17")
              )
            }
          }
          "step_2_6" -> {
            RealisticTo220(ref = "QFAN", partName = "BC547", pin1Label = "C", pin2Label = "B", pin3Label = "E")
          }
          "step_2_7" -> {
            RealisticTo220(ref = "Q_STR", partName = "FQP30N06L", pin1Label = "G", pin2Label = "D", pin3Label = "S")
          }
          else -> {}
        }
      }

      // Step 2.5: LM339 Pulser Comparator
      step.id == "step_2_5" -> {
        RealisticIcChip(
          ref = "U2",
          partNumber = "LM339N",
          pinCount = 14,
          activePins = setOf(2, 3, 4, 5, 12),
          pinLabels = mapOf(
            2 to if (isEsp) "OUT1 (GPIO4)" else "OUT1 (PA0)",
            3 to "VCC (5V)",
            4 to "IN1- (~2V)",
            5 to "IN1+ (Pulser)",
            12 to "GND"
          )
        )
        RealisticDiode(ref = "BAT54S", partName = "CLAMP", isGlass = true)
        RealisticResistor(ref = "R_HYST", valueText = "10M", colorBands = listOf(Color(0xFF795548), Color.Black, Color(0xFF1E88E5), Color(0xFFFFD54F)))
      }

      // Stage 3: Push-pull Transformer, MOSFETs, TC4427 Driver
      step.id == "step_3_1" || step.id == "step_3_2" || step.id == "step_3_3" || step.id == "step_3_4" || step.id == "step_3_5" || step.id == "step_3_6" -> {
        when (step.id) {
          "step_3_1" -> {
            RealisticFuse(ref = "FHV", rating = "3A", color = Color(0xFF8E24AA))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SparkAmber.copy(alpha = 0.2f),
              border = BorderStroke(1.5.dp, SparkAmber),
              modifier = Modifier.padding(4.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SW_SERVICE", fontSize = 9.sp, color = SparkAmber, fontWeight = FontWeight.Bold)
                Text("Isolator Servis Opsional", fontSize = 7.sp, color = TextSecondaryDark)
              }
            }
          }
          "step_3_3" -> {
            RealisticTransformer(ref = "T1", spec = "ATX EE-35 Push-Pull")
          }
          "step_3_4" -> {
            RealisticIcChip(
              ref = "U4",
              partNumber = "TC4427A",
              pinCount = 8,
              activePins = setOf(2, 3, 4, 5, 6, 7),
              pinLabels = mapOf(
                2 to if (isEsp) "INA (GPIO18)" else "INA (PA9)",
                3 to "GND",
                4 to if (isEsp) "INB (GPIO19)" else "INB (PB8)",
                5 to "OUTB",
                6 to "VDD (12V)",
                7 to "OUTA"
              )
            )
          }
          "step_3_5" -> {
            RealisticTo220(ref = "QHV1", partName = "IRF3205", pin1Label = "G", pin2Label = "D", pin3Label = "S")
            RealisticTo220(ref = "QHV2", partName = "IRF3205", pin1Label = "G", pin2Label = "D", pin3Label = "S")
            RealisticResistor(ref = "RSENSE", valueText = "0.05R 5W", colorBands = emptyList(), isPowerCement = true)
          }
          else -> {
            RealisticTransformer(ref = "T1", spec = "ATX EE-35 Push-Pull")
          }
        }
      }

      // Stage 4: Rectifier & Dual HV Banks
      step.id == "step_4_1" || step.id == "step_4_2" || step.id == "step_4_3" -> {
        RealisticDiode(ref = "DREC1", partName = "UF4007", anodeLabel = "AC1", cathodeLabel = "HV+")
        RealisticDiode(ref = "DREC2", partName = "UF4007", anodeLabel = "AC2", cathodeLabel = "HV+")
        RealisticDiode(ref = "DCH_C", partName = "UF4007", anodeLabel = "BRIDGE+", cathodeLabel = "HV_CENTER")
        RealisticDiode(ref = "DCH_S", partName = "UF4007", anodeLabel = "BRIDGE+", cathodeLabel = "HV_SIDE")
      }

      // Stage 5: MKP Pulse Capacitors & SCR BT151
      step.id == "step_5_1" || step.id == "step_5_2" || step.id == "step_5_3" || step.id == "step_5_4" -> {
        RealisticMkpCapacitor(ref = if (step.id.contains("5_2")) "C_SIDE" else "C_CENTER", spec = "1.0uF 630V MKP")
        RealisticTo220(ref = "SCR1", partName = "BT151-600R", pin1Label = "K", pin2Label = "A", pin3Label = "G", isTabHighVoltage = true)
        RealisticResistor(ref = "R_BLEED", valueText = "4x 470k", colorBands = listOf(Color(0xFFFDD835), Color(0xFF7E57C2), Color(0xFFFDD835), Color(0xFFFFD54F)))
      }

      // Default fallback component preview
      else -> {
        if (isEsp) {
          RealisticEsp32Board(activePins = setOf("VIN", "GND", "3V3"))
        } else {
          RealisticWeActBoard(activePins = setOf("G", "5V", "3V3"))
        }
      }
    }
  }
}

/**
 * Generates specific pin connections for each step if not provided in the data model.
 * Fully platform-aware: renders accurate ESP32 pin names when ESP32 is active, and WeAct/STM32 when STM32 is active.
 */
fun generateDefaultConnectionsForStep(step: WiringStep, platform: McuPlatform = McuPlatform.STM32WB55): List<PinConnection> {
  val isEsp = platform == McuPlatform.ESP32_WROOM

  if (isEsp) {
    return when (step.id) {
      "step_1_1" -> listOf(
        PinConnection("conn_1", "Harness J1.11 (Hitam-Kuning)", "Pusat Rel GND_STAR", 0xFF00E676, "Kabel AWG 18", "Solder langsung ke kawat tembaga tengah"),
        PinConnection("conn_2", "GND_STAR Bus Bar", "Jalur Ground PCB Logic & ESP32", 0xFF00E676, "Kawat 1.5mm²", "Bridging timah pad sepanjang rel memanjang")
      )
      "step_1_2" -> listOf(
        PinConnection("conn_1", "Harness J1.5 (+12V Kontak)", "Sekring FMAIN (5A)", 0xFFFF9E0B, "Kabel AWG 18", "Solder ke terminal input rumah sikring"),
        PinConnection("conn_2", "Sekring FMAIN Keluar", "Dioda DREV (SB560) Anoda", 0xFFFF9E0B, "Kaki Komponen", "Solder seri langsung di pad PCB"),
        PinConnection("conn_3", "DREV Katoda (Garis Perak)", "Simpul VIN_PROT & TVS Katoda", 0xFFFF9E0B, "Bridge Timah", "TVS Katoda ke VIN_PROT, Anoda ke GND_STAR"),
        PinConnection("conn_4", "VIN_PROT", "Induktor L_IN (47uH)", 0xFFFF9E0B, "Kawat Toroid", "Menyeberang ke simpul VIN_FILT"),
        PinConnection("conn_5", "VIN_FILT", "Kapasitor C_IN (470uF) (+)", 0xFFFF9E0B, "Kaki Komponen", "Kaki (-) elko ke GND_STAR")
      )
      "step_1_3" -> listOf(
        PinConnection("conn_1", "VIN_FILT", "Sekring FLOGIC (1A)", 0xFF00E5FF, "Jumper Kawat", "Proteksi catu daya modul LM2596"),
        PinConnection("conn_2", "FLOGIC Keluar", "Modul LM2596 IN+", 0xFF00E5FF, "Pin Header", "Solder ke lubang IN+ modul"),
        PinConnection("conn_3", "GND_STAR", "Modul LM2596 IN- & OUT-", 0xFF00E676, "Kabel AWG 22", "Satukan ground input dan output ke GND_STAR"),
        PinConnection("conn_4", "Modul LM2596 OUT+ (5.00V)", "Rel +5V_LOGIC PCB (Catu VIN ESP32)", 0xFF00E5FF, "Kabel Merah AWG 22", "Pastikan terukur tepat 5.00V sebelum ke ESP32")
      )
      "step_1_4" -> listOf(
        PinConnection("conn_1", "Rel +5V_LOGIC", "ESP32 VIN (LEFT.19)", 0xFF00E5FF, "Kabel AWG 22", "Catu 5V ke pin VIN baris kiri"),
        PinConnection("conn_2", "GND_STAR", "ESP32 GND (LEFT.14)", 0xFF00E676, "Kabel AWG 22", "Ground utama MCU baris kiri"),
        PinConnection("conn_3", "GND_STAR", "ESP32 GND (RIGHT.1)", 0xFF00E676, "Kabel AWG 22", "Ground sekunder MCU baris kanan"),
        PinConnection("conn_4", "ESP32 Pin 3V3 (3.3V Out)", "Rel +3.3V Logic Sensor", 0xFF00E5FF, "Kabel AWG 24", "Tegangan referensi ADC & pull-up (DILARANG 5V/12V!)")
      )
      "step_2_1" -> listOf(
        PinConnection("conn_1", "VIN_FILT", "Resistor R_BAT1 (100k)", 0xFFFF9E0B, "Kaki Komponen", "Ujung atas pembagi tegangan aki"),
        PinConnection("conn_2", "R_BAT1 Bawah", "Simpul VBAT_ADC & R_BAT2 (22k)", 0xFF00E5FF, "Simpul Bersama", "Paralel dengan kapasitor 10nF ke GND_STAR"),
        PinConnection("conn_3", "Simpul VBAT_ADC", "Resistor 1k ke ESP32 GPIO33 (ADC1_CH5)", 0xFF00E5FF, "Kawat Jumper", "Input ADC1 deteksi tegangan aki (Maks 3.3V)"),
        PinConnection("conn_4", "Simpul VBAT_ADC", "BAT54S Clamp SOT-23 (3V3 ESP32 & GND)", 0xFF00E5FF, "SMD Adapter", "Pin 1 ke GND, Pin 2 ke 3V3, Pin 3 ke ADC")
      )
      "step_2_2" -> listOf(
        PinConnection("conn_1", "Modul PC817 OUT1 (Center)", "ESP32 GPIO16 (OEM Tap Center)", 0xFF00E5FF, "Kabel AWG 24", "Input capture pulsa pengapian OEM Center"),
        PinConnection("conn_2", "Modul PC817 OUT2 (Side)", "ESP32 GPIO17 (OEM Tap Side)", 0xFF00E5FF, "Kabel AWG 24", "Input capture pulsa pengapian OEM Side"),
        PinConnection("conn_3", "Modul PC817 VCC & GND", "ESP32 3V3 & GND", 0xFF00E676, "Kabel AWG 24", "Daya isolasi sinyal optokopler 3.3V")
      )
      "step_2_3" -> listOf(
        PinConnection("conn_1", "Kabel Temp J1.3 (Hijau-Hitam)", "R-Divider & ESP32 GPIO39 (VN)", 0xFFFFD600, "Kabel AWG 24", "Sensor suhu mesin masuk ADC1_CH3 ESP32")
      )
      "step_2_4" -> listOf(
        PinConnection("conn_1", "Harness J1.2 (Hijau-Putih)", "Header J_TPS Pin 1 & Pin 6", 0xFFFFD600, "Kabel Sensor", "Kabel TPS A NS200"),
        PinConnection("conn_2", "Harness J1.4 (Abu-Abu)", "Header J_TPS Pin 3 & Pin 4", 0xFFFFD600, "Kabel Sensor", "Kabel TPS B NS200"),
        PinConnection("conn_3", "+5V_LOGIC", "Header J_TPS Pin 2 (TPS_REF)", 0xFF00E5FF, "Via 100R", "Tegangan referensi 5V sensor throttle"),
        PinConnection("conn_4", "Header J_TPS Pin 5 (TPS_SIG)", "ESP32 GPIO36 / VP (ADC1_CH0)", 0xFFFFD600, "Via Divider & Clamp", "Sinyal bukaan gas masuk ADC1 ESP32 (Maks 3.3V)")
      )
      "step_2_5" -> listOf(
        PinConnection("conn_1", "Harness J1.10 (Putih-Merah)", "Resistor 39k / 0.5W", 0xFFFFD600, "Metal Film 0.5W", "Input pulser spul magnet NS200"),
        PinConnection("conn_2", "Resistor 39k Keluar", "LM393 Pin 2 / Opto PC817 IN", 0xFFFFD600, "Pengkondisi Sinyal", "Ambang pulser & proteksi tegangan induktif"),
        PinConnection("conn_3", "Output Pengkondisi Sinyal", "ESP32 GPIO4 (Pulser ISR)", 0xFF00E5FF, "Kabel AWG 24", "GPIO4 eksternal interrupt capture pulsa magnet")
      )
      "step_2_6" -> listOf(
        PinConnection("conn_1", "ESP32 GPIO13", "Driver Relay IN (Kipas Radiator)", 0xFF00E5FF, "Kabel AWG 24", "Kontrol on/off kipas radiator via transistor/opto"),
        PinConnection("conn_2", "GND_STAR", "Driver Relay GND", 0xFF00E676, "Kabel AWG 22", "Ground bersama modul relay")
      )
      "step_2_7" -> listOf(
        PinConnection("conn_1", "ESP32 GPIO27", "Gate MOSFET Q_STR (Strobo TDC)", 0xFF00E5FF, "Resistor 100R", "Pulsa strobo timing kalibrasi TDC"),
        PinConnection("conn_2", "Drain MOSFET Q_STR", "Modul LED Strobo (-)", 0xFFFF9E0B, "Kabel AWG 22", "Kutub negatif lampu strobo")
      )
      "step_3_1" -> listOf(
        PinConnection("conn_1", "VIN_FILT (PCB Logic)", "Sekring FHV (3A Blade)", 0xFFFF9E0B, "Kabel AWG 18 Antar Board", "Daya charger push-pull"),
        PinConnection("conn_2", "Sekring FHV Keluar", "SW_SERVICE Pin 1", 0xFFFF9E0B, "Saklar/konektor arus sesuai", "Isolator servis mekanis opsional"),
        PinConnection("conn_3", "SW_SERVICE Pin 2", "Rel VIN_HV PCB Power", 0xFFFF9E0B, "Bus Bar Tembaga", "Menyuplai Center-Tap Trafo dan TC4427")
      )
      "step_3_3" -> listOf(
        PinConnection("conn_1", "Rel VIN_HV", "Trafo T1 LV_CT (Center-Tap)", 0xFFFF9E0B, "Kawat Tembaga Tebal", "Input catu daya 12V push-pull"),
        PinConnection("conn_2", "Trafo T1 LV_A (Kiri)", "Drain MOSFET QHV1 (IRF3205)", 0xFFFF9E0B, "Jalur Lebar", "Switching push-pull fasa A"),
        PinConnection("conn_3", "Trafo T1 LV_B (Kanan)", "Drain MOSFET QHV2 (IRF3205)", 0xFFFF9E0B, "Jalur Lebar", "Switching push-pull fasa B"),
        PinConnection("conn_4", "Trafo T1 HV_AC1 & HV_AC2", "Input Jembatan Dioda Ultrafast", 0xFFFF6D00, "Clearance >= 6mm", "Output sekunder AC 200V-300V")
      )
      "step_3_5" -> listOf(
        PinConnection("conn_1", "TC4427 Pin 7 (OUTA)", "Gate QHV1 via 10R", 0xFF00E5FF, "Jalur Pendek", "Pulsa gerbang MOSFET A"),
        PinConnection("conn_2", "TC4427 Pin 5 (OUTB)", "Gate QHV2 via 10R", 0xFF00E5FF, "Jalur Pendek", "Pulsa gerbang MOSFET B"),
        PinConnection("conn_3", "Source QHV1 & QHV2", "Simpul ISENSE_TOP", 0xFF00E676, "Kawat 1.5mm²", "Satukan kedua source MOSFET"),
        PinConnection("conn_4", "Simpul ISENSE_TOP", "RSENSE (0.05 Ohm 5W)", 0xFF00E676, "Resistor Semen", "Dari ISENSE_TOP ke rel GND_POWER")
      )
      "step_3_6" -> listOf(
        PinConnection("conn_1", "LM339 OUT2/OUT3/OUT4 wired-OR", "PWM_CLAMP", 0xFF00E5FF, "Jalur PCB + pull-up 4.7k", "Hardware clamp active-low"),
        PinConnection("conn_2", "PWM_CLAMP", "ESP32 GPIO14 (LEFT.12)", 0xFF00E5FF, "Kabel AWG 24", "Input fault active-low ke firmware")
      )
      "step_4_1" -> listOf(
        PinConnection("conn_1", "Trafo T1 HV_AC1", "DREC1 Anoda & DREC3 Katoda", 0xFFFF6D00, "Kabel HV 600V", "Fasa 1 jembatan UF4007"),
        PinConnection("conn_2", "Trafo T1 HV_AC2", "DREC2 Anoda & DREC4 Katoda", 0xFFFF6D00, "Kabel HV 600V", "Fasa 2 jembatan UF4007"),
        PinConnection("conn_3", "DREC1 & DREC2 Katoda", "Rel BRIDGE_PLUS (285V DC)", 0xFFFF6D00, "Kawat HV", "Keluaran positif penyearah"),
        PinConnection("conn_4", "DREC3 & DREC4 Anoda", "Rel GND_POWER", 0xFF00E676, "Kawat Tembaga 1.5mm²", "Kembali ke massa daya")
      )
      "step_4_2" -> listOf(
        PinConnection("conn_1", "Rel BRIDGE_PLUS", "DCH_C Anoda (UF4007)", 0xFFFF6D00, "Jalur HV", "Dioda isolasi Bank Center"),
        PinConnection("conn_2", "DCH_C Katoda", "Rel HV_CENTER (Bank Tengah)", 0xFFFF6D00, "Jalur HV", "Reservoir muatan koil tengah"),
        PinConnection("conn_3", "Rel BRIDGE_PLUS", "DCH_S Anoda (UF4007)", 0xFFFF6D00, "Jalur HV", "Dioda isolasi Bank Side"),
        PinConnection("conn_4", "DCH_S Katoda", "Rel HV_SIDE (Bank Samping)", 0xFFFF6D00, "Jalur HV", "Reservoir muatan koil samping")
      )
      "step_4_3" -> listOf(
        PinConnection("conn_1", "Divider HV_C_FB", "ESP32 GPIO35 (ADC1_CH7 Bank Center)", 0xFFFF6D00, "Kabel AWG 24", "Monitor tegangan reservoir tengah (target 285V/345V)"),
        PinConnection("conn_2", "Divider HV_S_FB", "ESP32 GPIO32 (ADC1_CH4 Bank Side)", 0xFFFF6D00, "Kabel AWG 24", "Monitor tegangan reservoir samping")
      )
      "step_5_1" -> listOf(
        PinConnection("conn_1", "Rel HV_CENTER", "Terminal A C_CENTER (1uF 630V)", 0xFFFF6D00, "Pad HV", "Kapasitor buang muatan koil tengah"),
        PinConnection("conn_2", "Terminal A & B", "Resistor Bleeder 4x 470k Seri", 0xFFFF6D00, "Melintang Kaki", "Pembuang muatan otomatis saat mati"),
        PinConnection("conn_3", "Terminal B C_CENTER", "Harness J1.12 (COIL_CENTER)", 0xFFFF6D00, "Kabel Oranye HV", "Menuju koil busi tengah NS200")
      )
      "step_5_3" -> listOf(
        PinConnection("conn_1", "Rel HV_CENTER", "SCR1 BT151 Pin 2 (Anode + Tab)", 0xFFFF6D00, "Kaki Tengah TO-220", "Tab logam bertegangan 285V!"),
        PinConnection("conn_2", "SCR1 BT151 Pin 1 (Cathode)", "Rel GND_POWER", 0xFF00E676, "Kawat 1.5mm²", "Discharge kapasitor ke ground"),
        PinConnection("conn_3", "Driver Gate (BC557)", "SCR1 BT151 Pin 3 (Gate)", 0xFF00E5FF, "Resistor 330R", "Pulsa arus pemicu pelepasan muatan"),
        PinConnection("conn_4", "SCR1 Gate ke Cathode", "Resistor Pulldown 1k", 0xFF00E5FF, "Bawah PCB", "Anti pemicuan liar akibat derau busi")
      )
      "step_5_4" -> listOf(
        PinConnection("conn_1", "ESP32 GPIO25", "4.7k -> Driver NPN/PNP -> 330R -> Gate SCR1", 0xFF00E5FF, "Jalur logika ke driver", "Pulsa pengapian busi tengah"),
        PinConnection("conn_2", "ESP32 GPIO26", "4.7k -> Driver NPN/PNP -> 330R -> Gate SCR2", 0xFF00E5FF, "Jalur logika ke driver", "Pulsa pengapian busi samping")
      )
      else -> {
        val adapted = step.adaptToPlatform(platform)
        listOf(
          PinConnection("conn_1", adapted.sourcePin, adapted.targetPin, 0xFF00E5FF, "Kabel Sirkuit", adapted.pinLegGuide),
          PinConnection("conn_2", "Ground Sirkuit", "GND_STAR / GND_POWER", 0xFF00E676, "Kawat Ground", "Pastikan kontinuitas ground < 0.2 Ohm")
        )
      }
    }
  }

  return when (step.id) {
    "step_1_1" -> listOf(
      PinConnection("conn_1", "Harness J1.11 (Hitam-Kuning)", "Pusat Rel GND_STAR", 0xFF00E676, "Kabel AWG 18", "Solder langsung ke kawat tembaga tengah"),
      PinConnection("conn_2", "GND_STAR Bus Bar", "Jalur Ground PCB Logic", 0xFF00E676, "Kawat 1.5mm²", "Bridging timah pad sepanjang rel memanjang")
    )
    "step_1_2" -> listOf(
      PinConnection("conn_1", "Harness J1.5 (+12V Kontak)", "Sekring FMAIN (5A)", 0xFFFF9E0B, "Kabel AWG 18", "Solder ke terminal input rumah sikring"),
      PinConnection("conn_2", "Sekring FMAIN Keluar", "Dioda DREV (SB560) Anoda", 0xFFFF9E0B, "Kaki Komponen", "Solder seri langsung di pad PCB"),
      PinConnection("conn_3", "DREV Katoda (Garis Perak)", "Simpul VIN_PROT & TVS Katoda", 0xFFFF9E0B, "Bridge Timah", "TVS Katoda ke VIN_PROT, Anoda ke GND_STAR"),
      PinConnection("conn_4", "VIN_PROT", "Induktor L_IN (47uH)", 0xFFFF9E0B, "Kawat Toroid", "Menyeberang ke simpul VIN_FILT"),
      PinConnection("conn_5", "VIN_FILT", "Kapasitor C_IN (470uF) (+)", 0xFFFF9E0B, "Kaki Komponen", "Kaki (-) elko ke GND_STAR")
    )
    "step_1_3" -> listOf(
      PinConnection("conn_1", "VIN_FILT", "Sekring FLOGIC (1A)", 0xFF00E5FF, "Jumper Kawat", "Proteksi catu daya modul LM2596"),
      PinConnection("conn_2", "FLOGIC Keluar", "Modul LM2596 IN+", 0xFF00E5FF, "Pin Header", "Solder ke lubang IN+ modul"),
      PinConnection("conn_3", "GND_STAR", "Modul LM2596 IN- & OUT-", 0xFF00E676, "Kabel AWG 22", "Satukan ground input dan output ke GND_STAR"),
      PinConnection("conn_4", "Modul LM2596 OUT+ (5.00V)", "Rel +5V_LOGIC PCB", 0xFF00E5FF, "Kabel Merah AWG 22", "Pastikan terukur tepat 5.00V sebelum ke MCU")
    )
    "step_1_4" -> listOf(
      PinConnection("conn_1", "Rel +5V_LOGIC", "STM32 H_BOTTOM.2 (5V)", 0xFF00E5FF, "Kabel AWG 22", "Solder ke socket female pin 2 baris bawah"),
      PinConnection("conn_2", "GND_STAR", "STM32 H_BOTTOM.1 (G)", 0xFF00E676, "Kabel AWG 22", "Ground utama MCU baris bawah"),
      PinConnection("conn_3", "GND_STAR", "STM32 H_TOP.1 (G)", 0xFF00E676, "Kabel AWG 22", "Ground sekunder MCU baris atas"),
      PinConnection("conn_4", "STM32 H_BOTTOM.4 (VBAT)", "DIBIARKAN KOSONG", 0xFFFF1744, "DILARANG SAMBUNG", "Pin backup RTC; dilarang kena 12V!")
    )
    "step_2_1" -> listOf(
      PinConnection("conn_1", "VIN_FILT", "Resistor R_BAT1 (100k)", 0xFFFF9E0B, "Kaki Komponen", "Ujung atas pembagi tegangan aki"),
      PinConnection("conn_2", "R_BAT1 Bawah", "Simpul VBAT_ADC & R_BAT2 (22k)", 0xFF00E5FF, "Simpul Bersama", "Paralel dengan kapasitor 10nF ke GND_STAR"),
      PinConnection("conn_3", "Simpul VBAT_ADC", "Resistor 1k ke H_TOP.14 (PB0)", 0xFF00E5FF, "Kawat Jumper", "Input ADC1_IN15 deteksi tegangan aki"),
      PinConnection("conn_4", "Simpul VBAT_ADC", "BAT54S Clamp SOT-23", 0xFF00E5FF, "SMD Adapter", "Pin 1 ke GND, Pin 2 ke 3V3, Pin 3 ke ADC")
    )
    "step_2_4" -> listOf(
      PinConnection("conn_1", "Harness J1.2 (Hijau-Putih)", "Header J_TPS Pin 1 & Pin 6", 0xFFFFD600, "Kabel Sensor", "Kabel TPS A NS200"),
      PinConnection("conn_2", "Harness J1.4 (Abu-Abu)", "Header J_TPS Pin 3 & Pin 4", 0xFFFFD600, "Kabel Sensor", "Kabel TPS B NS200"),
      PinConnection("conn_3", "+5V_LOGIC", "Header J_TPS Pin 2 (TPS_REF)", 0xFF00E5FF, "Via 100R", "Tegangan referensi 5V sensor throttle"),
      PinConnection("conn_4", "Header J_TPS Pin 5 (TPS_SIG)", "STM32 H_BOTTOM.12 (PA3)", 0xFFFFD600, "Via Divider & Clamp", "Sinyal bukaan gas masuk ADC MCU")
    )
    "step_2_5" -> listOf(
      PinConnection("conn_1", "Harness J1.10 (Putih-Merah)", "Resistor 39k / 0.5W", 0xFFFFD600, "Metal Film 0.5W", "Input pulser spul magnet NS200"),
      PinConnection("conn_2", "Resistor 39k Keluar", "LM339 Pin 5 (IN1+)", 0xFFFFD600, "Kaki Soket DIP-14", "Bias VMID ~2.5V & BAT54S clamp"),
      PinConnection("conn_3", "Pembagi 15k+10k (VZERO)", "LM339 Pin 4 (IN1-)", 0xFF00E5FF, "Tegangan Acuan", "Ambang tegangan nol pulser ~2.0V"),
      PinConnection("conn_4", "LM339 Pin 2 (OUT1)", "STM32 H_BOTTOM.9 (PA0)", 0xFF00E5FF, "Pullup 4.7k ke 3V3", "TIM2_CH1 capture pulsa pengapian"),
      PinConnection("conn_5", "LM339 Pin 2 (OUT1)", "LM339 Pin 5 (IN1+)", 0xFF00E5FF, "Resistor 10M", "Umpan balik histeresis anti noise")
    )
    "step_3_1" -> listOf(
      PinConnection("conn_1", "VIN_FILT (PCB Logic)", "Sekring FHV (3A Blade)", 0xFFFF9E0B, "Kabel AWG 18 Antar Board", "Daya charger push-pull"),
      PinConnection("conn_2", "Sekring FHV Keluar", "SW_SERVICE Pin 1", 0xFFFF9E0B, "Saklar/konektor arus sesuai", "Isolator servis mekanis opsional"),
      PinConnection("conn_3", "SW_SERVICE Pin 2", "Rel VIN_HV PCB Power", 0xFFFF9E0B, "Bus Bar Tembaga", "Menyuplai Center-Tap Trafo dan TC4427")
    )
    "step_3_3" -> listOf(
      PinConnection("conn_1", "Rel VIN_HV", "Trafo T1 LV_CT (Center-Tap)", 0xFFFF9E0B, "Kawat Tembaga Tebal", "Input catu daya 12V push-pull"),
      PinConnection("conn_2", "Trafo T1 LV_A (Kiri)", "Drain MOSFET QHV1 (IRF3205)", 0xFFFF9E0B, "Jalur Lebar", "Switching push-pull fasa A"),
      PinConnection("conn_3", "Trafo T1 LV_B (Kanan)", "Drain MOSFET QHV2 (IRF3205)", 0xFFFF9E0B, "Jalur Lebar", "Switching push-pull fasa B"),
      PinConnection("conn_4", "Trafo T1 HV_AC1 & HV_AC2", "Input Jembatan Dioda Ultrafast", 0xFFFF6D00, "Clearance >= 6mm", "Output sekunder AC 200V-300V")
    )
    "step_3_5" -> listOf(
      PinConnection("conn_1", "TC4427 Pin 7 (OUTA)", "Gate QHV1 via 10R", 0xFF00E5FF, "Jalur Pendek", "Pulsa gerbang MOSFET A"),
      PinConnection("conn_2", "TC4427 Pin 5 (OUTB)", "Gate QHV2 via 10R", 0xFF00E5FF, "Jalur Pendek", "Pulsa gerbang MOSFET B"),
      PinConnection("conn_3", "Source QHV1 & QHV2", "Simpul ISENSE_TOP", 0xFF00E676, "Kawat 1.5mm²", "Satukan kedua source MOSFET"),
      PinConnection("conn_4", "Simpul ISENSE_TOP", "RSENSE (0.05 Ohm 5W)", 0xFF00E676, "Resistor Semen", "Dari ISENSE_TOP ke rel GND_POWER")
    )
    "step_4_1" -> listOf(
      PinConnection("conn_1", "Trafo T1 HV_AC1", "DREC1 Anoda & DREC3 Katoda", 0xFFFF6D00, "Kabel HV 600V", "Fasa 1 jembatan UF4007"),
      PinConnection("conn_2", "Trafo T1 HV_AC2", "DREC2 Anoda & DREC4 Katoda", 0xFFFF6D00, "Kabel HV 600V", "Fasa 2 jembatan UF4007"),
      PinConnection("conn_3", "DREC1 & DREC2 Katoda", "Rel BRIDGE_PLUS (285V DC)", 0xFFFF6D00, "Kawat HV", "Keluaran positif penyearah"),
      PinConnection("conn_4", "DREC3 & DREC4 Anoda", "Rel GND_POWER", 0xFF00E676, "Kawat Tembaga 1.5mm²", "Kembali ke massa daya")
    )
    "step_4_2" -> listOf(
      PinConnection("conn_1", "Rel BRIDGE_PLUS", "DCH_C Anoda (UF4007)", 0xFFFF6D00, "Jalur HV", "Dioda isolasi Bank Center"),
      PinConnection("conn_2", "DCH_C Katoda", "Rel HV_CENTER (Bank Tengah)", 0xFFFF6D00, "Jalur HV", "Reservoir muatan koil tengah"),
      PinConnection("conn_3", "Rel BRIDGE_PLUS", "DCH_S Anoda (UF4007)", 0xFFFF6D00, "Jalur HV", "Dioda isolasi Bank Side"),
      PinConnection("conn_4", "DCH_S Katoda", "Rel HV_SIDE (Bank Samping)", 0xFFFF6D00, "Jalur HV", "Reservoir muatan koil samping")
    )
    "step_5_1" -> listOf(
      PinConnection("conn_1", "Rel HV_CENTER", "Terminal A C_CENTER (1uF 630V)", 0xFFFF6D00, "Pad HV", "Kapasitor buang muatan koil tengah"),
      PinConnection("conn_2", "Terminal A & B", "Resistor Bleeder 4x 470k Seri", 0xFFFF6D00, "Melintang Kaki", "Pembuang muatan otomatis saat mati"),
      PinConnection("conn_3", "Terminal B C_CENTER", "Harness J1.12 (COIL_CENTER)", 0xFFFF6D00, "Kabel Oranye HV", "Menuju koil busi tengah NS200")
    )
    "step_5_3" -> listOf(
      PinConnection("conn_1", "Rel HV_CENTER", "SCR1 BT151 Pin 2 (Anode + Tab)", 0xFFFF6D00, "Kaki Tengah TO-220", "Tab logam bertegangan 285V!"),
      PinConnection("conn_2", "SCR1 BT151 Pin 1 (Cathode)", "Rel GND_POWER", 0xFF00E676, "Kawat 1.5mm²", "Discharge kapasitor ke ground"),
      PinConnection("conn_3", "Driver Gate (BC557)", "SCR1 BT151 Pin 3 (Gate)", 0xFF00E5FF, "Resistor 330R", "Pulsa arus pemicu pelepasan muatan"),
      PinConnection("conn_4", "SCR1 Gate ke Cathode", "Resistor Pulldown 1k", 0xFF00E5FF, "Bawah PCB", "Anti pemicuan liar akibat derau busi")
    )
    else -> listOf(
      PinConnection("conn_1", step.sourcePin, step.targetPin, 0xFF00E5FF, "Kabel Sirkuit", step.pinLegGuide),
      PinConnection("conn_2", "Ground Sirkuit", "GND_STAR / GND_POWER", 0xFF00E676, "Kawat Ground", "Pastikan kontinuitas ground < 0.2 Ohm")
    )
  }
}
