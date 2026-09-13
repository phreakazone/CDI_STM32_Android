package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VerificationRecord
import com.example.model.WiringStep
import com.example.model.adaptToPlatform
import com.example.ui.components.FullCumulativeCircuitSimulator
import com.example.ui.components.StepVerificationDialog
import com.example.ui.components.StepWiringChecklist
import com.example.ui.components.StepWiringVisualCanvas
import com.example.ui.components.generateDefaultConnectionsForStep
import com.example.ui.theme.*
import com.example.viewmodel.WiringViewModel

/**
 * TutorialStepScreen:
 * Professional single-screen workstation UI for the Pulsar NS200 CDI wiring tutorial.
 * Optimizes layout to fit key workflow elements in one view:
 * 1. Compact Step Navigator & Stepper Bar
 * 2. Mode Selector: Interactive Pin-to-Pin Circuit Canvas vs Full Cumulative PCB
 * 3. Segmented Workstation Tabs directly below canvas (Checklist, Components/Schema, Solder & Multimeter)
 * 4. Docked Bottom Action Bar with direct Verification action
 */
@Composable
fun TutorialStepScreen(
  viewModel: WiringViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val rawStep by viewModel.currentStep.collectAsState()
  val currentStep = remember(rawStep, uiState.mcuPlatform) {
    rawStep.adaptToPlatform(uiState.mcuPlatform)
  }
  val verificationRecords by viewModel.verificationRecords.collectAsState()
  val checkedConnections by viewModel.checkedConnections.collectAsState()
  val verificationProgress by viewModel.verificationProgress.collectAsState()
  val enforceSequential by viewModel.enforceSequential.collectAsState()

  val stepScrollState = rememberScrollState()
  var selectedTab by remember(currentStep.id) { mutableIntStateOf(0) }
  var isCanvasExpanded by remember { mutableStateOf(false) }

  val currentRecord = verificationRecords[currentStep.id]
  val isCurrentVerified = currentRecord?.isVerified == true
  val isCurrentUnlocked = viewModel.repository.isStepUnlocked(uiState.selectedStepIndex, viewModel.allSteps.size)

  // Step-specific connections dynamically aware of MCU platform
  val connections = remember(currentStep.id, uiState.mcuPlatform) {
    if (currentStep.pinConnections.isNotEmpty()) {
      currentStep.pinConnections
    } else {
      generateDefaultConnectionsForStep(currentStep, uiState.mcuPlatform)
    }
  }

  val checkedCount = connections.count { checkedConnections.contains("${currentStep.id}_${it.id}") }
  val isAllWiresChecked = connections.isNotEmpty() && checkedCount == connections.size

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TechDarkBg)
  ) {
    // 1. COMPACT WORKSTATION TOP BAR
    Surface(
      color = TechSurface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Step Title, Stage & Stepper Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Left: Prev Step Button
          IconButton(
            onClick = { viewModel.prevStep() },
            enabled = uiState.selectedStepIndex > 0,
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Sebelumnya",
              tint = if (uiState.selectedStepIndex > 0) ElectricCyan else TextTertiaryDark,
              modifier = Modifier.size(18.dp)
            )
          }

          // Center: Step Title & Badges
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(currentStep.board.colorCode).copy(alpha = 0.2f),
                border = BorderStroke(0.8.dp, Color(currentStep.board.colorCode))
              ) {
                Text(
                  text = "LANGKAH ${currentStep.stepNumber}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = Color(currentStep.board.colorCode),
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }

              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isCurrentVerified) SafetyGreen.copy(alpha = 0.2f) else SparkAmber.copy(alpha = 0.2f),
                border = BorderStroke(0.8.dp, if (isCurrentVerified) SafetyGreen else SparkAmber)
              ) {
                Text(
                  text = if (isCurrentVerified) "TERVERIFIKASI ✓" else "BELUM VERIFIKASI",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = if (isCurrentVerified) SafetyGreen else SparkAmber,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
              text = currentStep.title,
              style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
              color = TextPrimaryDark,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          // Right: Next Step Button
          IconButton(
            onClick = { viewModel.nextStep() },
            enabled = uiState.selectedStepIndex < viewModel.allSteps.size - 1,
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Selanjutnya",
              tint = if (uiState.selectedStepIndex < viewModel.allSteps.size - 1) ElectricCyan else TextTertiaryDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Mini Step Selector Chips Strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(stepScrollState),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          viewModel.allSteps.forEachIndexed { idx, step ->
            val isVerified = verificationRecords[step.id]?.isVerified == true
            val isSelected = idx == uiState.selectedStepIndex
            val isUnlocked = viewModel.repository.isStepUnlocked(idx, viewModel.allSteps.size)

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = when {
                isSelected -> ElectricCyan
                isVerified -> SafetyGreen.copy(alpha = 0.2f)
                else -> Color(0xFF141F2D)
              },
              border = BorderStroke(
                1.dp,
                when {
                  isSelected -> ElectricCyan
                  isVerified -> SafetyGreen.copy(alpha = 0.8f)
                  else -> OutlineDark
                }
              ),
              modifier = Modifier
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = isUnlocked || !enforceSequential) {
                  viewModel.selectStep(idx)
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isVerified) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else SafetyGreen,
                    modifier = Modifier.size(10.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                  text = step.stepNumber,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = when {
                    isSelected -> Color.Black
                    isVerified -> SafetyGreen
                    isUnlocked -> TextPrimaryDark
                    else -> TextTertiaryDark
                  }
                )
              }
            }
          }
        }
      }
    }

    // 2. SLIM MODE SELECTOR BAR
    Surface(
      color = Color(0xFF0F1722),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Button 1: Mode Wiring Pin-ke-Pin (Default)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (!uiState.showFullCircuitSimulator) TechPrimary else Color(0xFF172333),
          border = BorderStroke(1.dp, if (!uiState.showFullCircuitSimulator) TechPrimary else Color(0xFF26374D)),
          modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
              viewModel.setFullCircuitSimulator(false)
              isCanvasExpanded = true
            }
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Cable,
              contentDescription = null,
              tint = if (!uiState.showFullCircuitSimulator) Color.Black else ElectricCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Diagram Pin-ke-Pin",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (!uiState.showFullCircuitSimulator) Color.Black else TextPrimaryDark
            )
          }
        }

        // Button 2: Simulator PCB Kumulatif
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (uiState.showFullCircuitSimulator) ElectricCyan else Color(0xFF172333),
          border = BorderStroke(1.dp, if (uiState.showFullCircuitSimulator) ElectricCyan else Color(0xFF26374D)),
          modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
              viewModel.setFullCircuitSimulator(true)
              isCanvasExpanded = true
            }
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Layers,
              contentDescription = null,
              tint = if (uiState.showFullCircuitSimulator) Color.Black else ElectricCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "PCB Kumulatif Full",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (uiState.showFullCircuitSimulator) Color.Black else TextPrimaryDark
            )
          }
        }
      }
    }

    // 3. CIRCUIT VISUAL CANVAS TOGGLE BAR
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color(0xFF0F1722),
      border = BorderStroke(1.dp, Color(0xFF1E2D40)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 2.dp)
        .clickable { isCanvasExpanded = !isCanvasExpanded }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (uiState.showFullCircuitSimulator) Icons.Default.Layers else Icons.Default.AccountTree,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (uiState.showFullCircuitSimulator) "Visual PCB Kumulatif" else "Visual Diagram Rangkaian PCB",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCanvasExpanded) ElectricCyan else TextPrimaryDark
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (isCanvasExpanded) "Sembunyikan" else "Buka Diagram",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan
          )
          Spacer(modifier = Modifier.width(3.dp))
          Icon(
            imageVector = if (isCanvasExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // 3b. EXPANDABLE CANVAS CONTENT
    if (isCanvasExpanded) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        if (uiState.showFullCircuitSimulator) {
          FullCumulativeCircuitSimulator(
            currentStep = currentStep,
            allSteps = viewModel.allSteps,
            verifiedStepIds = verificationRecords.filter { it.value.isVerified }.keys,
            onSelectStep = { idx -> viewModel.selectStep(idx) },
            onClose = { viewModel.setFullCircuitSimulator(false) },
            platform = uiState.mcuPlatform,
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          StepWiringVisualCanvas(
            step = currentStep,
            connections = connections,
            checkedConnections = checkedConnections,
            onToggleConnection = { stepId, connId ->
              viewModel.toggleConnection(stepId, connId)
            },
            platform = uiState.mcuPlatform,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // 4. SEGMENTED WORKSTATION TABS (Takes remaining vertical space cleanly)
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
      // Tab Bar
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFF0F1722),
        contentColor = ElectricCyan,
        indicator = { tabPositions ->
          if (selectedTab < tabPositions.size) {
            Box(
              modifier = Modifier
                .tabIndicatorOffset(tabPositions[selectedTab])
                .height(2.5.dp)
                .background(ElectricCyan)
            )
          }
        },
        divider = {
          HorizontalDivider(color = Color(0xFF1E2D40), thickness = 1.dp)
        },
        modifier = Modifier.height(36.dp)
      ) {
        // Tab 0: Ceklis Wiring
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.PlaylistAddCheck,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (selectedTab == 0) ElectricCyan else TextTertiaryDark
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Wiring (${checkedCount}/${connections.size})",
                fontSize = 11.sp,
                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        )

        // Tab 1: Komponen & Skema
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (selectedTab == 1) ElectricCyan else TextTertiaryDark
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Komponen & Skema",
                fontSize = 11.sp,
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        )

        // Tab 2: Panduan Solder & Multimeter
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (selectedTab == 2) ElectricCyan else TextTertiaryDark
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Solder & Ukur",
                fontSize = 11.sp,
                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Tab Content Area (Scrollable internally)
      val tabScrollState = rememberScrollState()
      Box(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(tabScrollState)
      ) {
        when (selectedTab) {
          // TAB 0: CEKLIS WIRING PIN-KE-PIN (Default & Primary)
          0 -> {
            StepWiringChecklist(
              step = currentStep,
              connections = connections,
              checkedConnections = checkedConnections,
              onToggleConnection = { stepId, connId ->
                viewModel.toggleConnection(stepId, connId)
              },
              onCheckAllConnections = { stepId, ids, checkAll ->
                viewModel.setAllConnectionsForStep(stepId, ids, checkAll)
              },
              onOpenVerification = { viewModel.openVerificationDialog() },
              modifier = Modifier.fillMaxWidth()
            )
          }

          // TAB 1: KOMPONEN & SKEMA DETAIL
          1 -> {
            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              // Titik Sambung Utama Card
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                border = BorderStroke(1.dp, Color(0xFF1E2D40)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "TITIK SAMBUNG UTAMA",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(6.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("PIN ASAL (SOURCE):", fontSize = 9.sp, color = TextSecondaryDark, fontFamily = FontFamily.Monospace)
                      Text(currentStep.sourcePin, fontSize = 12.sp, color = SparkAmber, fontWeight = FontWeight.Bold)
                    }

                    Icon(
                      imageVector = Icons.Default.ArrowForward,
                      contentDescription = null,
                      tint = ElectricCyan,
                      modifier = Modifier.size(18.dp)
                    )

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                      Text("PIN TUJUAN (TARGET):", fontSize = 9.sp, color = TextSecondaryDark, fontFamily = FontFamily.Monospace)
                      Text(currentStep.targetPin, fontSize = 12.sp, color = SafetyGreen, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }

              // Komponen yang Disolder Card
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                border = BorderStroke(1.dp, Color(0xFF1E2D40)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "DAFTAR KOMPONEN LANGKAH INI",
                    style = MaterialTheme.typography.labelSmall,
                    color = SparkAmber,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(6.dp))

                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentStep.components.forEach { comp ->
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF121B27),
                        border = BorderStroke(0.8.dp, Color(0xFF223247)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Row(
                          modifier = Modifier.padding(8.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E2D42)
                          ) {
                            Text(
                              text = comp.ref,
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Bold,
                              fontFamily = FontFamily.Monospace,
                              color = ElectricCyan,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                          }
                          Spacer(modifier = Modifier.width(8.dp))
                          Column(modifier = Modifier.weight(1f)) {
                            Text(comp.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("Nilai: ${comp.spec} • ${comp.pinDescription}", fontSize = 10.sp, color = TextSecondaryDark)
                          }
                        }
                      }
                    }
                  }
                }
              }

              // Skema Netlist Trace Card
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                border = BorderStroke(1.dp, Color(0xFF1E2D40)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "SKEMA RANGKAIAN (NETLIST)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF090D14),
                    border = BorderStroke(0.8.dp, Color(0xFF1B2838)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = currentStep.schematicTrace,
                      fontSize = 11.sp,
                      fontFamily = FontFamily.Monospace,
                      color = Color(0xFFB0BEC5),
                      lineHeight = 16.sp,
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                }
              }
            }
          }

          // TAB 2: PANDUAN SOLDER & MULTIMETER
          2 -> {
            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              // Critical Safety Warning (if present)
              currentStep.criticalSafetyWarning?.let { warning ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFF330B0B),
                  border = BorderStroke(1.5.dp, HighVoltageRed),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Icon(
                      imageVector = Icons.Default.Warning,
                      contentDescription = null,
                      tint = HighVoltageRed,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = "PERINGATAN KESELAMATAN KRITIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = HighVoltageRed
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = warning,
                        fontSize = 11.sp,
                        color = Color(0xFFFFCDD2),
                        lineHeight = 15.sp
                      )
                    }
                  }
                }
              }

              // Panduan Solder di PCB Lubang
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                border = BorderStroke(1.dp, Color(0xFF1E2D40)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "PANDUAN PENYOLDERAN PCB LUBANG",
                    style = MaterialTheme.typography.labelSmall,
                    color = SparkAmber,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(6.dp))

                  Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    currentStep.perfboardTips.forEachIndexed { i, tip ->
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                      ) {
                        Surface(
                          shape = CircleShape,
                          color = Color(0xFF1E2D40),
                          modifier = Modifier.size(18.dp)
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Text(
                              text = "${i + 1}",
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = SparkAmber
                            )
                          }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = tip,
                          fontSize = 11.sp,
                          color = TextPrimaryDark,
                          lineHeight = 15.sp,
                          modifier = Modifier.weight(1f)
                        )
                      }
                    }
                  }
                }
              }

              // Orientasi Kaki Pin
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                border = BorderStroke(1.dp, Color(0xFF1E2D40)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "ATURAN KAKI PIN & ORIENTASI",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = currentStep.pinLegGuide,
                    fontSize = 11.sp,
                    color = TextPrimaryDark,
                    lineHeight = 15.sp
                  )
                }
              }

              // Multimeter Requirement Card & Quick Measure Button
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isCurrentVerified) Color(0xFF00331A) else Color(0xFF121D2C),
                border = BorderStroke(1.dp, if (isCurrentVerified) SafetyGreen else SparkAmber),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "VERIFIKASI PENGUKURAN MULTIMETER",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isCurrentVerified) SafetyGreen else SparkAmber,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )

                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = if (isCurrentVerified) SafetyGreen else SparkAmber
                    ) {
                      Text(
                        text = if (isCurrentVerified) "LOLOS UJI ✓" else "TARGET WAJIB",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  Text(
                    text = "Metode Uji: ${currentStep.verificationRequirement}",
                    fontSize = 11.sp,
                    color = TextPrimaryDark,
                    lineHeight = 15.sp
                  )

                  Spacer(modifier = Modifier.height(4.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Target Nilai: ${currentStep.expectedValue}",
                      fontSize = 11.sp,
                      color = SafetyGreen,
                      fontWeight = FontWeight.Bold
                    )

                    Button(
                      onClick = { viewModel.openVerificationDialog() },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentVerified) SafetyGreen else SparkAmber,
                        contentColor = Color.Black
                      ),
                      shape = RoundedCornerShape(6.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                      modifier = Modifier.height(28.dp)
                    ) {
                      Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = if (isCurrentVerified) "Ubah Nilai" else "Input Hasil Ukur",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
            }
          }
        }
      }
    }

    // 5. DOCKED BOTTOM ACTION BAR
    Surface(
      color = TechSurfaceElevated,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Prev Step
        OutlinedButton(
          onClick = { viewModel.prevStep() },
          enabled = uiState.selectedStepIndex > 0,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.height(38.dp)
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Prev", fontSize = 11.sp)
        }

        // Central Action: Verifikasi Hasil Ukur
        Button(
          onClick = { viewModel.openVerificationDialog() },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isCurrentVerified) SafetyGreen else if (isAllWiresChecked) SparkAmber else Color(0xFF1E2D40),
            contentColor = if (isCurrentVerified || isAllWiresChecked) Color.Black else TextPrimaryDark
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.height(38.dp)
        ) {
          Icon(
            imageVector = if (isCurrentVerified) Icons.Default.CheckCircle else Icons.Default.Speed,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isCurrentVerified) "Terverifikasi ✓" else "Verifikasi Langkah",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Next Step
        Button(
          onClick = { viewModel.nextStep() },
          enabled = uiState.selectedStepIndex < viewModel.allSteps.size - 1,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ElectricCyan,
            contentColor = Color.Black
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.height(38.dp)
        ) {
          Text("Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.width(4.dp))
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
        }
      }
    }
  }

  // Verification Dialog Popup
  if (uiState.showVerificationDialog) {
    StepVerificationDialog(
      step = currentStep,
      currentMeasuredValue = currentRecord?.measuredValue ?: "",
      currentNotes = currentRecord?.userNotes ?: "",
      onDismiss = { viewModel.closeVerificationDialog() },
      onConfirmVerification = { measured, notes ->
        viewModel.verifyCurrentStep(measured, notes)
      }
    )
  }
}
