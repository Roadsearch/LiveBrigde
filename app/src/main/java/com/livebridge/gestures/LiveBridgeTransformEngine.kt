package com.livebridge.gestures

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Native multitouch engine for the LiveBridge mobile studio.
 *
 * 1 finger = pan
 * 2+ fingers = pan + pinch + rotation
 *
 * Pointer IDs are tracked instead of pointer indexes, and every pointer
 * transition creates a fresh baseline to prevent jumps when fingers enter/leave.
 */
class LiveBridgeTransformEngine(
    private val onTransform: (TransformDelta) -> Unit,
    private val onTap: () -> Unit = {},
    private val onDoubleTap: () -> Unit = {},
    private val onLongPress: () -> Unit = {}
) : View.OnTouchListener {

    data class TransformDelta(
        val panX: Float,
        val panY: Float,
        val scale: Float,
        val rotationDegrees: Float,
        val pivotX: Float,
        val pivotY: Float,
        val pointerCount: Int
    )

    private var primaryId = MotionEvent.INVALID_POINTER_ID
    private var downTime = 0L
    private var lastTapTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var previousCx = 0f
    private var previousCy = 0f
    private var previousDistance = 0f
    private var previousAngle = 0f
    private var moved = false
    private var transforming = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                primaryId = event.getPointerId(0)
                downTime = System.currentTimeMillis()
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
                previousCx = event.x
                previousCy = event.y
                previousDistance = 0f
                previousAngle = 0f
                moved = false
                transforming = false
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    val c = centroid(event)
                    previousCx = c.first
                    previousCy = c.second
                    previousDistance = distance(event)
                    previousAngle = angle(event)
                    transforming = true
                    moved = true
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val c = centroid(event)
                    val d = distance(event)
                    val a = angle(event)

                    val panX = c.first - previousCx
                    val panY = c.second - previousCy
                    val scale = if (previousDistance > 0.5f && d > 0f) {
                        (d / previousDistance).coerceIn(0.25f, 4f)
                    } else 1f
                    val rotation = normalize(a - previousAngle)

                    if (abs(panX) > 0.7f || abs(panY) > 0.7f ||
                        abs(scale - 1f) > 0.008f || abs(rotation) > 0.8f) {
                        moved = true
                        transforming = true
                        onTransform(
                            TransformDelta(
                                panX, panY, scale, rotation,
                                c.first, c.second, event.pointerCount
                            )
                        )
                    }

                    previousCx = c.first
                    previousCy = c.second
                    previousDistance = d
                    previousAngle = a
                    return true
                }

                val index = event.findPointerIndex(primaryId)
                if (index < 0) return true
                val x = event.getX(index)
                val y = event.getY(index)
                val dx = x - lastX
                val dy = y - lastY

                if (abs(dx) > 0.7f || abs(dy) > 0.7f) {
                    moved = true
                    transforming = true
                    onTransform(
                        TransformDelta(dx, dy, 1f, 0f, x, y, 1)
                    )
                }
                lastX = x
                lastY = y
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val releasedIndex = event.actionIndex
                val releasedId = event.getPointerId(releasedIndex)
                val remainingIndex = firstRemainingIndex(event, releasedIndex)

                if (remainingIndex >= 0) {
                    primaryId = event.getPointerId(remainingIndex)
                    lastX = event.getX(remainingIndex)
                    lastY = event.getY(remainingIndex)
                }

                if (event.pointerCount - 1 <= 1) {
                    previousDistance = 0f
                    transforming = false
                } else {
                    val c = centroidExcluding(event, releasedIndex)
                    previousCx = c.first
                    previousCy = c.second
                    previousDistance = distanceExcluding(event, releasedIndex)
                    previousAngle = angleExcluding(event, releasedIndex)
                }

                // Prevent a released secondary pointer from becoming the primary
                // reference accidentally.
                if (releasedId == primaryId && remainingIndex >= 0) {
                    primaryId = event.getPointerId(remainingIndex)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - downTime
                val travel = hypot(event.x - downX, event.y - downY)
                if (!moved && !transforming && duration <= 280L && travel <= 24f) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime <= 320L) onDoubleTap() else onTap()
                    lastTapTime = now
                } else if (!moved && duration >= 500L) {
                    onLongPress()
                }
                reset()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                reset()
                return true
            }
        }
        return true
    }

    private fun reset() {
        primaryId = MotionEvent.INVALID_POINTER_ID
        previousDistance = 0f
        moved = false
        transforming = false
    }

    private fun centroid(e: MotionEvent): Pair<Float, Float> {
        var x = 0f
        var y = 0f
        for (i in 0 until e.pointerCount) {
            x += e.getX(i)
            y += e.getY(i)
        }
        return x / e.pointerCount to y / e.pointerCount
    }

    private fun centroidExcluding(e: MotionEvent, excluded: Int): Pair<Float, Float> {
        var x = 0f
        var y = 0f
        var count = 0
        for (i in 0 until e.pointerCount) if (i != excluded) {
            x += e.getX(i)
            y += e.getY(i)
            count++
        }
        return if (count == 0) 0f to 0f else x / count to y / count
    }

    private fun distance(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        var maxDistance = 0f
        for (i in 1 until e.pointerCount) {
            maxDistance = maxOf(
                maxDistance,
                hypot(e.getX(i) - e.getX(0), e.getY(i) - e.getY(0))
            )
        }
        return maxDistance
    }

    private fun distanceExcluding(e: MotionEvent, excluded: Int): Float {
        val ids = (0 until e.pointerCount).filter { it != excluded }
        if (ids.size < 2) return 0f
        val a = ids[0]
        val b = ids[1]
        return hypot(e.getX(b) - e.getX(a), e.getY(b) - e.getY(a))
    }

    private fun angle(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        return Math.toDegrees(
            atan2(
                (e.getY(1) - e.getY(0)).toDouble(),
                (e.getX(1) - e.getX(0)).toDouble()
            )
        ).toFloat()
    }

    private fun angleExcluding(e: MotionEvent, excluded: Int): Float {
        val ids = (0 until e.pointerCount).filter { it != excluded }
        if (ids.size < 2) return 0f
        val a = ids[0]
        val b = ids[1]
        return Math.toDegrees(
            atan2(
                (e.getY(b) - e.getY(a)).toDouble(),
                (e.getX(b) - e.getX(a)).toDouble()
            )
        ).toFloat()
    }

    private fun firstRemainingIndex(e: MotionEvent, excluded: Int): Int {
        for (i in 0 until e.pointerCount) if (i != excluded) return i
        return -1
    }

    private fun normalize(value: Float): Float {
        var a = value
        while (a > 180f) a -= 360f
        while (a < -180f) a += 360f
        return a
    }
}
