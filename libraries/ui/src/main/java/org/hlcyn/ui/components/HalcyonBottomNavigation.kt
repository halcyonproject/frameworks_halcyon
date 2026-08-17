package org.hlcyn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import org.hlcyn.ui.R

@Composable
fun HalcyonFloatingBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.nest_bottom_bar_margin_bottom)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(dimensionResource(R.dimen.nest_bottom_bar_height)),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            tonalElevation = dimensionResource(R.dimen.nest_bottom_bar_tonal_elevation),
            shadowElevation = dimensionResource(R.dimen.nest_bottom_bar_shadow_elevation)
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = dimensionResource(R.dimen.nest_bottom_bar_padding_horizontal),
                        vertical = dimensionResource(R.dimen.nest_bottom_bar_padding_vertical)
                    )
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.nest_bottom_bar_item_spacing)
                ),
                content = content
            )
        }
    }
}

@Composable
fun RowScope.HalcyonFloatingBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight(),
        shape = CircleShape,
        color = containerColor,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(dimensionResource(R.dimen.nest_bottom_bar_icon_size))
            )
        }
    }
}
