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

    override fun onOutputReport(outputReport: ByteArray?) {
        // do nothing
    }


    override val reportMap = byteArrayOf(
        USAGE_PAGE(1), 0x01,  // Generic Desktop
        USAGE(1), 0x02,  // Mouse
        COLLECTION(1), 0x01,  // Application
        USAGE(1), 0x01,  //  Pointer
        COLLECTION(1), 0x00,  //  Physical
        USAGE_PAGE(1), 0x09,  //   Buttons
        USAGE_MINIMUM(1), 0x01,
        USAGE_MAXIMUM(1), 0x03,
        LOGICAL_MINIMUM(1), 0x00,
        LOGICAL_MAXIMUM(1), 0x01,
        REPORT_COUNT(1), 0x03,  //   3 bits (Buttons)
        REPORT_SIZE(1), 0x01,
        INPUT(1), 0x02,  //   Data, Variable, Absolute
        REPORT_COUNT(1), 0x01,  //   5 bits (Padding)
        REPORT_SIZE(1), 0x05,
        INPUT(1), 0x01,  //   Constant
        USAGE_PAGE(1), 0x01,  //   Generic Desktop
        USAGE(1), 0x30,  //   X
        USAGE(1), 0x31,  //   Y
        USAGE(1), 0x38,  //   Wheel
        LOGICAL_MINIMUM(1), 0x81.toByte(),  //   -127
        LOGICAL_MAXIMUM(1), 0x7f,  //   127
        REPORT_SIZE(1), 0x08,  //   Three bytes
        REPORT_COUNT(1), 0x03,
        INPUT(1), 0x06,  //   Data, Variable, Relative
        END_COLLECTION(0),
        END_COLLECTION(0)
    )

}