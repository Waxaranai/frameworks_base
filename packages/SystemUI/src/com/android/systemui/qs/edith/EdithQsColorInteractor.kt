/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.edith

import android.os.SystemProperties
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.qs.edith.EdithQsColorRepository.Companion.UNSET
import com.android.systemui.qs.edith.EdithQsColorRepository.Companion.UNSET_INT
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Exposes whether the Edith Quick Settings color scheme is enabled as a [StateFlow]. */
@SysUISingleton
class EdithQsColorInteractor
@Inject
constructor(
    @Background scope: CoroutineScope,
    repository: EdithQsColorRepository,
) {
    val isEnabled: StateFlow<Boolean> =
        repository.isEnabled.stateIn(scope, SharingStarted.Eagerly, true)

    /**
     * Live per-slot tile color overrides from the (debug) color tuner, or `null` when the tuner is
     * disabled (the [TUNER_PROP] system property is off) or nothing is overridden.
     */
    val tileColorOverride: StateFlow<EdithTileColorOverride?> =
        combine(
                repository.tileColorActiveBg,
                repository.tileColorActiveFg,
                repository.tileColorInactiveBg,
                repository.tileColorInactiveFg,
                repository.tileColorInactiveAlpha,
            ) { activeBg, activeFg, inactiveBg, inactiveFg, inactiveAlpha ->
                if (!isTunerEnabled()) return@combine null
                val override =
                    EdithTileColorOverride(
                        activeBg = activeBg.takeIf { it != UNSET },
                        activeFg = activeFg.takeIf { it != UNSET },
                        inactiveBg = inactiveBg.takeIf { it != UNSET },
                        inactiveFg = inactiveFg.takeIf { it != UNSET },
                        inactiveAlpha =
                            inactiveAlpha.takeIf { it != UNSET_INT }?.coerceIn(0, 100)?.let {
                                it / 100f
                            },
                    )
                override.takeIf { it.hasAny }
            }
            .stateIn(scope, SharingStarted.Eagerly, null)

    companion object {
        /**
         * System property gating the Edith tile color tuner. When unset/false, the tuner screen is
         * hidden and tile color overrides are ignored.
         */
        const val TUNER_PROP = "debug.enable_edith_tile_color"

        /** Whether the (debug) Edith tile color tuner is enabled. */
        fun isTunerEnabled(): Boolean = SystemProperties.getBoolean(TUNER_PROP, false)
    }
}
