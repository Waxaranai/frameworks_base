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
import com.android.systemui.qs.flags.QsSplitInternetTile
import com.android.systemui.qs.panels.shared.model.SizedTileImpl
import com.android.systemui.qs.pipeline.domain.interactor.CurrentTilesInteractor
import com.android.systemui.qs.pipeline.shared.TileSpec
import javax.inject.Inject

/**
 * View model backing the fixed "Quick Actions" grid.
 *
 * The grid shows up to [TILE_COUNT] (4) fixed 1x1 tiles in a 2x2 layout. It first picks from
 * [preferredSpecs] (WiFi, Bluetooth, Mobile data, Hotspot) and backfills any missing or unavailable
 * ones with [FALLBACK_SPECS], so the grid is always full even on devices without cellular, Wi-Fi, or
 * Bluetooth.
 *
 * Resolution is done once, at startup ([CurrentTilesInteractor.createTileSync]); the tiles are not
 * resizable and are not necessarily part of the user's selectable QS tile list.
 */
@SysUISingleton
class QuickActionsGridViewModel
@Inject
constructor(
    private val tilesInteractor: CurrentTilesInteractor,
    val squishinessViewModel: TileSquishinessViewModel,
    val tileHapticsViewModelFactory: TileHapticsViewModel.Factory,
) {

    // Resolved lazily, once, and kept for the lifetime of the (singleton) view model. Recreating
    // tiles on every recomposition would leak listeners and create duplicate tiles.
    private val tiles: List<Pair<TileSpec, QSTile>> by lazy { resolveTiles() }

    /** Tiles, in display order, for the 2x2 grid. At most [TILE_COUNT] entries. */
    val tileViewModels: List<TileViewModel> by derivedStateOf {
        tiles.map { (spec, tile) -> TileViewModel(tile, spec, Expandable()) }
    }

    /** The tiles as [SizedTile]s, all with a fixed width of 1 (non-resizable). */
    val sizedTiles: List<SizedTileImpl<TileViewModel>> by derivedStateOf {
        tileViewModels.map { SizedTileImpl(it, width = 1) }
    }

    private fun resolveTiles(): List<Pair<TileSpec, QSTile>> {
        val resolved = mutableListOf<Pair<TileSpec, QSTile>>()
        // Preferred tiles first (in order), then backfill with fallbacks to fill the grid.
        for (spec in preferredSpecs + FALLBACK_SPECS) {
            if (resolved.size == TILE_COUNT) break
            if (resolved.any { it.first == spec }) continue
            val tile = createAvailableTile(spec) ?: continue
            resolved += spec to tile
        }
        return resolved
    }

    private fun createAvailableTile(spec: TileSpec): QSTile? {
        val tile = tilesInteractor.createTileSync(spec) ?: return null
        if (!tile.isAvailable) {
            tile.destroy()
            return null
        }
        return tile
    }

    companion object {
        /** Number of columns of the fixed Quick Actions grid. */
        const val COLUMNS = 2

        /** Total number of tiles in the grid (2x2). */
        const val TILE_COUNT = COLUMNS * 2

        /**
         * Preferred specs shown in the Quick Actions grid, in order (row-major).
         *
         * When the internet tile is split, WiFi and Mobile data are separate tiles. Otherwise they
         * are combined into a single "internet" tile, which fills the first slot.
         */
        val preferredSpecs: List<TileSpec>
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
                    )
                }

        /**
         * Specs used to backfill the grid when a preferred tile is missing or unavailable (for
         * example on devices without cellular). Chosen to be broadly available on most devices.
         */
        val FALLBACK_SPECS: List<TileSpec> =
            listOf(
                TileSpec.create("airplane"),
                TileSpec.create("dnd"),
                TileSpec.create("rotation"),
                TileSpec.create("flashlight"),
                TileSpec.create("battery"),
            )
    }
}
