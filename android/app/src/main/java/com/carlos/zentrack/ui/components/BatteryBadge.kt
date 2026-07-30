package com.carlos.zentrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.utils.rememberBatteryInfo

@Composable
fun BatteryBadge(currentTheme: ZenThemeConfig, modifier: Modifier = Modifier) {
    val batteryInfo = rememberBatteryInfo()
    val isLow = batteryInfo.level <= 15
    val batteryColor = when {
        batteryInfo.isCharging -> currentTheme.primaryAccent
        isLow -> Color(0xFFF7768E) // TokyoPink / Warning Red
        else -> currentTheme.primaryAccent.copy(alpha = 0.9f)
    }

    Surface(
        modifier = modifier,
        color = currentTheme.card.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, batteryColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (batteryInfo.isCharging) Icons.Default.Bolt else (if (isLow) Icons.Default.BatteryAlert else Icons.Default.BatteryFull),
                contentDescription = null,
                tint = batteryColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${batteryInfo.level}%",
                color = batteryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
