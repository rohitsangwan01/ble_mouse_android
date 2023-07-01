package com.rohit.blehid

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "BLE HID"

        askForPermission()

        findViewById<View>(R.id.btnMouse).setOnClickListener {
            startActivity(
                Intent(
                    applicationContext,
                    MouseActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.btnPermission).setOnClickListener { askForPermission() }

    }

    private fun askForPermission() {
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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //  Log.e("Test", "onRequestPermissionsResult $requestCode ${permissions.contentToString()} ${grantResults.contentToString()}")
        permissions.forEachIndexed { index, s ->
            Log.e("Test", "onRequestPermissionsResult $s ${grantResults[index]}")
        }
    }

}