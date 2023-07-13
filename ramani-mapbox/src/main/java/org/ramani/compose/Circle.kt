/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2023 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions

@Composable
@MapboxComposable
fun Circle(
    center: LatLng,
    radius: Float,
    isDraggable: Boolean = false,
    color: String = "Yellow",
    opacity: Float = 1.0f,
    borderColor: String = "Black",
    borderWidth: Float = 0.0F,
    zIndex: Int = 0,
    onCenterDragged: (LatLng) -> Unit = {},
    onDragFinished: (LatLng) -> Unit = {},
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<CircleNode, MapApplier>(factory = {
        val circleManager = mapApplier.getOrCreateCircleManagerForZIndex(zIndex)

        val circleOptions = CircleAnnotationOptions()
            .withCircleRadius(radius.toDouble())
            .withPoint(Point.fromLngLat(center.longitude, center.latitude))
            .withDraggable(isDraggable)
            .withCircleStrokeColor(borderColor)
            .withCircleStrokeWidth(borderWidth.toDouble())
            .withCircleOpacity(opacity.toDouble())
            .withCircleSortKey(zIndex.toDouble())

        val circle = circleManager.create(circleOptions)

        CircleNode(
            circleManager = circleManager,
            circleManagerId = zIndex,
            circle = circle,
            onCircleDragged = {
                onCenterDragged(LatLng(it.point.latitude(), it.point.longitude()))
            },
            onCircleDragStopped = {
                onDragFinished(LatLng(it.point.latitude(), it.point.longitude()))
            },
        )
    }, update = {
        update(onCenterDragged) {
            this.onCircleDragged = {
                onCenterDragged(LatLng(it.point.latitude(), it.point.longitude()))
            }
        }

        update(onDragFinished) {
            this.onCircleDragStopped = {
                onDragFinished(LatLng(it.point.latitude(), it.point.longitude()))
            }
        }

        set(center) {
            circle.point = Point.fromLngLat(center.longitude, center.latitude)
            circleManager.update(circle)
        }

        set(color) {
            circle.circleColorString = color
            circleManager.update(circle)
        }

        set(radius) {
            circle.circleRadius = radius.toDouble()
            circleManager.update(circle)
        }
    })
}
