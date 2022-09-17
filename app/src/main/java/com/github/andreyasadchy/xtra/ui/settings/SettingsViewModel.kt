package com.github.andreyasadchy.xtra.ui.settings

import com.github.andreyasadchy.xtra.db.VideoPositionsDao
import com.github.andreyasadchy.xtra.db.VideosDao
import com.github.andreyasadchy.xtra.ui.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val videoPositions: VideoPositionsDao,
    private val videos: VideosDao) : BaseViewModel() {

    fun deletePositions() {
        GlobalScope.launch {
            videoPositions.deleteAll()
            videos.deletePositions()
        }
    }
}