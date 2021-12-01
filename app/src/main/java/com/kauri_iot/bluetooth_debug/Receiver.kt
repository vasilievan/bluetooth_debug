package com.kauri_iot.bluetooth_debug

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.MutableLiveData

class Receiver : BroadcastReceiver() {
    val device = MutableLiveData<BluetoothDevice>()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
            val state = intent?.extras?.get(BluetoothDevice.EXTRA_BOND_STATE) as Int
            if (state == 12) {
                Toast.makeText(context, "Запарились!", Toast.LENGTH_LONG).show()
            }
        } else if (intent?.action.equals(BluetoothDevice.ACTION_FOUND)) {
            val device = intent?.extras?.get(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
            this.device.value = device
        }
    }
}