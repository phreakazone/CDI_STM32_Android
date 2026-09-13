package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.VerificationRecord
import id.ns200.cdir7.McuPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class WiringRepository(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("cdi_wiring_prefs", Context.MODE_PRIVATE)

  private val _verificationRecords = MutableStateFlow<Map<String, VerificationRecord>>(emptyMap())
  val verificationRecords: StateFlow<Map<String, VerificationRecord>> = _verificationRecords.asStateFlow()

  private val _acquiredBomIds = MutableStateFlow<Set<String>>(emptySet())
  val acquiredBomIds: StateFlow<Set<String>> = _acquiredBomIds.asStateFlow()

  private val _checkedConnections = MutableStateFlow<Set<String>>(emptySet())
  val checkedConnections: StateFlow<Set<String>> = _checkedConnections.asStateFlow()

  private val _currentStepId = MutableStateFlow<String>("step_1_1")
  val currentStepId: StateFlow<String> = _currentStepId.asStateFlow()

  private val _enforceSequentialVerification = MutableStateFlow<Boolean>(true)
  val enforceSequentialVerification: StateFlow<Boolean> = _enforceSequentialVerification.asStateFlow()

  private val _mcuPlatform = MutableStateFlow<McuPlatform>(McuPlatform.STM32WB55)
  val mcuPlatform: StateFlow<McuPlatform> = _mcuPlatform.asStateFlow()

  init {
    loadPreferences()
  }

  private fun loadPreferences() {
    val recordsJson = prefs.getString("verification_records", "{}") ?: "{}"
    val map = mutableMapOf<String, VerificationRecord>()
    try {
      val json = JSONObject(recordsJson)
      val keys = json.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val itemObj = json.getJSONObject(key)
        map[key] = VerificationRecord(
          stepId = itemObj.optString("stepId", key),
          isVerified = itemObj.optBoolean("isVerified", false),
          measuredValue = itemObj.optString("measuredValue", ""),
          userNotes = itemObj.optString("userNotes", ""),
          verifiedTimestamp = itemObj.optLong("verifiedTimestamp", 0L)
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    _verificationRecords.value = map

    val bomSet = prefs.getStringSet("acquired_bom_ids", emptySet()) ?: emptySet()
    _acquiredBomIds.value = bomSet.toSet()

    val connSet = prefs.getStringSet("checked_connections", emptySet()) ?: emptySet()
    _checkedConnections.value = connSet.toSet()

    _currentStepId.value = prefs.getString("last_active_step", "step_1_1") ?: "step_1_1"
    _enforceSequentialVerification.value = prefs.getBoolean("enforce_sequential", true)
    val platformId = prefs.getString("mcu_platform", McuPlatform.STM32WB55.id) ?: McuPlatform.STM32WB55.id
    _mcuPlatform.value = McuPlatform.fromId(platformId)
  }

  fun setMcuPlatform(platform: McuPlatform) {
    _mcuPlatform.value = platform
    prefs.edit().putString("mcu_platform", platform.id).apply()
  }

  fun toggleConnectionChecked(stepId: String, connectionId: String) {
    val key = "${stepId}_$connectionId"
    val set = _checkedConnections.value.toMutableSet()
    if (set.contains(key)) {
      set.remove(key)
    } else {
      set.add(key)
    }
    _checkedConnections.value = set
    prefs.edit().putStringSet("checked_connections", set).apply()
  }

  fun setAllConnectionsForStep(stepId: String, connectionIds: List<String>, checked: Boolean) {
    val set = _checkedConnections.value.toMutableSet()
    connectionIds.forEach { connId ->
      val key = "${stepId}_$connId"
      if (checked) set.add(key) else set.remove(key)
    }
    _checkedConnections.value = set
    prefs.edit().putStringSet("checked_connections", set).apply()
  }

  fun isConnectionChecked(stepId: String, connectionId: String): Boolean {
    return _checkedConnections.value.contains("${stepId}_$connectionId")
  }

  fun setVerificationRecord(stepId: String, isVerified: Boolean, measuredValue: String, userNotes: String) {
    val current = _verificationRecords.value.toMutableMap()
    current[stepId] = VerificationRecord(
      stepId = stepId,
      isVerified = isVerified,
      measuredValue = measuredValue,
      userNotes = userNotes,
      verifiedTimestamp = if (isVerified) System.currentTimeMillis() else 0L
    )
    _verificationRecords.value = current
    saveRecordsToPrefs(current)
  }

  private fun saveRecordsToPrefs(records: Map<String, VerificationRecord>) {
    val json = JSONObject()
    for ((key, record) in records) {
      val itemObj = JSONObject()
      itemObj.put("stepId", record.stepId)
      itemObj.put("isVerified", record.isVerified)
      itemObj.put("measuredValue", record.measuredValue)
      itemObj.put("userNotes", record.userNotes)
      itemObj.put("verifiedTimestamp", record.verifiedTimestamp)
      json.put(key, itemObj)
    }
    prefs.edit().putString("verification_records", json.toString()).apply()
  }

  fun toggleBomAcquired(id: String) {
    val current = _acquiredBomIds.value.toMutableSet()
    if (current.contains(id)) {
      current.remove(id)
    } else {
      current.add(id)
    }
    _acquiredBomIds.value = current
    prefs.edit().putStringSet("acquired_bom_ids", current).apply()
  }

  fun setCurrentStep(stepId: String) {
    _currentStepId.value = stepId
    prefs.edit().putString("last_active_step", stepId).apply()
  }

  fun toggleEnforceSequential(enforce: Boolean) {
    _enforceSequentialVerification.value = enforce
    prefs.edit().putBoolean("enforce_sequential", enforce).apply()
  }

  fun resetAllProgress() {
    _verificationRecords.value = emptyMap()
    _checkedConnections.value = emptySet()
    _currentStepId.value = "step_1_1"
    prefs.edit()
      .remove("verification_records")
      .remove("checked_connections")
      .putString("last_active_step", "step_1_1")
      .apply()
  }

  fun isStepUnlocked(stepIndex: Int, stepsCount: Int): Boolean {
    if (!_enforceSequentialVerification.value) return true
    if (stepIndex == 0) return true
    val allSteps = WiringDataProvider.wiringSteps
    // Check if the immediately preceding step is verified
    val previousStep = allSteps.getOrNull(stepIndex - 1) ?: return true
    val record = _verificationRecords.value[previousStep.id]
    return record?.isVerified == true
  }
}
