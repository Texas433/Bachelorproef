/*
 * Copyright (c) 2023, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.kotlin.ble.core.scanner

import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Defines callback type for Bluetooth LE scan.
 *
 * @property value Native Android API value.
 * @see [ScanSettings.Builder](https://developer.android.com/reference/android/bluetooth/le/ScanSettings.Builder#setCallbackType(int))
 */
enum class BleScannerCallbackType(val value: Int) {
    /**
     * Trigger a callback for every Bluetooth advertisement found that matches the filter criteria.
     * If no filter is active, all advertisement packets are reported.
     */
    CALLBACK_TYPE_ALL_MATCHES(ScanSettings.CALLBACK_TYPE_ALL_MATCHES),

    /**
     * A result callback is only triggered for the first advertisement packet received that matches
     * the filter criteria.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    CALLBACK_TYPE_FIRST_MATCH(ScanSettings.CALLBACK_TYPE_FIRST_MATCH),

    /**
     * Receive a callback when advertisements are no longer received from a device that has been
     * previously reported by a first match callback.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    CALLBACK_TYPE_MATCH_LOST(ScanSettings.CALLBACK_TYPE_MATCH_LOST);
}
