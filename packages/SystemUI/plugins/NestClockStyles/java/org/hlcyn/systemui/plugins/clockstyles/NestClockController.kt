/*
 * SPDX-FileCopyrightText: 2022 The Android Open Source Project
 * SPDX-FileCopyrightText: 2026 The Halcyon Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hlcyn.systemui.plugins.clockstyles

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.icu.text.NumberFormat
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.annotation.VisibleForTesting
import com.android.systemui.log.core.MessageBuffer
import com.android.systemui.plugins.clocks.AlarmData
import com.android.systemui.plugins.clocks.ClockAnimations
import com.android.systemui.plugins.clocks.ClockAxisStyle
import com.android.systemui.plugins.clocks.ClockConfig
import com.android.systemui.plugins.clocks.ClockController
import com.android.systemui.plugins.clocks.ClockEventListener
import com.android.systemui.plugins.clocks.ClockEvents
import com.android.systemui.plugins.clocks.ClockFaceConfig
import com.android.systemui.plugins.clocks.ClockFaceController
import com.android.systemui.plugins.clocks.ClockFaceEvents
import com.android.systemui.plugins.clocks.ClockMessageBuffers
import com.android.systemui.plugins.clocks.ClockSettings
import com.android.systemui.plugins.clocks.DefaultClockFaceLayout
import com.android.systemui.plugins.clocks.ThemeConfig
import com.android.systemui.plugins.clocks.WeatherData
import com.android.systemui.plugins.clocks.ZenData
import java.io.PrintWriter
import java.util.Locale
import java.util.TimeZone

/**
 * Controls the default clock visuals.
 */
