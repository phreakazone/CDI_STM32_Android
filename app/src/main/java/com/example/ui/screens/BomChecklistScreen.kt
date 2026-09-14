package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WiringDataProvider
import com.example.model.BomItem
import com.example.model.adaptToPlatform
import com.example.ui.theme.*
import com.example.viewmodel.WiringViewModel

@Composable
fun BomChecklistScreen(
  viewModel: WiringViewModel,
  modifier: Modifier = Modifier
) {
  val acquiredIds by viewModel.acquiredBomIds.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  var selectedCategory by remember { mutableStateOf("SEMUA") }
  val allBomItems = remember(uiState.mcuPlatform) {
    WiringDataProvider.bomItems.map { it.adaptToPlatform(uiState.mcuPlatform) }
  }

  val filteredItems = remember(selectedCategory, allBomItems) {
    if (selectedCategory == "SEMUA") allBomItems
    else allBomItems.filter { it.section.contains(selectedCategory, ignoreCase = true) }
  }

  val totalCount = allBomItems.size
  val acquiredCount = allBomItems.count { acquiredIds.contains(it.id) }
  val progressPercent = if (totalCount > 0) ((acquiredCount.toFloat() / totalCount) * 100).toInt() else 0

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TechDarkBg)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
  ) {
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(OutlineDark, ElectricCyan.copy(alpha = 0.4f)))),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = SparkAmber, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "DAFTAR BELANJA & DONOR PSU",
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }

            Text(
              text = "$acquiredCount / $totalCount Siap ($progressPercent%)",
              style = MaterialTheme.typography.labelSmall,
              color = SafetyGreen,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          LinearProgressIndicator(
            progress = { if (totalCount > 0) acquiredCount.toFloat() / totalCount else 0f },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = SafetyGreen,
            trackColor = Color(0xFF1B2C42)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Komponen ditandai 'DONOR PSU' dapat diambil dari PSU komputer bekas yang masih bagus.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextSecondaryDark
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("SEMUA", "LOGIC", "POWER", "HARNESS", "AUDIO", "PASSIVE").forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ElectricCyan,
              selectedLabelColor = Color.Black,
              containerColor = TechSurfaceElevated,
              labelColor = TextPrimaryDark
            )
          )
        }
      }
    }

    items(filteredItems, key = { it.id }) { item ->
      BomItemCard(
        item = item,
        isAcquired = acquiredIds.contains(item.id),
        onToggle = { viewModel.toggleBomAcquired(item.id) }
      )
    }
  }
}

@Composable
fun BomItemCard(
  item: BomItem,
  isAcquired: Boolean,
  onToggle: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isAcquired) Color(0xFF092015) else TechSurfaceElevated
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(
        listOf(
          if (isAcquired) SafetyGreen.copy(alpha = 0.5f) else OutlineDark,
          if (isAcquired) SafetyGreen.copy(alpha = 0.2f) else OutlineDark
        )
      )
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggle() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = isAcquired,
        onCheckedChange = { onToggle() },
        colors = CheckboxDefaults.colors(
          checkedColor = SafetyGreen,
          checkmarkColor = Color.Black,
          uncheckedColor = OutlineDark
        )
      )

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = item.ref,
            style = MaterialTheme.typography.titleMedium,
            color = if (isAcquired) SafetyGreen else SparkAmber,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (item.source.contains("DONOR", ignoreCase = true)) SparkAmber.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f)
          ) {
            Text(
              text = item.source,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              color = if (item.source.contains("DONOR", ignoreCase = true)) SparkAmber else ElectricCyan,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Text(
          text = "${item.section} • Qty: ${item.qty}",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimaryDark,
          fontWeight = FontWeight.Medium
        )

        Text(
          text = "Spesifikasi: ${item.spec}",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = TextSecondaryDark
        )

        if (item.notes.isNotBlank()) {
          Text(
            text = "Catatan: ${item.notes}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = TextTertiaryDark
          )
        }
      }
    }
  }
}
