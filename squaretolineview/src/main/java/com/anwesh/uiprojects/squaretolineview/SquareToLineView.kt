package com.anwesh.uiprojects.squaretolineview

/**
 * Created by anweshmishra on 09/08/18.
 */

import android.app.Activity
import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path

val nodes : Int = 5

fun Canvas.drawSTLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / nodes
    paint.style = Paint.Style.STROKE
    paint.color = Color.parseColor("#4CAF50")
    paint.strokeWidth = Math.min(w, h) / 60
    paint.strokeCap = Paint.Cap.ROUND
    val index : Int = i % 2
    val y : Float = -gap * (1 - scale)
    val factor : Int = 1 - 2 * index
    save()
    translate(i * gap, h/2)
    val path : Path = Path()
    path.moveTo(0f, 0f)
    path.lineTo(0f, y)
    path.lineTo(gap, y)
    path.lineTo(gap, 0f)
    drawPath(path, paint)
    restore()
}

class SquareToLineView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += 0.1f * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1 - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class STLNode(var i : Int, val state : State = State()) {

        private var next : STLNode? = null

        private var prev : STLNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = STLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSTLNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : STLNode {
            var curr : STLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LinkedSTL(var i : Int) {

        private var root : STLNode = STLNode(0)
        private var curr : STLNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, fl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, fl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : SquareToLineView) {

        private var lstl : LinkedSTL = LinkedSTL(0)

        private var animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#212121"))
            lstl.draw(canvas, paint)
            animator.animate {
                lstl.update {i, fl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            lstl.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : SquareToLineView {
            val view : SquareToLineView = SquareToLineView(activity)
            activity.setContentView(view)
            return view
        }
    }
}