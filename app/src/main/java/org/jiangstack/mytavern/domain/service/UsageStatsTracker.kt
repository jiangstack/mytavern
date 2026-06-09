package org.jiangstack.mytavern.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jiangstack.mytavern.data.remote.Usage
import java.util.concurrent.atomic.AtomicLong

/**
 * 本次 App 使用期间的 LLM 用量统计（不持久化）。
 * 线程安全，可在多个协程中并发调用。
 */
object UsageStatsTracker {

    data class Stats(
        val promptTokens: Long = 0,
        val cachedTokens: Long = 0,
        val completionTokens: Long = 0,
        val cost: Double = 0.0,
        val requestCount: Long = 0
    )

    private val _promptTokens = AtomicLong(0)
    private val _cachedTokens = AtomicLong(0)
    private val _completionTokens = AtomicLong(0)
    private val _costMicros = AtomicLong(0) // cost * 1_000_000，避免浮点原子操作
    private val _requestCount = AtomicLong(0)

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun recordUsage(usage: Usage) {
        val prompt = usage.prompt_tokens ?: 0
        val cached = usage.prompt_tokens_details?.cached_tokens ?: 0
        val completion = usage.completion_tokens ?: 0
        val costMicros = ((usage.cost ?: 0.0) * 1_000_000).toLong()

        _promptTokens.addAndGet(prompt.toLong())
        _cachedTokens.addAndGet(cached.toLong())
        _completionTokens.addAndGet(completion.toLong())
        _costMicros.addAndGet(costMicros)
        _requestCount.incrementAndGet()

        _stats.value = Stats(
            promptTokens = _promptTokens.get(),
            cachedTokens = _cachedTokens.get(),
            completionTokens = _completionTokens.get(),
            cost = _costMicros.get() / 1_000_000.0,
            requestCount = _requestCount.get()
        )
    }
}
