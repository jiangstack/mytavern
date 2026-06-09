package org.jiangstack.mytavern.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.service.UsageStatsTracker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(
    onNavigateBack: () -> Unit
) {
    val stats by UsageStatsTracker.stats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.usage_stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 请求次数
            StatCard(
                label = stringResource(R.string.usage_stats_request_count),
                value = "${stats.requestCount}"
            )

            // 输入 Token
            StatCard(
                label = stringResource(R.string.usage_stats_prompt_tokens),
                value = formatNumber(stats.promptTokens)
            )

            // 缓存 Token
            StatCard(
                label = stringResource(R.string.usage_stats_cached_tokens),
                value = formatNumber(stats.cachedTokens)
            )

            // 输出 Token
            StatCard(
                label = stringResource(R.string.usage_stats_completion_tokens),
                value = formatNumber(stats.completionTokens)
            )

            // 总花费
            StatCard(
                label = stringResource(R.string.usage_stats_cost),
                value = formatCost(stats.cost)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatNumber(value: Long): String {
    return "%,d".format(value)
}

private fun formatCost(value: Double): String {
    return if (value < 0.01 && value > 0) {
        String.format("%.6f", value)
    } else {
        String.format("%.4f", value)
    }
}
