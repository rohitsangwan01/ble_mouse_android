package com.rohit.blehid.ble

import android.content.Context
import android.util.Log
import java.util.Arrays


class KeyboardPeripheral(context: Context) :
    HidPeripheral(context.applicationContext, true, true, false, 20) {

    fun sendKeys(text: String) {
        val lastKey: String? = null
        for (i in 0 until text.length) {
            val key = text.substring(i, i + 1)
            val report = ByteArray(8)
            report[KEY_PACKET_MODIFIER_KEY_INDEX] = modifier(key)
            report[KEY_PACKET_KEY_INDEX] = keyCode(key)
            if (key == lastKey) {
                sendKeyUp()
            }
            addInputReport(report)
            //lastKey = key;
            sendKeyUp()
        }
        sendKeyUp()
    }

    fun sendKeyDown(modifier: Byte, keyCode: Byte) {
        val report = ByteArray(8)
        report[KEY_PACKET_MODIFIER_KEY_INDEX] = modifier
        report[KEY_PACKET_KEY_INDEX] = keyCode
        addInputReport(report)
    }

    fun sendKeyUp() {
        addInputReport(EMPTY_REPORT)
    }

    override fun onOutputReport(outputReport: ByteArray?) {
        Log.i(TAG, "onOutputReport data: " + Arrays.toString(outputReport))
    }

    override val reportMap = byteArrayOf(
        usagePage(1),
        0x01,  // Generic Desktop Ctrls
        usage(1),
        0x06,  // Keyboard
        collection(1),
        0x01,  // Application
        usagePage(1),
        0x07,  //   Kbrd/Keypad
        usageMinimum(1),
        0xE0.toByte(),
        usageMaximum(1),
        0xE7.toByte(),
        logicalMinimum(1),
        0x00,
        logicalMaximum(1),
        0x01,
        reportSize(1),
        0x01,  //   1 byte (Modifier)
        reportCount(1),
        0x08,
        input(1),
        0x02,  //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position
        reportCount(1),
        0x01,  //   1 byte (Reserved)
        reportSize(1),
        0x08,
        input(1),
        0x01,  //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
        reportCount(1),
        0x05,  //   5 bits (Num lock, Caps lock, Scroll lock, Compose, Kana)
        reportSize(1),
        0x01,
        usagePage(1),
        0x08,  //   LEDs
        usageMinimum(1),
        0x01,  //   Num Lock
        usageMaximum(1),
        0x05,  //   Kana
        output(1),
        0x02,  //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
        reportCount(1),
        0x01,  //   3 bits (Padding)
        reportSize(1),
        0x03,
        output(1),
        0x01,  //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
        reportCount(1),
        0x06,  //   6 bytes (Keys)
        reportSize(1),
        0x08,
        logicalMinimum(1),
        0x00,
        logicalMaximum(1),
        0x65,  //   101 keys
        usagePage(1),
        0x07,  //   Keypad
        usageMinimum(1),
        0x00,
        usageMaximum(1),
        0x65,
        input(1),
        0x00,  //   Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
        endCollection(0)
    )

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
    private fun output(size: Int): Byte = (0x90 or size).toByte()

    companion object {
        private val TAG = KeyboardPeripheral::class.java.simpleName
        const val MODIFIER_KEY_NONE = 0
        const val MODIFIER_KEY_CTRL = 1
        const val MODIFIER_KEY_SHIFT = 2
        const val MODIFIER_KEY_ALT = 4
        const val KEY_F1 = 0x3a
        const val KEY_F2 = 0x3b
        const val KEY_F3 = 0x3c
        const val KEY_F4 = 0x3d
        const val KEY_F5 = 0x3e
        const val KEY_F6 = 0x3f
        const val KEY_F7 = 0x40
        const val KEY_F8 = 0x41
        const val KEY_F9 = 0x42
        const val KEY_F10 = 0x43
        const val KEY_F11 = 0x44
        const val KEY_F12 = 0x45
        const val KEY_PRINT_SCREEN = 0x46
        const val KEY_SCROLL_LOCK = 0x47
        const val KEY_CAPS_LOCK = 0x39
        const val KEY_NUM_LOCK = 0x53
        const val KEY_INSERT = 0x49
        const val KEY_HOME = 0x4a
        const val KEY_PAGE_UP = 0x4b
        const val KEY_PAGE_DOWN = 0x4e
        const val KEY_RIGHT_ARROW = 0x4f
        const val KEY_LEFT_ARROW = 0x50
        const val KEY_DOWN_ARROW = 0x51
        const val KEY_UP_ARROW = 0x52

        fun modifier(aChar: String?): Byte {
            return when (aChar) {
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+", "{", "}", "|", ":", "\"", "~", "<", ">", "?" -> MODIFIER_KEY_SHIFT.toByte()
                else -> 0
            }
        }

        fun keyCode(aChar: String?): Byte {
            return when (aChar) {
                "A", "a" -> 0x04
                "B", "b" -> 0x05
                "C", "c" -> 0x06
                "D", "d" -> 0x07
                "E", "e" -> 0x08
                "F", "f" -> 0x09
                "G", "g" -> 0x0a
                "H", "h" -> 0x0b
                "I", "i" -> 0x0c
                "J", "j" -> 0x0d
                "K", "k" -> 0x0e
                "L", "l" -> 0x0f
                "M", "m" -> 0x10
                "N", "n" -> 0x11
                "O", "o" -> 0x12
                "P", "p" -> 0x13
                "Q", "q" -> 0x14
                "R", "r" -> 0x15
                "S", "s" -> 0x16
                "T", "t" -> 0x17
                "U", "u" -> 0x18
                "V", "v" -> 0x19
                "W", "w" -> 0x1a
                "X", "x" -> 0x1b
                "Y", "y" -> 0x1c
                "Z", "z" -> 0x1d
                "!", "1" -> 0x1e
                "@", "2" -> 0x1f
                "#", "3" -> 0x20
                "$", "4" -> 0x21
                "%", "5" -> 0x22
                "^", "6" -> 0x23
                "&", "7" -> 0x24
                "*", "8" -> 0x25
                "(", "9" -> 0x26
                ")", "0" -> 0x27
                "\n" -> 0x28
                "\b" -> 0x2a
                "\t" -> 0x2b
                " " -> 0x2c
                "_", "-" -> 0x2d
                "+", "=" -> 0x2e
                "{", "[" -> 0x2f
                "}", "]" -> 0x30
                "|", "\\" -> 0x31
                ":", ";" -> 0x33
                "\"", "'" -> 0x34
                "~", "`" -> 0x35
                "<", "," -> 0x36
                ">", "." -> 0x37
                "?", "/" -> 0x38
                else -> 0
            }
        }

        private const val KEY_PACKET_MODIFIER_KEY_INDEX = 0
        private const val KEY_PACKET_KEY_INDEX = 2
        private val EMPTY_REPORT = ByteArray(8)
    }
}