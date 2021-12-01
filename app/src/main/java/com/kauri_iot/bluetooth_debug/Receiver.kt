package com.kauri_iot.bluetooth_debug

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import kotlin.concurrent.thread

class Receiver(private val activity: Activity) : BroadcastReceiver() {
    val device = MutableLiveData<BluetoothDevice>()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
            val state = intent?.extras?.get(BluetoothDevice.EXTRA_BOND_STATE) as Int
            if (state == 12) {
                thread {
                    val manager =
                        context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val adapter = manager.adapter
                    val boundedDevices = adapter.bondedDevices.toList()
                    val currentMac = activity.javaClass.declaredFields.firstOrNull {
                        it.name == "mac"
                    }
                    currentMac?.isAccessible = true
                    val device = boundedDevices.first { it.address == currentMac?.get(activity) }
                    currentMac?.isAccessible = false
                    val m =
                        device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.java)
                    val deviceSocket = m.invoke(device, 1) as BluetoothSocket
                    deviceSocket.connect()
                    val outputStream = deviceSocket.outputStream
                    outputStream.write("Hello, Sasha".toByteArray())
                    outputStream.flush()
                    Thread.sleep(5000)
                    outputStream.close()
                }
            }
        } else if (intent?.action.equals(BluetoothDevice.ACTION_FOUND)) {
            val device = intent?.extras?.get(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
            this.device.value = device
        }
    }
}