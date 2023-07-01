package com.rohit.blehid.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.Collections
import java.util.Queue
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue

private val TAG = HidPeripheral::class.java.simpleName

@SuppressLint("MissingPermission")
abstract class HidPeripheral protected constructor(
    context: Context,
    needInputReport: Boolean,
    needOutputReport: Boolean,
    needFeatureReport: Boolean,
    dataSendingRate: Int,
    onAdvertiseStateChange: ((isAdvertising: Boolean) -> Unit)? = null,
    private val onConnectionStateChanged: ((device: BluetoothDevice, status: Int, newState: Int) -> Unit)? = null
) {
    private var manufacturer = "rohit_s"
    private var deviceName = "BLE HID"
    private var serialNumber = "12345678"
    protected abstract val reportMap: ByteArray
    protected abstract fun onOutputReport(outputReport: ByteArray?)
    private lateinit var applicationContext: Context
    private lateinit var handler: Handler
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var inputReportCharacteristic: BluetoothGattCharacteristic? = null
    private var gattServer: BluetoothGattServer? = null
    private val bluetoothDevicesMap: MutableMap<String, BluetoothDevice> = HashMap()
    private val servicesToAdd: Queue<BluetoothGattService?> = LinkedBlockingQueue()
    private val inputReportQueue: Queue<ByteArray> = ConcurrentLinkedQueue()
    private val deviceInfoMaxLength = 20
    private val emptyBytes = byteArrayOf()
    private val responseHidInformation = byteArrayOf(0x11, 0x01, 0x00, 0x03)
    private val serviceDeviceInfo = BleUtils.fromShortValue(0x180A)
    private val characteristicManufacturerName = BleUtils.fromShortValue(0x2A29)
    private val characteristicModelNumber = BleUtils.fromShortValue(0x2A24)
    private val characteristicSerialNumber = BleUtils.fromShortValue(0x2A25)
    private val serviceBattery = BleUtils.fromShortValue(0x180F)
    private val characteristicBatteryLevel = BleUtils.fromShortValue(0x2A19)
    private val serviceBleHid = BleUtils.fromShortValue(0x1812)
    private val characteristicHidInformation = BleUtils.fromShortValue(0x2A4A)
    private val characteristicReportMap = BleUtils.fromShortValue(0x2A4B)
    private val characteristicHidControlPoint = BleUtils.fromShortValue(0x2A4C)
    private val characteristicReport = BleUtils.fromShortValue(0x2A4D)
    private val characteristicProtocolMode = BleUtils.fromShortValue(0x2A4E)
    private val descriptorReportReference = BleUtils.fromShortValue(0x2908)
    private val descriptorClientCharacteristicConfig = BleUtils.fromShortValue(0x2902)

    protected fun addInputReport(inputReport: ByteArray?) {
        if (inputReport != null && inputReport.isNotEmpty()) {
            inputReportQueue.offer(inputReport)
        }
    }


    private fun addService(service: BluetoothGattService?) {
        assert(gattServer != null)
        var serviceAdded = false
        while (!serviceAdded) {
            try {
                serviceAdded = gattServer!!.addService(service)
            } catch (e: Exception) {
                Log.d(TAG, "Adding Service failed", e)
            }
        }
        Log.d(TAG, "Service: " + service!!.uuid + " added.")
    }

    private fun setUpHidService(
        isNeedInputReport: Boolean,
        isNeedOutputReport: Boolean,
        isNeedFeatureReport: Boolean
    ): BluetoothGattService {
        val service =
            BluetoothGattService(serviceBleHid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // HID Information
        run {
            val characteristic = BluetoothGattCharacteristic(
                characteristicHidInformation,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }

        // Report Map
        run {
            val characteristic = BluetoothGattCharacteristic(
                characteristicReportMap,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }

        // Protocol Mode
        run {
            val characteristic = BluetoothGattCharacteristic(
                characteristicProtocolMode,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }

        // HID Control Point
        run {
            val characteristic = BluetoothGattCharacteristic(
                characteristicHidControlPoint,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }

        // Input Report
        if (isNeedInputReport) {
            val characteristic = BluetoothGattCharacteristic(
                characteristicReport,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            val clientCharacteristicConfigurationDescriptor = BluetoothGattDescriptor(
                descriptorClientCharacteristicConfig,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            ) //  | BluetoothGattDescriptor.PERMISSION_WRITE
            clientCharacteristicConfigurationDescriptor.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            characteristic.addDescriptor(clientCharacteristicConfigurationDescriptor)
            val reportReferenceDescriptor = BluetoothGattDescriptor(
                descriptorReportReference,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            )
            characteristic.addDescriptor(reportReferenceDescriptor)
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
            inputReportCharacteristic = characteristic
        }

        // Output Report
        if (isNeedOutputReport) {
            val characteristic = BluetoothGattCharacteristic(
                characteristicReport,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            val descriptor = BluetoothGattDescriptor(
                descriptorReportReference,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            )
            characteristic.addDescriptor(descriptor)
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }

        // Feature Report
        if (isNeedFeatureReport) {
            val characteristic = BluetoothGattCharacteristic(
                characteristicReport,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            val descriptor = BluetoothGattDescriptor(
                descriptorReportReference,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            )
            characteristic.addDescriptor(descriptor)
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }
        return service
    }

    fun startAdvertising() {
        handler.post { // set up advertising setting
            val advertiseSettings = AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build()
            // set up advertising data
            val advertiseData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid.fromString(serviceDeviceInfo.toString()))
                .addServiceUuid(ParcelUuid.fromString(serviceBleHid.toString()))
                .addServiceUuid(ParcelUuid.fromString(serviceBattery.toString()))
                .build()
            // set up scan result
            val scanResult = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid.fromString(serviceDeviceInfo.toString()))
                .addServiceUuid(ParcelUuid.fromString(serviceBleHid.toString()))
                .addServiceUuid(ParcelUuid.fromString(serviceBattery.toString()))
                .build()
            bluetoothLeAdvertiser!!.startAdvertising(
                advertiseSettings,
                advertiseData,
                scanResult,
                advertiseCallback
            )
        }
    }

    fun stopAdvertising() {
        handler.post {
            try {
                bluetoothLeAdvertiser!!.stopAdvertising(advertiseCallback)
            } catch (ignored: IllegalStateException) {
                // BT Adapter is not turned ON
            }
            try {
                if (gattServer != null) {
                    val devices: Set<BluetoothDevice> = devices
                    for (device in devices) {
                        gattServer!!.cancelConnection(device)
                    }
                    gattServer!!.close()
                    gattServer = null
                }
            } catch (ignored: IllegalStateException) {
                // BT Adapter is not turned ON
            }
        }
    }

    private val devices: Set<BluetoothDevice>
        get() {
            val deviceSet: MutableSet<BluetoothDevice> = HashSet()
            synchronized(bluetoothDevicesMap) { deviceSet.addAll(bluetoothDevicesMap.values) }
            return Collections.unmodifiableSet(deviceSet)
        }


    private fun onConnectionUpdate(device: BluetoothDevice, status: Int, newState: Int) {
        onConnectionStateChanged?.invoke(device, status, newState)
    }

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            onAdvertiseStateChange?.invoke(false)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            onAdvertiseStateChange?.invoke(true)
        }
    }

    private val gattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(device, status, newState)
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(
                            TAG,
                            "BluetoothProfile.STATE_CONNECTED bondState: " + device.bondState
                        )
                        if (device.bondState == BluetoothDevice.BOND_NONE) {
                            applicationContext.registerReceiver(object : BroadcastReceiver() {
                                override fun onReceive(context: Context, intent: Intent) {
                                    val action = intent.action
                                    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                                        val state = intent.getIntExtra(
                                            BluetoothDevice.EXTRA_BOND_STATE,
                                            BluetoothDevice.ERROR
                                        )
                                        if (state == BluetoothDevice.BOND_BONDED) {
                                            // successfully bonded
                                            context.unregisterReceiver(this)
                                            handler.post {
                                                if (gattServer != null) {
                                                    gattServer!!.connect(device, true)
                                                }
                                            }
                                        }
                                    }
                                }
                            }, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                            // create bond
                            try {
                                device.setPairingConfirmation(true)
                            } catch (e: SecurityException) {
                                Log.d(TAG, e.message, e)
                            }
                            device.createBond()
                        } else if (device.bondState == BluetoothDevice.BOND_BONDED) {
                            handler.post {
                                if (gattServer != null) {
                                    gattServer!!.connect(device, true)
                                }
                            }
                            synchronized(bluetoothDevicesMap) {
                                bluetoothDevicesMap.put(
                                    device.address,
                                    device
                                )
                            }
                        }
                        onConnectionUpdate(device, status, newState)
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        val deviceAddress = device.address

                        // try reconnect immediately
                        handler.post {
                            if (gattServer != null) {
                                // gattServer.cancelConnection(device);
                                gattServer!!.connect(device, true)
                            }
                        }
                        synchronized(bluetoothDevicesMap) { bluetoothDevicesMap.remove(deviceAddress) }
                        onConnectionUpdate(device, status, newState)
                    }

                    else -> {}
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                if (gattServer == null) {
                    return
                }
                Log.d(
                    TAG,
                    "onCharacteristicReadRequest characteristic: " + characteristic.uuid + ", offset: " + offset
                )
                handler.post(object : Runnable {
                    override fun run() {
                        val characteristicUuid = characteristic.uuid
                        if (BleUtils.matches(characteristicHidInformation, characteristicUuid)) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                responseHidInformation
                            )
                        } else if (BleUtils.matches(
                                characteristicReportMap,
                                characteristicUuid
                            )
                        ) {
                            if (offset == 0) {
                                gattServer!!.sendResponse(
                                    device, requestId, BluetoothGatt.GATT_SUCCESS, 0,
                                    reportMap
                                )
                            } else {
                                val remainLength: Int = reportMap.size - offset
                                if (remainLength > 0) {
                                    val data = ByteArray(remainLength)
                                    System.arraycopy(reportMap, offset, data, 0, remainLength)
                                    gattServer!!.sendResponse(
                                        device,
                                        requestId,
                                        BluetoothGatt.GATT_SUCCESS,
                                        offset,
                                        data
                                    )
                                } else {
                                    gattServer!!.sendResponse(
                                        device,
                                        requestId,
                                        BluetoothGatt.GATT_SUCCESS,
                                        offset,
                                        null
                                    )
                                }
                            }
                        } else if (BleUtils.matches(
                                characteristicHidControlPoint,
                                characteristicUuid
                            )
                        ) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                byteArrayOf(0)
                            )
                        } else if (BleUtils.matches(characteristicReport, characteristicUuid)) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                emptyBytes
                            )
                        } else if (BleUtils.matches(
                                characteristicManufacturerName,
                                characteristicUuid
                            )
                        ) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                manufacturer.toByteArray(
                                    StandardCharsets.UTF_8
                                )
                            )
                        } else if (BleUtils.matches(
                                characteristicSerialNumber,
                                characteristicUuid
                            )
                        ) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                serialNumber.toByteArray(
                                    StandardCharsets.UTF_8
                                )
                            )
                        } else if (BleUtils.matches(
                                characteristicModelNumber,
                                characteristicUuid
                            )
                        ) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                deviceName.toByteArray(
                                    StandardCharsets.UTF_8
                                )
                            )
                        } else if (BleUtils.matches(
                                characteristicBatteryLevel,
                                characteristicUuid
                            )
                        ) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                byteArrayOf(0x64)
                            ) // always 100%
                        } else {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                characteristic.value
                            )
                        }
                    }
                })
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor
            ) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)
                Log.d(
                    TAG,
                    "onDescriptorReadRequest requestId: " + requestId + ", offset: " + offset + ", descriptor: " + descriptor.uuid
                )
                if (gattServer == null) {
                    return
                }
                handler.post {
                    if (BleUtils.matches(
                            descriptorReportReference,
                            descriptor.uuid
                        )
                    ) {
                        val characteristicProperties = descriptor.characteristic.properties
                        if (characteristicProperties == BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                            // Input Report
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                byteArrayOf(0, 1)
                            )
                        } else if (characteristicProperties == BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                            // Output Report
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                byteArrayOf(0, 2)
                            )
                        } else if (characteristicProperties == BluetoothGattCharacteristic.PROPERTY_READ || characteristicProperties == BluetoothGattCharacteristic.PROPERTY_WRITE) {
                            // Feature Report
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                byteArrayOf(0, 3)
                            )
                        } else {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_FAILURE,
                                0,
                                emptyBytes
                            )
                        }
                    }
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                Log.d(
                    TAG,
                    "onCharacteristicWriteRequest characteristic: " + characteristic.uuid + ", value: " + Arrays.toString(
                        value
                    )
                )
                if (gattServer == null) {
                    return
                }
                if (responseNeeded) {
                    if (BleUtils.matches(characteristicReport, characteristic.uuid)) {
                        if (characteristic.properties == BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE || characteristic.properties == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                            // Output Report
                            onOutputReport(value)

                            // send empty
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                emptyBytes
                            )
                        } else {
                            // send empty
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                emptyBytes
                            )
                        }
                    }
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                Log.d(
                    TAG,
                    "onDescriptorWriteRequest descriptor: " + descriptor.uuid + ", value: " + Arrays.toString(
                        value
                    ) + ", responseNeeded: " + responseNeeded + ", preparedWrite: " + preparedWrite
                )
                descriptor.value = value
                if (responseNeeded) {
                    if (BleUtils.matches(
                            descriptorClientCharacteristicConfig,
                            descriptor.uuid
                        )
                    ) {
                        // send empty
                        if (gattServer != null) {
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                emptyBytes
                            )
                        }
                    }
                }
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                super.onServiceAdded(status, service)
                Log.d(TAG, "onServiceAdded status: " + status + ", service: " + service.uuid)
                if (status != 0) {
                    Log.d(TAG, "onServiceAdded Adding Service failed..")
                }
                if (servicesToAdd.peek() != null) {
                    addService(servicesToAdd.remove())
                }
            }
        }

    init {
        applicationContext = context.applicationContext
        handler = Handler(applicationContext.mainLooper)
        val bluetoothManager =
            applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
            ?: throw UnsupportedOperationException("Bluetooth is not available.")
        if (!bluetoothAdapter.isEnabled) {
            throw UnsupportedOperationException("Bluetooth is disabled.")
        }
        Log.d(
            TAG,
            "isMultipleAdvertisementSupported:" + bluetoothAdapter.isMultipleAdvertisementSupported
        )
        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            throw UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.")
        }
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        Log.d(
            TAG,
            "bluetoothLeAdvertiser: $bluetoothLeAdvertiser"
        )
        if (bluetoothLeAdvertiser == null) {
            throw UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.")
        }
        gattServer = bluetoothManager.openGattServer(applicationContext, gattServerCallback)
        if (gattServer == null) {
            throw UnsupportedOperationException("gattServer is null, check Bluetooth is ON.")
        }

        // setup services
        servicesToAdd.add(setUpHidService(needInputReport, needOutputReport, needFeatureReport))
        servicesToAdd.add(setUpDeviceInformationService())
        addService(setUpBatteryService())

        // send report each dataSendingRate, if data available
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val polled = inputReportQueue.poll()
                if (polled != null && inputReportCharacteristic != null) {
                    inputReportCharacteristic!!.value = polled
                    handler.post {
                        val devices: Set<BluetoothDevice> = devices
                        for (device in devices) {
                            try {
                                if (gattServer != null) {
                                    gattServer!!.notifyCharacteristicChanged(
                                        device,
                                        inputReportCharacteristic,
                                        false
                                    )
                                }
                            } catch (ignored: Throwable) {
                            }
                        }
                    }
                }
            }
        }, 0, dataSendingRate.toLong())
    }


    private fun setUpDeviceInformationService(): BluetoothGattService {
        val service = BluetoothGattService(
            serviceDeviceInfo,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        run {
            val characteristic = BluetoothGattCharacteristic(
                characteristicManufacturerName,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }
        run {
            val characteristic = BluetoothGattCharacteristic(
                characteristicModelNumber,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }
        run {
            val characteristic = BluetoothGattCharacteristic(
                characteristicSerialNumber,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            while (!service.addCharacteristic(characteristic)) {
                Log.e(TAG, "Adding Char: ${characteristic.uuid}")
            }
        }
        return service
    }

    private fun setUpBatteryService(): BluetoothGattService {
        val service =
            BluetoothGattService(serviceBattery, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Battery Level
        val characteristic = BluetoothGattCharacteristic(
            characteristicBatteryLevel,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        )
        val clientCharacteristicConfigurationDescriptor = BluetoothGattDescriptor(
            descriptorClientCharacteristicConfig,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        clientCharacteristicConfigurationDescriptor.value =
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        characteristic.addDescriptor(clientCharacteristicConfigurationDescriptor)
        while (!service.addCharacteristic(characteristic)) {
            Log.e(TAG, "Adding Char: ${characteristic.uuid}")
        }
        return service
    }

    fun setDeviceName(newDeviceName: String) {
        // length check
        val deviceNameBytes = newDeviceName.toByteArray(StandardCharsets.UTF_8)
        deviceName = if (deviceNameBytes.size > deviceInfoMaxLength) {
            // shorten
            val bytes = ByteArray(deviceInfoMaxLength)
            System.arraycopy(deviceNameBytes, 0, bytes, 0, deviceInfoMaxLength)
            String(bytes, StandardCharsets.UTF_8)
        } else {
            newDeviceName
        }
    }

    fun setSerialNumber(newSerialNumber: String) {
        // length check
        val deviceNameBytes = newSerialNumber.toByteArray(StandardCharsets.UTF_8)
        serialNumber = if (deviceNameBytes.size > deviceInfoMaxLength) {
            // shorten
            val bytes = ByteArray(deviceInfoMaxLength)
            System.arraycopy(deviceNameBytes, 0, bytes, 0, deviceInfoMaxLength)
            String(bytes, StandardCharsets.UTF_8)
        } else {
            newSerialNumber
        }
    }

    fun setManufacturer(newManufacturer: String) {
        // length check
        val manufacturerBytes = newManufacturer.toByteArray(StandardCharsets.UTF_8)
        manufacturer = if (manufacturerBytes.size > deviceInfoMaxLength) {
            // shorten
            val bytes = ByteArray(deviceInfoMaxLength)
            System.arraycopy(manufacturerBytes, 0, bytes, 0, deviceInfoMaxLength)
            String(bytes, StandardCharsets.UTF_8)
        } else {
            newManufacturer
        }
    }

}