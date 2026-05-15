package org.jiangstack.mytavern.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import org.jiangstack.mytavern.data.repository.HttpLogRepository
import org.jiangstack.mytavern.domain.model.HttpLog

class HttpLogViewModel(
    private val httpLogRepository: HttpLogRepository
) : ViewModel() {

    val logs: StateFlow<List<HttpLog>> = httpLogRepository.logsFlow

    fun clearLogs() {
        httpLogRepository.clear()
    }

    companion object {
        fun factory(httpLogRepository: HttpLogRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HttpLogViewModel(httpLogRepository) as T
                }
            }
        }
    }
}
