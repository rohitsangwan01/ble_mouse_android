package com.rohit.blehid.ble

import android.content.Context

class MousePeripheral(context: Context) :
    HidPeripheral(context.applicationContext, true, false, false, 10) {

    private val lastSent = ByteArray(4)

    fun movePointer(
        dxx: Int,
        dyy: Int,
        wheelBtn: Int,
        leftButton: Boolean,
        rightButton: Boolean,
        middleButton: Boolean
    ) {
        var dx = dxx
        var dy = dyy
        var wheel = wheelBtn
        if (dx > 127) dx = 127
        if (dx < -127) dx = -127
        if (dy > 127) dy = 127
        if (dy < -127) dy = -127
        if (wheel > 127) wheel = 127
        if (wheel < -127) wheel = -127
        var button: Byte = 0
        if (leftButton) {
            button = (button.toInt() or 1).toByte()
        }
        if (rightButton) {
            button = (button.toInt() or 2).toByte()
        }
        if (middleButton) {
            button = (button.toInt() or 4).toByte()
        }
        val report = ByteArray(4)
        report[0] = (button.toInt() and 7).toByte()
        report[1] = dx.toByte()
        report[2] = dy.toByte()
        report[3] = wheel.toByte()
        if (
            lastSent[0].toInt() == 0
            && lastSent[1].toInt() == 0
            && lastSent[2].toInt() == 0
            && lastSent[3].toInt() == 0
            && report[0].toInt() == 0
            && report[1].toInt() == 0
            && report[2].toInt() == 0
            && report[3].toInt() == 0
        ) {
            return
        }
        lastSent[0] = report[0]
        lastSent[1] = report[1]
        lastSent[2] = report[2]
        lastSent[3] = report[3]
        addInputReport(report)
    }


    override val reportMap = byteArrayOf(
        usagePage(1), 0x01,  // Generic Desktop
        usage(1), 0x02,  // Mouse
        collection(1), 0x01,  // Application
        usage(1), 0x01,  //  Pointer
        collection(1), 0x00,  //  Physical
        usagePage(1), 0x09,  //   Buttons
        usageMinimum(1), 0x01,
        usageMaximum(1), 0x03,
        logicalMinimum(1), 0x00,
        logicalMaximum(1), 0x01,
        reportCount(1), 0x03,  //   3 bits (Buttons)
        reportSize(1), 0x01,
        input(1), 0x02,  //   Data, Variable, Absolute
        reportCount(1), 0x01,  //   5 bits (Padding)
        reportSize(1), 0x05,
        input(1), 0x01,  //   Constant
        usagePage(1), 0x01,  //   Generic Desktop
        usage(1), 0x30,  //   X
        usage(1), 0x31,  //   Y
        usage(1), 0x38,  //   Wheel
        logicalMinimum(1), 0x81.toByte(),  //   -127
        logicalMaximum(1), 0x7f,  //   127
        reportSize(1), 0x08,  //   Three bytes
        reportCount(1), 0x03,
        input(1), 0x06,  //   Data, Variable, Relative
        endCollection(0),
        endCollection(0)
    )

    override fun onOutputReport(outputReport: ByteArray?) {
        // do nothing
    }
    private fun input(size: Int): Byte = (0x80 or size).toByte()
    private fun collection(size: Int): Byte = (0xA0 or size).toByte()
    private fun endCollection(size: Int): Byte = (0xC0 or size).toByte()
    private fun usagePage(size: Int): Byte = (0x04 or size).toByte()
    private fun logicalMinimum(size: Int): Byte = (0x14 or size).toByte()
    private fun logicalMaximum(size: Int): Byte = (0x24 or size).toByte()
    private fun reportSize(size: Int): Byte = (0x74 or size).toByte()
    private fun reportCount(size: Int): Byte = (0x94 or size).toByte()
    private fun usage(size: Int): Byte = (0x08 or size).toByte()
    private fun usageMinimum(size: Int): Byte = (0x18 or size).toByte()
    private fun usageMaximum(size: Int): Byte = (0x28 or size).toByte()
//        fun reportId(size: Int): Byte = (0x84 or size).toByte()
//        fun lsb(value: Int): Byte = (value and 0xff).toByte()
//        fun msb(value: Int): Byte = (value shr 8 and 0xff).toByte()
//        fun physicalMinimum(size: Int): Byte = (0x34 or size).toByte()
//        fun physicalMaximum(size: Int): Byte = (0x44 or size).toByte()
//        fun unitExponent(size: Int): Byte = (0x54 or size).toByte()
//        fun feature(size: Int): Byte = (0xB0 or size).toByte()
//        fun output(size: Int): Byte = (0x90 or size).toByte()
//        fun unit(size: Int): Byte = (0x64 or size).toByte()

}