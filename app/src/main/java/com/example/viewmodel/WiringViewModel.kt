package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WiringDataProvider
import com.example.data.WiringRepository
import com.example.model.*
import id.ns200.cdir7.McuPlatform
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val iconLabel: String) {
  TUTORIAL("Langkah Wiring", "Wiring"),
  HARNESS_J1("Harness J1", "Harness"),
  WEACT_HEADER("Pinout MCU", "MCU"),
  PINOUT_LIBRARY("Kaki Komponen", "Pinout"),
  BOM_CHECKLIST("Daftar Belanja", "BOM"),
  QUICK_SETUP("Quick Setup", "Setup")
}

data class WiringUiState(
  val currentTab: AppTab = AppTab.TUTORIAL,
  val selectedStepIndex: Int = 0,
  val searchQuery: String = "",
  val selectedHarnessPin: HarnessPin? = null,
  val selectedWeActPin: WeActPin? = null,
  val selectedEsp32Pin: Esp32Pin? = null,
  val mcuPlatform: McuPlatform = McuPlatform.STM32WB55,
  val selectedComponentPinout: ComponentPinout? = null,
  val bomFilterSection: String = "SEMUA",
  val showVerificationDialog: Boolean = false,
  val showResetDialog: Boolean = false,
  val showPinDetailBottomSheet: Boolean = false,
  val showFullCircuitSimulator: Boolean = false
)

class WiringViewModel(application: Application) : AndroidViewModel(application) {
  val repository = WiringRepository(application.applicationContext)

  private val _uiState = MutableStateFlow(WiringUiState(mcuPlatform = repository.mcuPlatform.value))
  val uiState: StateFlow<WiringUiState> = _uiState.asStateFlow()

  val verificationRecords = repository.verificationRecords
  val acquiredBomIds = repository.acquiredBomIds
  val checkedConnections = repository.checkedConnections
  val enforceSequential = repository.enforceSequentialVerification
  val mcuPlatform: StateFlow<McuPlatform> = repository.mcuPlatform

  init {
    viewModelScope.launch {
      repository.mcuPlatform.collect { platform ->
        _uiState.update { it.copy(mcuPlatform = platform) }
      }
    }
  }

  fun setMcuPlatform(platform: McuPlatform) {
    repository.setMcuPlatform(platform)
    _uiState.update { it.copy(mcuPlatform = platform) }
  }

  val allSteps: List<WiringStep> = WiringDataProvider.wiringSteps

  val currentStep: StateFlow<WiringStep> = _uiState.map { state ->
    allSteps.getOrElse(state.selectedStepIndex.coerceIn(0, allSteps.size - 1)) { allSteps.first() }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, allSteps.first())

  val verificationProgress: StateFlow<Pair<Int, Int>> = verificationRecords.map { records ->
    val verifiedCount = allSteps.count { step -> records[step.id]?.isVerified == true }
    Pair(verifiedCount, allSteps.size)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(0, allSteps.size))

  fun setTab(tab: AppTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  fun selectStep(index: Int) {
    val clamped = index.coerceIn(0, allSteps.size - 1)
    _uiState.update { it.copy(selectedStepIndex = clamped) }
    repository.setCurrentStep(allSteps[clamped].id)
  }

  fun nextStep() {
    val nextIdx = _uiState.value.selectedStepIndex + 1
    if (nextIdx < allSteps.size) {
      selectStep(nextIdx)
    }
  }

  fun prevStep() {
    val prevIdx = _uiState.value.selectedStepIndex - 1
    if (prevIdx >= 0) {
      selectStep(prevIdx)
    }
  }

  fun openVerificationDialog() {
    _uiState.update { it.copy(showVerificationDialog = true) }
  }

  fun closeVerificationDialog() {
    _uiState.update { it.copy(showVerificationDialog = false) }
  }

  fun verifyCurrentStep(measuredValue: String, userNotes: String) {
    val step = currentStep.value
    repository.setVerificationRecord(
      stepId = step.id,
      isVerified = true,
      measuredValue = measuredValue,
      userNotes = userNotes
    )
    closeVerificationDialog()
    // Auto advance to next step if available
    val nextIdx = _uiState.value.selectedStepIndex + 1
    if (nextIdx < allSteps.size) {
      selectStep(nextIdx)
    }
  }

  fun unverifyStep(stepId: String) {
    repository.setVerificationRecord(stepId, false, "", "")
  }

  fun selectHarnessPin(pin: HarnessPin?) {
    _uiState.update { it.copy(selectedHarnessPin = pin, showPinDetailBottomSheet = pin != null) }
  }

  fun selectWeActPin(pin: WeActPin?) {
    _uiState.update { it.copy(selectedWeActPin = pin, showPinDetailBottomSheet = pin != null) }
  }

  fun selectEsp32Pin(pin: Esp32Pin?) {
    _uiState.update { it.copy(selectedEsp32Pin = pin, showPinDetailBottomSheet = pin != null) }
  }

  fun selectComponentPinout(comp: ComponentPinout?) {
    _uiState.update { it.copy(selectedComponentPinout = comp) }
  }

  fun setBomFilter(section: String) {
    _uiState.update { it.copy(bomFilterSection = section) }
  }

  fun toggleBomAcquired(id: String) {
    repository.toggleBomAcquired(id)
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun toggleEnforceSequential(enforce: Boolean) {
    repository.toggleEnforceSequential(enforce)
  }

  fun resetAllProgress() {
    repository.resetAllProgress()
    _uiState.update { it.copy(selectedStepIndex = 0, showResetDialog = false) }
  }

  fun openResetDialog() {
    _uiState.update { it.copy(showResetDialog = true) }
  }

  fun closeResetDialog() {
    _uiState.update { it.copy(showResetDialog = false) }
  }

  fun toggleFullCircuitSimulator() {
    _uiState.update { it.copy(showFullCircuitSimulator = !it.showFullCircuitSimulator) }
  }

  fun setFullCircuitSimulator(show: Boolean) {
    _uiState.update { it.copy(showFullCircuitSimulator = show) }
  }

  fun toggleConnection(stepId: String, connectionId: String) {
    repository.toggleConnectionChecked(stepId, connectionId)
  }

  fun setAllConnectionsForStep(stepId: String, connectionIds: List<String>, checked: Boolean) {
    repository.setAllConnectionsForStep(stepId, connectionIds, checked)
  }

  fun closePinDetail() {
    _uiState.update { it.copy(showPinDetailBottomSheet = false, selectedHarnessPin = null, selectedWeActPin = null, selectedEsp32Pin = null) }
  }
}
