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

/**
 * Per-slot color overrides for the Edith QS tertiary tile scheme, set from the (debug) color tuner
 * in Settings.
 *
 * Each slot holds a swatch *tag* (see [EdithTileSwatches]) rather than a resolved color, so the
 * tile can re-resolve the actual color from the current theme on every draw. This keeps the
 * override in sync with the dynamic palette (e.g. after a wallpaper / ThemePicker change).
 *
 * Every field is `null` when the corresponding slot is not overridden, in which case the tile falls
 * back to the default tertiary role for that slot. A `null` [EdithTileColorOverride] (i.e. no
 * override at all) means "use the defaults entirely".
 */
data class EdithTileColorOverride(
    val activeBg: String? = null,
    val activeFg: String? = null,
    val inactiveBg: String? = null,
    val inactiveFg: String? = null,
    /** Alpha for the inactive background, in [0, 1]. */
    val inactiveAlpha: Float? = null,
) {
    /** Whether at least one slot is overridden. */
    val hasAny: Boolean
        get() = activeBg != null ||
            activeFg != null ||
            inactiveBg != null ||
            inactiveFg != null ||
            inactiveAlpha != null
}
