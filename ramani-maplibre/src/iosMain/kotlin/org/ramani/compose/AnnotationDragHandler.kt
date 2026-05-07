/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import MapLibre.MLNMapView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UILongPressGestureRecognizer
import platform.darwin.NSObject

/**
 * Handles dragging of map annotations (Circle, Symbol) on iOS.
 *
 * MLNMapView has no built-in drag-on-feature support. This handler uses a
 * [UILongPressGestureRecognizer] with zero press duration to detect touches
 * on draggable features.
 *
 * The map's own gesture recognizers are configured to require our recognizer
 * to fail before they begin (via [requireGestureRecognizerToFail]). When the
 * touch is on a draggable feature, our recognizer succeeds and the map's
 * gestures are suppressed for that touch. When the touch is not on a feature,
 * [gestureRecognizerShouldBegin] returns false, our recognizer fails
 * immediately, and the map's gestures proceed with no delay.
 */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal class AnnotationDragHandler(
    private val mapView: MLNMapView,
    private val mapApplier: MapApplier,
) : NSObject(), UIGestureRecognizerDelegateProtocol {

    private var draggedNode: DraggableNode? = null
    private var touchOffsetX: Double = 0.0
    private var touchOffsetY: Double = 0.0

    private val longPressRecognizer = UILongPressGestureRecognizer(
        target = this,
        action = platform.objc.sel_registerName("handleLongPress:"),
    ).apply {
        minimumPressDuration = 0.0
    }

    init {
        longPressRecognizer.delegate = this
        mapView.addGestureRecognizer(longPressRecognizer)

        // Make the map's gesture recognizers wait for ours to fail before
        // they start. If we detect a draggable feature, the map's gestures
        // are suppressed. If not, ours fails immediately and the map
        // proceeds with no delay.
        @Suppress("UNCHECKED_CAST")
        (mapView.gestureRecognizers as? List<UIGestureRecognizer>)?.forEach { recognizer ->
            if (recognizer != longPressRecognizer) {
                recognizer.requireGestureRecognizerToFail(longPressRecognizer)
            }
        }
    }

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean {
        if (gestureRecognizer != longPressRecognizer) return true

        val touchPoint = gestureRecognizer.locationInView(mapView)
        val draggableIds = mapApplier.draggableLayerIds()
        if (draggableIds.isEmpty()) return false

        for (layerId in draggableIds) {
            val features = mapView.visibleFeaturesAtPoint(
                touchPoint,
                inStyleLayersWithIdentifiers = setOf(layerId),
            )
            if (features.isNotEmpty()) {
                val node = mapApplier.findDraggableNodeForLayerId(layerId)
                if (node != null) return true
            }
        }
        return false
    }

    @kotlinx.cinterop.ObjCAction
    fun handleLongPress(recognizer: UILongPressGestureRecognizer) {
        when (recognizer.state) {
            UIGestureRecognizerStateBegan -> onGestureBegan(recognizer)
            UIGestureRecognizerStateChanged -> onGestureChanged(recognizer)
            UIGestureRecognizerStateEnded -> onGestureEnded(recognizer)
            UIGestureRecognizerStateCancelled, UIGestureRecognizerStateFailed -> cleanup()
            else -> {}
        }
    }

    private fun onGestureBegan(recognizer: UILongPressGestureRecognizer) {
        val touchPoint = recognizer.locationInView(mapView)
        val draggableIds = mapApplier.draggableLayerIds()

        for (layerId in draggableIds) {
            val features = mapView.visibleFeaturesAtPoint(
                touchPoint,
                inStyleLayersWithIdentifiers = setOf(layerId),
            )
            if (features.isNotEmpty()) {
                val node = mapApplier.findDraggableNodeForLayerId(layerId)
                if (node != null) {
                    draggedNode = node

                    // Calculate offset between touch point and annotation center
                    // to prevent the annotation from snapping to the finger
                    val centerState = when (node) {
                        is CircleNode -> node.centerState
                        is SymbolNode -> node.centerState
                        else -> null
                    }
                    centerState?.let { cs ->
                        val annotationScreenPoint = mapView.convertCoordinate(
                            cs.center.toCLLocationCoordinate2D(),
                            toPointToView = mapView,
                        )
                        touchPoint.useContents {
                            val tp = this
                            annotationScreenPoint.useContents {
                                touchOffsetX = tp.x - this.x
                                touchOffsetY = tp.y - this.y
                            }
                        }
                    }
                    return
                }
            }
        }
    }

    private fun onGestureChanged(recognizer: UILongPressGestureRecognizer) {
        val node = draggedNode ?: return

        val touchPoint = recognizer.locationInView(mapView)
        val adjustedPoint = touchPoint.useContents {
            CGPointMake(x - touchOffsetX, y - touchOffsetY)
        }
        val coordinate = mapView.convertPoint(adjustedPoint, toCoordinateFromView = mapView)
        node.handleDrag(coordinate.toCommon())
    }

    private fun onGestureEnded(recognizer: UILongPressGestureRecognizer) {
        draggedNode?.let { node ->
            val touchPoint = recognizer.locationInView(mapView)
            val adjustedPoint = touchPoint.useContents {
                CGPointMake(x - touchOffsetX, y - touchOffsetY)
            }
            val coordinate = mapView.convertPoint(adjustedPoint, toCoordinateFromView = mapView)
            node.handleDragFinished(coordinate.toCommon())
        }
        cleanup()
    }

    private fun cleanup() {
        draggedNode = null
        touchOffsetX = 0.0
        touchOffsetY = 0.0
    }
}
