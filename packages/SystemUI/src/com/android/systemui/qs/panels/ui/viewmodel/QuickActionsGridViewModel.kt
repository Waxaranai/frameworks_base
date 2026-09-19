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

package com.android.systemui.qs.panels.ui.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import com.android.systemui.animation.Expandable
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.haptics.msdl.qs.TileHapticsViewModel
import com.android.systemui.plugins.qs.QSTile
import com.android.systemui.qs.edith.QuickActionsInteractor
import com.android.systemui.qs.panels.shared.model.SizedTileImpl
import com.android.systemui.qs.pipeline.domain.interactor.CurrentTilesInteractor
import com.android.systemui.qs.pipeline.shared.TileSpec
import javax.inject.Inject

/**
 * View model backing the "Quick Actions" grid.
 *
 * The grid shows up to [TILE_COUNT] (4) fixed 1x1 tiles in a 2x2 layout, in the order given by
 * [QuickActionsInteractor] (the user-editable list, defaulting to the connectivity tiles). Tiles
 * that are unavailable on the current device are dropped.
 *
 * Tiles are resolved through [CurrentTilesInteractor.createTileSync]; they are not resizable and are
 * not necessarily part of the user's selectable QS tile list.
 */
@SysUISingleton
class QuickActionsGridViewModel
@Inject
constructor(
    private val tilesInteractor: CurrentTilesInteractor,
    private val quickActionsInteractor: QuickActionsInteractor,
    val squishinessViewModel: TileSquishinessViewModel,
    val tileHapticsViewModelFactory: TileHapticsViewModel.Factory,
) {

    // Cache of created tiles, keyed by spec. Tiles are expensive and listener-bound, so they are
    // created at most once per spec and reused across list edits.
    private val tileCache = mutableMapOf<TileSpec, QSTile?>()

    /** Tiles, in display order, for the 2x2 grid. At most [TILE_COUNT] entries. */
    val tileViewModels: List<TileViewModel> by derivedStateOf {
        val resolved = mutableListOf<TileViewModel>()
        for (spec in quickActionsInteractor.specs.value) {
            if (resolved.size == TILE_COUNT) break
            if (resolved.any { it.spec == spec }) continue
            val tile = getOrCreateTile(spec) ?: continue
            resolved += TileViewModel(tile, spec, Expandable())
        }
        resolved
    }

    /** The tiles as [SizedTile]s, all with a fixed width of 1 (non-resizable). */
    val sizedTiles: List<SizedTileImpl<TileViewModel>> by derivedStateOf {
        tileViewModels.map { SizedTileImpl(it, width = 1) }
    }

    private fun getOrCreateTile(spec: TileSpec): QSTile? {
        if (tileCache.containsKey(spec)) {
            return tileCache[spec]
        }
        val tile = tilesInteractor.createTileSync(spec)
        val available = tile != null && tile.isAvailable
        if (!available) {
            tile?.destroy()
        }
        val result = if (available) tile else null
        tileCache[spec] = result
        return result
    }

    companion object {
        /** Number of columns of the Quick Actions grid. */
        const val COLUMNS = 2

        /** Total number of tiles in the grid (2x2). */
        const val TILE_COUNT = COLUMNS * 2
    }
}
