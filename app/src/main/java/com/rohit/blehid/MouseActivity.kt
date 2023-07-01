package com.rohit.blehid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rohit.blehid.ble.MousePeripheral

@SuppressLint("SetTextI18n")
class MouseActivity : AppCompatActivity() {
    private var mouse: MousePeripheral? = null
    private var X = 0f
    private var Y = 0f
    private var firstX = 0f
    private var firstY = 0f
    private var maxPointerCount = 0
    private lateinit var txtUpdate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mouse)
        title = "Mouse"
        txtUpdate = findViewById(R.id.txtUpdate)

        mouse = MousePeripheral(
            this,
            onConnectionStateChanged = { device: BluetoothDevice, status: Int, newState: Int ->
                txtUpdate.text =
                    "${txtUpdate.text}\nConnection state changed: ${device.address} $status -> $newState"
            },
            onAdvertiseStateChange = { isAdvertising: Boolean ->
                txtUpdate.text = "${txtUpdate.text}\nAdvertise state changed: $isAdvertising"
            }

        )

        mouse?.setDeviceName("Ble Mouse")
        mouse?.startAdvertising()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { onTouch(it) }
        return super.onTouchEvent(event)
    }

    private fun onTouch(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Log.e("MouseActivity", "ACTION_DOWN");
                maxPointerCount = motionEvent.pointerCount
                X = motionEvent.x
                Y = motionEvent.y
                firstX = X
                firstY = Y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Log.e("MouseActivity", "ACTION_MOVE");
                maxPointerCount = Math.max(maxPointerCount, motionEvent.pointerCount)
                if (mouse != null) {
                    mouse?.movePointer(
                        (motionEvent.x - X).toInt(),
                        (motionEvent.y - Y).toInt(),
                        0,
                        leftButton = false,
                        rightButton = false,
                        middleButton = false
                    )
                }
                X = motionEvent.x
                Y = motionEvent.y
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                X = motionEvent.x
                Y = motionEvent.y
                if ((X - firstX) * (X - firstX) + (Y - firstY) * (Y - firstY) < 20) {
                    if (mouse != null) {
                        if (maxPointerCount == 1) {
                            mouse?.movePointer(
                                (motionEvent.x - X).toInt(),
                                (motionEvent.y - Y).toInt(),
                                0,
                                leftButton = true,
                                rightButton = false,
                                middleButton = false
                            )
                            mouse?.movePointer(
                                (motionEvent.x - X).toInt(),
                                (motionEvent.y - Y).toInt(),
                                0,
                                leftButton = false,
                                rightButton = false,
                                middleButton = false
                            )
                        } else if (maxPointerCount == 2) {
                            mouse?.movePointer(
                                (motionEvent.x - X).toInt(),
                                (motionEvent.y - Y).toInt(),
                                0,
                                leftButton = false,
                                rightButton = false,
                                middleButton = true
                            )
                            mouse?.movePointer(
                                (motionEvent.x - X).toInt(),
                                (motionEvent.y - Y).toInt(),
                                0,
                                leftButton = false,
                                rightButton = false,
                                middleButton = false
                            )
                        } else if (maxPointerCount > 2) {
                            mouse?.movePointer(
                                (motionEvent.x - X).toInt(),
                                (motionEvent.y - Y).toInt(),
                                0,
                                leftButton = false,
                                rightButton = true,
                                middleButton = false
                            )
                            mouse?.movePointer(
                                (motionEvent.x - X).toInt(),
                                (motionEvent.y - Y).toInt(),
                                0,
                                leftButton = false,
                                rightButton = false,
                                middleButton = false
                            )
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mouse != null) {
            mouse?.stopAdvertising()
        }
    }

}