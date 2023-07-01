package com.rohit.blehid

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.rohit.blehid.ble.BleUtils
import com.rohit.blehid.ble.MousePeripheral


class MouseActivity : AppCompatActivity() {
    private var mouse: MousePeripheral? = null
    private var X = 0f
    private var Y = 0f
    private var firstX = 0f
    private var firstY = 0f
    private var maxPointerCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mouse)
        title = "Mouse"

        setupBlePeripheralProvider()
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

    private fun setupBlePeripheralProvider() {
        mouse = MousePeripheral(this)
        mouse?.setDeviceName("Ble Mouse")
        mouse?.startAdvertising()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mouse != null) {
            mouse?.stopAdvertising()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BleUtils.REQUEST_CODE_BLUETOOTH_ENABLE) {
            if (!BleUtils.isBluetoothEnabled(this)) {
                Toast.makeText(this, "requires_bl_enabled", Toast.LENGTH_LONG).show()
                return
            }
            if (!BleUtils.isBleSupported(this) || !BleUtils.isBlePeripheralSupported(this)) {
                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setTitle("not_supported")
                alertDialog.setMessage("Ble not supported")
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, "ok"
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.setOnDismissListener { finish() }
                alertDialog.show()
            } else {
                setupBlePeripheralProvider()
            }
        }
    }
}