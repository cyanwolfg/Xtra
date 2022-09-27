package com.github.andreyasadchy.xtra.ui.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.andreyasadchy.xtra.ui.main.MainViewModel
import com.github.andreyasadchy.xtra.util.isNetworkAvailable

abstract class BaseNetworkFragment : Fragment() {

    private companion object {
        const val LAST_KEY = "last"
        const val RESTORE_KEY = "restore"
        const val CREATED_KEY = "created"
    }

    private val mainViewModel: MainViewModel by activityViewModels()

    protected var enableNetworkCheck = true
    private var lastIsOnlineState = false
    private var shouldRestore = false
    private var isInitialized = false
    private var created = false

    abstract fun initialize()
    abstract fun onNetworkRestored()
    open fun onNetworkLost() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (enableNetworkCheck) {
            lastIsOnlineState = savedInstanceState?.getBoolean(LAST_KEY) ?: requireContext().isNetworkAvailable
            shouldRestore = savedInstanceState?.getBoolean(RESTORE_KEY) ?: false
            created = savedInstanceState?.getBoolean(CREATED_KEY) ?: false
        }
    }

    override fun onResume() {
        super.onResume()
        if (enableNetworkCheck) {
            if (!isInitialized) {
                if (created || lastIsOnlineState) {
                    init()
                }
                mainViewModel.isNetworkAvailable.observe(viewLifecycleOwner) {
                    val isOnline = it.peekContent()
                    if (isOnline) {
                        if (!lastIsOnlineState) {
                            shouldRestore = if (isResumed) {
                                if (isInitialized) {
                                    onNetworkRestored()
                                } else {
                                    init()
                                }
                                false
                            } else {
                                true
                            }
                        }
                    } else {
                        if (isInitialized) {
                            onNetworkLost()
                        }
                    }
                    lastIsOnlineState = isOnline
                }
            } else if (shouldRestore && lastIsOnlineState) {
                onNetworkRestored()
                shouldRestore = false
            }
        } else {
            if (!isInitialized) {
                initialize()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (enableNetworkCheck) {
            isInitialized = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (enableNetworkCheck) {
            outState.putBoolean(LAST_KEY, lastIsOnlineState)
            outState.putBoolean(RESTORE_KEY, shouldRestore)
            outState.putBoolean(CREATED_KEY, created)
        }
    }

    private fun init() {
        initialize()
        isInitialized = true
        created = true
    }
}