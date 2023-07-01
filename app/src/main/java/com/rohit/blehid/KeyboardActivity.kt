package com.rohit.blehid

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rohit.blehid.ble.KeyboardPeripheral

class KeyboardActivity : AppCompatActivity() {

    private var keyboard: KeyboardPeripheral? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard)

        title = "Keyboard"

        val edtText = findViewById<TextView>(R.id.editText)

        setupBlePeripheralProvider()

        findViewById<Button>(R.id.btnSend).setOnClickListener(View.OnClickListener {
            keyboard?.sendKeys(edtText.text.toString())
        })
    }

    private fun setupBlePeripheralProvider() {
        keyboard = KeyboardPeripheral(this)
        keyboard?.setDeviceName("BLE Keyboard")
        keyboard?.startAdvertising()
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboard?.stopAdvertising()
    }
}