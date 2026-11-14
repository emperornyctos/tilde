package com.example.ui.components

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.WidgetHostManager
import com.example.ui.theme.LocalLauncherTokens

@Composable
fun SystemAppWidgetView(
    appWidgetId: Int,
    widgetHostManager: WidgetHostManager,
    widthDp: Dp,
    heightDp: Dp,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val hostView: AppWidgetHostView? = remember(appWidgetId) {
        widgetHostManager.createHostView(appWidgetId)
    }

    if (hostView == null) {
        TuiCard(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                TuiSectionHeader(text = "Widget unavailable")
                Text(
                    text = "id=$appWidgetId",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted
                )
            }
        }
        return
    }

    val wDpInt = widthDp.value.toInt().coerceAtLeast(60)
    val hDpInt = heightDp.value.toInt().coerceAtLeast(60)

    DisposableEffect(hostView, wDpInt, hDpInt) {
        val options = Bundle()
        runCatching {
            hostView.updateAppWidgetSize(options, wDpInt, hDpInt, wDpInt, hDpInt)
        }
        onDispose {}
    }

    TuiCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { hostView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
