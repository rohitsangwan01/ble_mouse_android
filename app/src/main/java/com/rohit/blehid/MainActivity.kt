package com.rohit.blehid

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.rohit.blehid.ble.BleUtils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "BLE HID"

        initialize()


        findViewById<View>(R.id.btnMouse).setOnClickListener {
            startActivity(
                Intent(
                    applicationContext,
                    MouseActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnKeyboard).setOnClickListener {
            startActivity(
                Intent(
                    applicationContext,
                    KeyboardActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnPermission).setOnClickListener {
            val result = initialize()
            if (result) {
                Toast.makeText(this, "Al Set", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun initialize(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                ),
                1
            )
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
                ),
                1
            )
        }
        if (!BleUtils.isBluetoothEnabled(this)) {
            BleUtils.enableBluetooth(this)
            return false
        }
        if (!BleUtils.isBleSupported(this) || !BleUtils.isBlePeripheralSupported(this)) {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle("Not Supported")
            alertDialog.setMessage("not supported")
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, "ok"
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.setOnDismissListener { finish() }
            alertDialog.show()
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissions.forEachIndexed { index, s ->
            Log.e("Test", "onRequestPermissionsResult $s ${grantResults[index]}")
        }
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
            }
        }
    }

}