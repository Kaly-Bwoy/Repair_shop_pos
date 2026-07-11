package com.mobilehub.pos.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilehub.pos.data.local.AppDatabase
import com.mobilehub.pos.data.local.entity.Settings
import com.mobilehub.pos.data.repository.BackupRepository
import com.mobilehub.pos.util.SecurityUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class SettingsState(
    val shopName: String = "MobileHub POS",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val currencySymbol: String = "GH₵",
    val lowStockThreshold: String = "5",
    val message: String? = null,
    val isSuccess: Boolean = false,
    val isLoading: Boolean = false
)

class SettingsViewModel(
    private val db: AppDatabase,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val expenseDao = db.expenseDao()
    private val adminDao = db.adminDao()

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val name = expenseDao.getSetting("shop_name")?.value ?: "MobileHub POS"
            val phone = expenseDao.getSetting("shop_phone")?.value ?: ""
            val address = expenseDao.getSetting("shop_address")?.value ?: ""
            val currency = expenseDao.getSetting("currency_symbol")?.value ?: "GH₵"
            val lowStock = expenseDao.getSetting("low_stock_alert_threshold")?.value ?: "5"

            _state.update {
                it.copy(
                    shopName = name,
                    shopPhone = phone,
                    shopAddress = address,
                    currencySymbol = currency,
                    lowStockThreshold = lowStock,
                    isLoading = false
                )
            }
        }
    }

    fun saveSettings(name: String, phone: String, address: String, currency: String, lowStock: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            expenseDao.saveSetting(Settings("shop_name", name))
            expenseDao.saveSetting(Settings("shop_phone", phone))
            expenseDao.saveSetting(Settings("shop_address", address))
            expenseDao.saveSetting(Settings("currency_symbol", currency))
            expenseDao.saveSetting(Settings("low_stock_alert_threshold", lowStock))

            // Update Store Profile also for dynamic layout injection
            val currentProfile = adminDao.getStoreProfile()
            if (currentProfile != null) {
                adminDao.insertStoreProfile(
                    currentProfile.copy(
                        storeName = name,
                        phone = phone,
                        address = address,
                        currency = currency,
                        taxRate = currentProfile.taxRate
                    )
                )
            }

            _state.update {
                it.copy(
                    shopName = name,
                    shopPhone = phone,
                    shopAddress = address,
                    currencySymbol = currency,
                    lowStockThreshold = lowStock,
                    message = "Settings saved successfully!",
                    isSuccess = true,
                    isLoading = false
                )
            }
        }
    }

    fun exportDatabaseBackup(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Read encryption key from current profile adminPin
                val profile = adminDao.getStoreProfile()
                val key = profile?.adminPin ?: "1234" // Use pin hash as encryption key

                val backupDir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(backupDir, "MobileHubPOS_Backup.json")
                val result = backupRepository.exportBackup(file, key)
                
                if (result.isSuccess) {
                    _state.update {
                        it.copy(
                            message = "Secure AES Encrypted Backup exported to folder: ${file.absolutePath}",
                            isSuccess = true,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            message = "Failed to export backup: ${result.exceptionOrNull()?.message}",
                            isSuccess = false,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        message = "Error: ${e.message}",
                        isSuccess = false,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun importDatabaseBackup(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Read current key to authenticate incoming backup
                val profile = adminDao.getStoreProfile()
                val key = profile?.adminPin ?: "1234"

                val backupDir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(backupDir, "MobileHubPOS_Backup.json")
                if (!file.exists()) {
                    _state.update {
                        it.copy(
                            message = "No backup file found at ${file.absolutePath}. Please make sure you have exported one first.",
                            isSuccess = false,
                            isLoading = false
                        )
                    }
                    return@launch
                }
                
                val result = backupRepository.importBackup(file, key)
                if (result.isSuccess) {
                    _state.update {
                        it.copy(
                            message = "Secure AES Database Backup restored successfully!",
                            isSuccess = true,
                            isLoading = false
                        )
                    }
                    loadSettings() // Reload UI settings from restored db
                } else {
                    _state.update {
                        it.copy(
                            message = "Restore failed: Either the backup file has been tampered with, or the original Admin PIN doesn't match this device key.",
                            isSuccess = false,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        message = "Error: ${e.message}",
                        isSuccess = false,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
