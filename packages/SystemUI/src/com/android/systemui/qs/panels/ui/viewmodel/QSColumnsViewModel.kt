/*
 * Copyright (C) 2024 The Android Open Source Project
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
import com.android.systemui.lifecycle.HydratedActivatable
import com.android.systemui.media.controls.ui.controller.MediaLocation
import com.android.systemui.media.remedia.ui.compose.MediaUiBehavior
import com.android.systemui.qs.panels.domain.interactor.QSColumnsInteractor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * View model for the number of columns that should be shown in a QS grid.
 *
 * The number of columns is resource-driven (see [QSColumnsInteractor]). The Edith QS style keeps
 * the configured column count but forces every tile to 1x1 (see [largeSpan]).
 */
class QSColumnsViewModel
@AssistedInject
constructor(
    interactor: QSColumnsInteractor,
    mediaInRowInLandscapeViewModelFactory: MediaInRowInLandscapeViewModel.Factory,
    @Assisted @MediaLocation mediaLocation: Int?,
    @Assisted mediaUiBehavior: MediaUiBehavior?,
) : HydratedActivatable() {

    val columns by derivedStateOf {
        if (mediaInRowInLandscapeViewModel?.shouldMediaShowInRow == true) {
            columnsWithoutMedia / 2
        } else {
            columnsWithoutMedia
        }
    }

    // Edith QS style: no large (span > 1) tiles, every tile is 1x1.
    val largeSpan: Int
        get() = 1

    private val mediaInRowInLandscapeViewModel =
        if (mediaLocation != null && mediaUiBehavior != null) {
            mediaInRowInLandscapeViewModelFactory.create(mediaLocation, mediaUiBehavior)
        } else {
            null
        }

    private val columnsWithoutMedia by interactor.columns.hydratedStateOf()

    override suspend fun onActivated() {
        coroutineScope { launch { mediaInRowInLandscapeViewModel?.activate() } }
    }

    @AssistedFactory
    interface Factory {
        fun create(mediaLocation: Int?, mediaUiBehavior: MediaUiBehavior?): QSColumnsViewModel

        fun createWithoutMediaTracking() = create(null, null)
    }
}
