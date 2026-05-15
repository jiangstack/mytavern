package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jiangstack.mytavern.domain.model.HttpLog
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class HttpLogRepository {
    private val logs = CopyOnWriteArrayList<HttpLog>()
    private val nextId = AtomicLong(1)

    private val _logsFlow = MutableStateFlow<List<HttpLog>>(emptyList())
    val logsFlow: StateFlow<List<HttpLog>> = _logsFlow.asStateFlow()

    companion object {
        private const val MAX_LOGS = 100
    }

    fun add(log: HttpLog) {
        logs.add(log)
        if (logs.size > MAX_LOGS) {
            logs.removeAt(0)
        }
        _logsFlow.value = logs.toList().asReversed()
    }

    fun getAll(): List<HttpLog> {
        return logs.toList().asReversed()
    }

    fun clear() {
        logs.clear()
        _logsFlow.value = emptyList()
    }

    fun nextId(): Long = nextId.getAndIncrement()
}
