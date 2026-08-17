package org.hlcyn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import org.hlcyn.ui.R
import kotlin.math.roundToInt

private class HalcyonBottomBarState {
    var indicatorTargetX by mutableFloatStateOf(0f)
    var indicatorTargetWidth by mutableFloatStateOf(0f)
    var isInitialized by mutableStateOf(false)

    fun updateSelectedBounds(x: Float, width: Float) {
        indicatorTargetX = x
        indicatorTargetWidth = width
        isInitialized = true
    }
}

private val LocalHalcyonBottomBarState = compositionLocalOf<HalcyonBottomBarState?> { null }

@Composable
fun HalcyonFloatingBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val barState = remember { HalcyonBottomBarState() }

    CompositionLocalProvider(LocalHalcyonBottomBarState provides barState) {
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
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = dimensionResource(R.dimen.nest_bottom_bar_padding_horizontal),
                            vertical = dimensionResource(R.dimen.nest_bottom_bar_padding_vertical)
                        )
                        .fillMaxSize()
                ) {
                    // Sliding active indicator chip background
                    if (barState.isInitialized && barState.indicatorTargetWidth > 0f) {
                        val animatedX by animateFloatAsState(
                            targetValue = barState.indicatorTargetX,
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "indicatorX"
                        )
                        val animatedWidth by animateFloatAsState(
                            targetValue = barState.indicatorTargetWidth,
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "indicatorWidth"
                        )

                        Surface(
                            modifier = Modifier
                                .offset { IntOffset(animatedX.roundToInt(), 0) }
                                .width(with(LocalDensity.current) { animatedWidth.toDp() })
                                .fillMaxHeight(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = dimensionResource(R.dimen.nest_bottom_bar_indicator_tonal_elevation),
                            shadowElevation = dimensionResource(R.dimen.nest_bottom_bar_indicator_shadow_elevation)
                        ) {}
                    }

                    // Row containing the item icons and click listeners
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.nest_bottom_bar_item_spacing)
                        ),
                        content = content
                    )
                }
            }
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
    val barState = LocalHalcyonBottomBarState.current

    // Smoothly fade icon color between active and inactive states
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "iconColor"
    )

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                if (selected) {
                    val x = coordinates.positionInParent().x
                    val width = coordinates.size.width.toFloat()
                    barState?.updateSelectedBounds(x, width)
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(dimensionResource(R.dimen.nest_bottom_bar_icon_size))
        )
    }
}
