package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.WiringDataProvider
import com.example.model.adaptToPlatform
import com.example.ui.components.ComponentPinoutCard
import com.example.ui.theme.*
import com.example.viewmodel.WiringViewModel

@Composable
fun ComponentLibraryScreen(
  viewModel: WiringViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  val allComponents = remember(uiState.mcuPlatform) {
    WiringDataProvider.componentPinouts.map { it.adaptToPlatform(uiState.mcuPlatform) }
  }

  val filteredComponents = remember(searchQuery, allComponents) {
    if (searchQuery.isBlank()) allComponents
    else {
      val q = searchQuery.lowercase()
      allComponents.filter {
        it.ref.lowercase().contains(q) ||
          it.name.lowercase().contains(q) ||
          it.packageType.lowercase().contains(q) ||
          it.donorPsuRule.lowercase().contains(q) ||
          it.ratingSpec.lowercase().contains(q)
      }
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TechDarkBg)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
  ) {
    item {
      Text(
        text = "ENSIKLOPEDIA PIN KAKI & ORIENTASI KOMPONEN",
        style = MaterialTheme.typography.titleMedium,
        color = ElectricCyan,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = "Panduan fisik: Pin 1, 2, 3, Notch/Tab, dan aturan donasi dari PSU ATX PC bekas",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondaryDark
      )
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Cari Komponen (contoh: BT151, IRF3205, LM339, Trafo)") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ElectricCyan)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark)
            }
          }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = ElectricCyan,
          unfocusedBorderColor = OutlineDark,
          focusedTextColor = TextPrimaryDark,
          unfocusedTextColor = TextPrimaryDark,
          focusedContainerColor = TechSurfaceElevated,
          unfocusedContainerColor = TechSurfaceElevated
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      )
    }

    items(filteredComponents, key = { it.ref }) { component ->
      ComponentPinoutCard(component = component)
    }
  }
}
