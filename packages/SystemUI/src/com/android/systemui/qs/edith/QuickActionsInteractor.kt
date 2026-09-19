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

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.qs.flags.QsSplitInternetTile
import com.android.systemui.qs.pipeline.shared.TileSpec
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the user-editable Quick Actions tile list.
 *
 * When the user has not customized the list yet, [specs] emits [defaultSpecs] (the connectivity
 * tiles the grid shipped with).
 */
@SysUISingleton
class QuickActionsInteractor
@Inject
constructor(
    @Background scope: CoroutineScope,
    private val repository: QuickActionsRepository,
) {
    /** The ordered list of specs shown in the Quick Actions grid. */
    val specs: StateFlow<List<TileSpec>> =
        repository.specs
            .map { stored -> stored.ifEmpty { defaultSpecs } }
            .stateIn(scope, SharingStarted.Eagerly, defaultSpecs)

    /** Sets the Quick Actions list to [specs]. */
    fun setSpecs(specs: List<TileSpec>) {
        repository.writeQuickActionSpecs(specs)
    }

    /** Adds [spec] to the end of the Quick Actions list (no-op if full or already present). */
    fun addSpec(spec: TileSpec) {
        val current = specs.value
        if (spec in current || current.size >= QuickActionsRepository.MAX_SPECS) return
        setSpecs(current + spec)
    }

    /** Removes [spec] from the Quick Actions list. */
    fun removeSpec(spec: TileSpec) {
        setSpecs(specs.value - spec)
    }

    companion object {
        /**
         * Default specs used until the user customizes the list.
         *
         * When the internet tile is split, WiFi and Mobile data are separate tiles. Otherwise they
         * are combined into a single "internet" tile.
         */
        val defaultSpecs: List<TileSpec>
            get() =
                if (QsSplitInternetTile.isEnabled) {
                    listOf(
                        TileSpec.create("wifi"),
                        TileSpec.create("cell"),
                        TileSpec.create("bt"),
                        TileSpec.create("hotspot"),
                    )
                } else {
                    listOf(
                        TileSpec.create("internet"),
                        TileSpec.create("bt"),
                        TileSpec.create("hotspot"),
                        TileSpec.create("airplane"),
                    )
                }
    }
}
