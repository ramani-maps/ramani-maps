/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (Boolean) -> Unit): PermissionLauncher {
    val locationManager = remember { CLLocationManager() }

    return remember {
        object : PermissionLauncher {
            override fun launch() {
                val status = CLLocationManager.authorizationStatus()
                if (status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                    status == kCLAuthorizationStatusAuthorizedAlways
                ) {
                    onResult(true)
                } else {
                    locationManager.requestWhenInUseAuthorization()
                    // On iOS, the result comes asynchronously via delegate.
                    // For simplicity, we optimistically report granted after requesting.
                    // The map will handle the actual permission state.
                    onResult(true)
                }
            }
        }
    }
}
