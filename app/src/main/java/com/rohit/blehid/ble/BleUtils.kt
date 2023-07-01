package com.rohit.blehid.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.UUID


@SuppressLint("MissingPermission")
object BleUtils {
    const val REQUEST_CODE_BLUETOOTH_ENABLE = 0xb1e
    private const val UUID_LONG_STYLE_PREFIX = "0000"
    private const val UUID_LONG_STYLE_POSTFIX = "-0000-1000-8000-00805F9B34FB"

    // Bluetooth methods
    fun isBleSupported(context: Context): Boolean {
        try {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                return false
            }
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
            if (bluetoothAdapter != null) {
                return true
            }
        } catch (ignored: Throwable) {
            // ignore exception
        }
        return false
    }

    fun isBlePeripheralSupported(context: Context): Boolean {
        val bluetoothAdapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                ?: return false
        return bluetoothAdapter.isMultipleAdvertisementSupported
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                ?: return false
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        return bluetoothAdapter?.isEnabled ?: false
    }

    fun enableBluetooth(activity: Activity) {
        activity.startActivityForResult(
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
            REQUEST_CODE_BLUETOOTH_ENABLE
        )
    }


    // UUID methods
    fun fromShortValue(uuidShortValue: Int): UUID {
        return UUID.fromString(
            UUID_LONG_STYLE_PREFIX + String.format(
                "%04X",
                uuidShortValue and 0xffff
            ) + UUID_LONG_STYLE_POSTFIX
        )
    }

    fun matches(src: UUID, dst: UUID): Boolean {
        return if (isShortUuid(src) || isShortUuid(dst)) {
            // at least one instance is short style: check only 16bits
            val srcShortUUID = src.mostSignificantBits and 0x0000ffff00000000L
            val dstShortUUID = dst.mostSignificantBits and 0x0000ffff00000000L
            srcShortUUID == dstShortUUID
        } else {
            src == dst
        }
    }

    private fun isShortUuid(src: UUID): Boolean {
        return src.mostSignificantBits and -0xffff00000001L == 0L && src.leastSignificantBits == 0L
    }
}
