package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.WiringStep
import com.example.ui.theme.*
import id.ns200.cdir7.ui.components.MotecButton

@Composable
fun StepVerificationDialog(
  step: WiringStep,
  currentMeasuredValue: String,
  currentNotes: String,
  onDismiss: () -> Unit,
  onConfirmVerification: (measuredValue: String, userNotes: String) -> Unit
) {
  var measuredValueInput by remember { mutableStateOf(currentMeasuredValue) }
  var userNotesInput by remember { mutableStateOf(currentNotes) }
  var isPhysicalChecked by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = TechSurfaceElevated),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ElectricCyan, SafetyGreen))),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SafetyGreen.copy(alpha = 0.2f),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SafetyGreen,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = "VERIFIKASI LANGKAH ${step.stepNumber}",
              style = MaterialTheme.typography.titleMedium,
              color = SafetyGreen,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = step.title,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondaryDark,
              maxLines = 1
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Requirement card
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1722)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "SYARAT UJI PENGUKURAN SEBELUM LANJUT:",
              style = MaterialTheme.typography.labelSmall,
              color = SparkAmber,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = step.verificationRequirement,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
              color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Nilai Target / Kriteria: ",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
              )
              Text(
                text = step.expectedValue,
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Measured Value TextField
        OutlinedTextField(
          value = measuredValueInput,
          onValueChange = { measuredValueInput = it },
          label = { Text("Hasil Ukur Multimeter Aktual") },
          placeholder = { Text("Contoh: 5.01V / 0.1 Ohm / OK") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricCyan,
            unfocusedBorderColor = OutlineDark,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // User Notes TextField
        OutlinedTextField(
          value = userNotesInput,
          onValueChange = { userNotesInput = it },
          label = { Text("Catatan Pengerjaan (Opsional)") },
          placeholder = { Text("Contoh: Kabel disolder rapi, heat-shrink terpasang") },
          maxLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricCyan,
            unfocusedBorderColor = OutlineDark,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Checkbox confirmation
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Checkbox(
            checked = isPhysicalChecked,
            onCheckedChange = { isPhysicalChecked = it },
            colors = CheckboxDefaults.colors(
              checkedColor = SafetyGreen,
              checkmarkColor = Color.Black
            )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Saya telah memeriksa solderan, isolasi jarak bebas, dan polaritas kaki komponen.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextPrimaryDark
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          MotecButton(
            text = "BATAL",
            onClick = onDismiss,
            color = TextSecondaryDark,
            height = 32.dp,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          MotecButton(
            text = "VERIFIKASI & LANJUT",
            onClick = {
              onConfirmVerification(
                measuredValueInput.ifBlank { step.expectedValue },
                userNotesInput
              )
            },
            enabled = isPhysicalChecked,
            color = SafetyGreen,
            icon = Icons.Default.CheckCircle,
            height = 32.dp,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}
