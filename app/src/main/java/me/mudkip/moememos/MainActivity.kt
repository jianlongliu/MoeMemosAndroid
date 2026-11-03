package me.mudkip.moememos

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import me.mudkip.moememos.ui.page.common.Navigation
import me.mudkip.moememos.ui.security.AppLockGate
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemosViewModel
import me.mudkip.moememos.viewmodel.UserStateViewModel

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val userStateViewModel: UserStateViewModel by viewModels()
    private val memosViewModel: MemosViewModel by viewModels()

    var pendingIntent: Intent? by mutableStateOf(null)

    companion object {
        const val ACTION_NEW_MEMO = "me.mudkip.moememos.action.NEW_MEMO"
        const val ACTION_EDIT_MEMO = "me.mudkip.moememos.action.EDIT_MEMO"
        const val ACTION_VIEW_MEMO = "me.mudkip.moememos.action.VIEW_MEMO"
        const val EXTRA_MEMO_ID = "memoId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (savedInstanceState == null) {
            pendingIntent = intent
        }
        requestHighestRefreshRate()
        setContent {
            CompositionLocalProvider(
                LocalUserState provides userStateViewModel,
                LocalMemos provides memosViewModel
            ) {
                AppLockGate {
                    Navigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntent = intent
    }

    /**
     * Compose does not request a high refresh rate by itself; without this the system
     * can keep the app on a 60Hz frame-rate override even on a 120Hz display.
     */
    private fun requestHighestRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display ?: return
            val maxRefreshRate = display.supportedModes.maxOfOrNull { it.refreshRate } ?: return
            window.attributes = window.attributes.apply {
                preferredRefreshRate = maxRefreshRate
            }
            if (Build.VERSION.SDK_INT >= 35) {
                window.setFrameRateBoostOnTouchEnabled(true)
            }
        } else {
            @Suppress("DEPRECATION")
            val maxRefreshRate = windowManager.defaultDisplay.supportedModes
                .maxOfOrNull { it.refreshRate } ?: return
            window.attributes = window.attributes.apply {
                preferredRefreshRate = maxRefreshRate
            }
        }
    }
}
