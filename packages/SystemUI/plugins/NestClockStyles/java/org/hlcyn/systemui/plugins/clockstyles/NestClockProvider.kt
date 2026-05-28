/*
 * SPDX-FileCopyrightText: 2022 The Android Open Source Project
 * SPDX-FileCopyrightText: 2026 The Halcyon Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hlcyn.systemui.plugins.clockstyles

import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.res.ResourcesCompat
import com.android.systemui.plugins.annotations.Requires
import com.android.systemui.plugins.clocks.ClockController
import com.android.systemui.plugins.clocks.ClockMessageBuffers
import com.android.systemui.plugins.clocks.ClockMetadata
import com.android.systemui.plugins.clocks.ClockPickerConfig
import com.android.systemui.plugins.clocks.ClockProviderPlugin
import com.android.systemui.plugins.clocks.ClockSettings

private val TAG = NestClockProvider::class.simpleName

const val NEST_CLOCK_ID = "NestClock"

val NEST_CLOCKS = listOf(
    NEST_CLOCK_ID,
)

@Requires(target = ClockProviderPlugin::class, version = ClockProviderPlugin.VERSION)
class NestClockProvider : ClockProviderPlugin {

    private var messageBuffers: ClockMessageBuffers? = null

    private lateinit var pluginContext: Context
    private lateinit var sysuiContext: Context

    override fun onCreate(sysuiCtx: Context, pluginCtx: Context) {
        pluginContext = pluginCtx
        sysuiContext = sysuiCtx
    }

    override fun initialize(buffers: ClockMessageBuffers?) {
        messageBuffers = buffers
    }

    override fun getClocks(): List<ClockMetadata> = NEST_CLOCKS.map { ClockMetadata(it) }

    override fun createClock(settings: ClockSettings): ClockController {
        if (!NEST_CLOCKS.contains(settings.clockId)) {
            throw IllegalArgumentException("${settings.clockId} is unsupported by $TAG")
        }

        return NestClockController(
            settings.clockId!!,
            pluginContext,
            sysuiContext,
            LayoutInflater.from(pluginContext),
            pluginContext.resources,
            sysuiContext.resources,
            settings,
            messageBuffers,
        )
    }

    override fun getClockPickerConfig(settings: ClockSettings): ClockPickerConfig {
        if (!NEST_CLOCKS.contains(settings.clockId) || !this::pluginContext.isInitialized) {
            throw IllegalArgumentException("${settings.clockId} is unsupported by $TAG")
        }

        val thumbnail = ResourcesCompat.getDrawable(
            pluginContext.resources,
            R.drawable.clock_default_thumbnail,
            null
        ) ?: throw NullPointerException("Default thumbnail is null")

        return ClockPickerConfig(
            settings.clockId.toString(),
            pluginContext.getString(R.string.clock_nest_clock_name),
            pluginContext.getString(R.string.clock_nest_clock_description),
            thumbnail,
            isReactiveToTone = true,
            axes = emptyList(),
            presetConfig = null,
        )
    }
}
