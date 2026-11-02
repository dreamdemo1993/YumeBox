/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumeyucca.yumebox.runtime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Target of the ongoing notification's stop action. It is declared in the manifest so a tap still
 * lands after the app process was killed, and it asks [RuntimeCoordinator] to stop the session
 * exactly like the QS tile does — the owning foreground service then drops its own notification.
 */
class ProxyStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP) return
        // The stop itself is suspended work; goAsync() keeps the receiver (and its process) alive
        // while the coordinator tears the session down.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AutoStartSessionGate.markManualPaused()
                RuntimeCoordinator.stop()
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                // fault barrier: a receiver crash would surface as an ANR inside SystemUI
                Timber.tag(TAG).e(error, "Notification stop action failed")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ProxyStopReceiver"

        const val ACTION_STOP = "com.github.yumeyucca.yumebox.intent.action.STOP_PROXY"
    }
}
