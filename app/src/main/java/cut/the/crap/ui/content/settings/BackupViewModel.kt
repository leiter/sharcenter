package cut.the.crap.ui.content.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.backup.BackupInfo
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.platform.PlatformUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the backup-management screen: loads the list of existing backups and deletes
 * the ones the user selects.
 */
class BackupViewModel constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _backups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val backups: StateFlow<List<BackupInfo>> = _backups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    /** Reloads the backup list from storage. */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _backups.value = backupManager.listBackups()
            _isLoading.value = false
        }
    }

    /**
     * Deletes the given backups and refreshes the list.
     * @param onResult invoked with the number of files actually deleted.
     */
    fun deleteBackups(uris: List<PlatformUri>, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val deleted = backupManager.deleteBackups(uris).getOrDefault(0)
            _backups.value = backupManager.listBackups()
            onResult(deleted)
        }
    }
}