class NestClockController(
    private val clockId: String,
    private val ctx: Context,
    private val sysuiCtx: Context,
    private val layoutInflater: LayoutInflater,
    private val resources: Resources,
    private val sysuiResources: Resources,
    private val settings: ClockSettings?,
    messageBuffers: ClockMessageBuffers? = null,
) : ClockController {
    override val smallClock: DefaultClockFaceController
    override val largeClock: LargeClockFaceController
    private val clocks: List<AnimatableClockView>

    private val burmeseNf = NumberFormat.getInstance(Locale.forLanguageTag("my"))
    private val burmeseNumerals = burmeseNf.format(FORMAT_NUMBER.toLong())
    private val burmeseLineSpacing =
        resources.getFloat(R.dimen.keyguard_clock_line_spacing_scale_burmese)
    private val defaultLineSpacing by lazy {
        resources.getFloat(AnimatableClockView.getLineSpaceByClockId(clockId))
    }

    override val events: DefaultClockEvents
    override val config: ClockConfig by lazy {
        ClockConfig(
            clockId,
            getClockName(),
            getClockDescription(),
        )
    }

    init {
        smallClock =
            DefaultClockFaceController(
                AnimatableClockView.getSmallClockView(ctx, clockId),
                settings?.seedColor,
                messageBuffers?.smallClockMessageBuffer,
            )
        largeClock =
            LargeClockFaceController(
                AnimatableClockView.getLargeClockView(ctx, clockId),
                settings?.seedColor,
                messageBuffers?.largeClockMessageBuffer,
            )
        clocks = listOf(smallClock.view, largeClock.view)

        events = DefaultClockEvents()
        events.onLocaleChanged(Locale.getDefault())
    }

    override fun initialize(
        isDarkTheme: Boolean,
        dozeFraction: Float,
        foldFraction: Float,
        clockListener: ClockEventListener?,
    ) {
        largeClock.recomputePadding(null)

        largeClock.animations = LargeClockAnimations(largeClock.view, dozeFraction, foldFraction)
        smallClock.animations = DefaultClockAnimations(smallClock.view, dozeFraction, foldFraction)

        largeClock.events.onThemeChanged(largeClock.theme.copy(isDarkTheme = isDarkTheme))
        smallClock.events.onThemeChanged(smallClock.theme.copy(isDarkTheme = isDarkTheme))
        events.onTimeZoneChanged(TimeZone.getDefault())

        smallClock.events.onTimeTick()
        largeClock.events.onTimeTick()
    }

    open inner class DefaultClockFaceController(
        override val view: AnimatableClockView,
        seedColor: Int?,
        messageBuffer: MessageBuffer?,
    ) : ClockFaceController {
        private var currentColor = seedColor ?: Color.MAGENTA
        protected var targetRegion: Rect? = null

        override val config = ClockFaceConfig()
        override var theme = ThemeConfig(true, seedColor)
        override val layout =
            DefaultClockFaceLayout(view).apply {
                views[0].id =
                    sysuiResources.getIdentifier("lockscreen_clock_view", "id", sysuiCtx.packageName)
            }

        override var animations: DefaultClockAnimations = DefaultClockAnimations(view, 0f, 0f)
            internal set

        init {
            view.setColors(DOZE_COLOR, currentColor)
            messageBuffer?.let { view.messageBuffer = it }
        }

        override val events =
            object : ClockFaceEvents {
                override fun onTimeTick() = view.refreshTime()

                override fun onThemeChanged(theme: ThemeConfig) {
                    this@DefaultClockFaceController.theme = theme

                    val color = theme.getDefaultColor(sysuiCtx)
                    if (currentColor == color) {
                        return
                    }

                    currentColor = color
                    view.setColors(DOZE_COLOR, color)
                    if (!animations.dozeState.isActive) {
                        view.animateColorChange()
                    }
                }

                override fun onTargetRegionChanged(targetRegion: Rect?) {
                    this@DefaultClockFaceController.targetRegion = targetRegion
                    recomputePadding(targetRegion)
                }

                override fun onFontSettingChanged(fontSizePx: Float) {
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
                    recomputePadding(targetRegion)
                }

                override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) {}
            }

        open fun recomputePadding(targetRegion: Rect?) {}
    }

    inner class LargeClockFaceController(
        view: AnimatableClockView,
        seedColor: Int?,
        messageBuffer: MessageBuffer?,
    ) : DefaultClockFaceController(view, seedColor, messageBuffer) {
        override val layout =
            DefaultClockFaceLayout(view).apply {
                views[0].id =
                    sysuiResources.getIdentifier("lockscreen_clock_view_large", "id", sysuiCtx.packageName)
            }
        override val config = ClockFaceConfig(hasCustomPositionUpdatedAnimation = true)

        init {
            view.hasCustomPositionUpdatedAnimation = true
            animations = LargeClockAnimations(view, 0f, 0f)
        }

        override fun recomputePadding(targetRegion: Rect?) {}

        fun offsetGlyphsForStepClockAnimation(fromLeft: Int, direction: Int, fraction: Float) {
            view.offsetGlyphsForStepClockAnimation(fromLeft, direction, fraction)
        }

        fun offsetGlyphsForStepClockAnimation(distance: Float, fraction: Float) {
            view.offsetGlyphsForStepClockAnimation(distance, fraction)
        }
    }

    inner class DefaultClockEvents : ClockEvents {
        override var isReactiveTouchInteractionEnabled: Boolean = false

        override fun onTimeFormatChanged(is24Hr: Boolean) =
            clocks.forEach { it.refreshFormat(is24Hr) }

        override fun onTimeZoneChanged(timeZone: TimeZone) =
            clocks.forEach { it.onTimeZoneChanged(timeZone) }

        override fun onLocaleChanged(locale: Locale) {
            val nf = NumberFormat.getInstance(locale)
            if (nf.format(FORMAT_NUMBER.toLong()) == burmeseNumerals) {
                clocks.forEach { it.setLineSpacingScale(burmeseLineSpacing) }
            } else {
                clocks.forEach { it.setLineSpacingScale(defaultLineSpacing) }
            }

            clocks.forEach { it.refreshFormat() }
        }

        override fun onWeatherDataChanged(data: WeatherData) {}

        override fun onAlarmDataChanged(data: AlarmData) {}

        override fun onZenDataChanged(data: ZenData) {}
    }

    open inner class DefaultClockAnimations(
        val view: AnimatableClockView,
        dozeFraction: Float,
        foldFraction: Float,
    ) : ClockAnimations {
        internal val dozeState = AnimationState(dozeFraction)
        private val foldState = AnimationState(foldFraction)

        init {
            if (foldState.isActive) {
                view.animateFoldAppear(false)
            } else {
                view.animateDoze(dozeState.isActive, false)
            }
        }

        override fun enter() {
            if (!dozeState.isActive) {
                view.animateAppearOnLockscreen()
            }
        }

        override fun charge() = view.animateCharge { dozeState.isActive }

        override fun fold(fraction: Float) {
            val (hasChanged, hasJumped) = foldState.update(fraction)
            if (hasChanged) {
                view.animateFoldAppear(!hasJumped)
            }
        }

        override fun doze(fraction: Float) {
            val (hasChanged, hasJumped) = dozeState.update(fraction)
            if (hasChanged) {
                view.animateDoze(dozeState.isActive, !hasJumped)
            }
        }

        override fun onPickerCarouselSwiping(swipingFraction: Float) {
            view.translationY = 0.5f * view.bottom * (1 - swipingFraction)
        }

        override fun onPositionUpdated(fromLeft: Int, direction: Int, fraction: Float) {}

        override fun onPositionUpdated(distance: Float, fraction: Float) {}

        override fun onFidgetTap(x: Float, y: Float) {}

        override fun onFontAxesChanged(style: ClockAxisStyle) {}
    }

    inner class LargeClockAnimations(
        view: AnimatableClockView,
        dozeFraction: Float,
        foldFraction: Float,
    ) : DefaultClockAnimations(view, dozeFraction, foldFraction) {
        override fun onPositionUpdated(fromLeft: Int, direction: Int, fraction: Float) {
            largeClock.offsetGlyphsForStepClockAnimation(fromLeft, direction, fraction)
        }

        override fun onPositionUpdated(distance: Float, fraction: Float) {
            largeClock.offsetGlyphsForStepClockAnimation(distance, fraction)
        }
    }

    class AnimationState(var fraction: Float) {
        var isActive: Boolean = fraction > 0.5f

        fun update(newFraction: Float): Pair<Boolean, Boolean> {
            if (newFraction == fraction) {
                return Pair(isActive, false)
            }
            val wasActive = isActive
            val hasJumped =
                (fraction == 0f && newFraction == 1f) || (fraction == 1f && newFraction == 0f)
            isActive = newFraction > fraction
            fraction = newFraction
            return Pair(wasActive != isActive, hasJumped)
        }
    }

    override fun dump(pw: PrintWriter) {
        pw.print("smallClock=")
        smallClock.view.dump(pw)

        pw.print("largeClock=")
        largeClock.view.dump(pw)
    }

    private fun getClockName(): String {
        return when(clockId) {
            NEST_CLOCK_ID -> ctx.getString(R.string.clock_nest_clock_name)
            else -> ""
        }
    }

    private fun getClockDescription(): String {
        return when(clockId) {
            NEST_CLOCK_ID -> ctx.getString(R.string.clock_nest_clock_description)
            else -> ""
        }
    }

    companion object {
        @VisibleForTesting const val DOZE_COLOR = Color.WHITE
        private const val FORMAT_NUMBER = 1234567890
    }
}
