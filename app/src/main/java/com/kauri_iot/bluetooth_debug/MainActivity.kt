package com.kauri_iot.bluetooth_debug

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.kauri_iot.bluetooth_debug.ui.theme.Bluetooth_debugTheme
import java.lang.reflect.Method
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val laptop = "88:78:73:2B:D9:F9"
    private val pc = "8C:88:2B:00:8A:89"
    private var mac: String = "88:78:73:2B:D9:F9"
    private lateinit var receiver: Receiver
    private lateinit var manager: BluetoothManager
    private lateinit var adapter: BluetoothAdapter
    private lateinit var liveData: MutableLiveData<BluetoothDevice>

    override fun onResume() {
        registerBroadcastReceiver()
        super.onResume()
    }

    override fun onPause() {
        unregisterBroadcastReceiver()
        super.onPause()
    }

    private fun registerBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        this.registerReceiver(
            receiver, filter
        )
    }

    private fun unregisterBroadcastReceiver() {
        this.unregisterReceiver(receiver)
    }

    private fun connect(mac: String) {
        if (adapter.bondedDevices.any { it.address == mac }) {
            Toast.makeText(this, "Уже запарены!", Toast.LENGTH_LONG).show()
            return
        }
        adapter.startDiscovery()
        liveData.observe(this, {
            if (it.address == mac) {
                val method: Method = it.javaClass.getMethod("createBond")
                method.invoke(it)
                adapter.cancelDiscovery()
            }
        })
        runOnUiThread {
            Toast.makeText(this, "Присоединяюсь...", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun askForPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ),
                abs(Random.nextInt())
            )
            return
        }
    }

    private fun send(content: String, mac: String) {
        thread {
            try {
                val boundedDevices = adapter.bondedDevices.toList()
                val device = boundedDevices.first { it.address == mac }
                val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.java)
                val deviceSocket = m.invoke(device, 1) as BluetoothSocket
                deviceSocket.connect()
                val outputStream = deviceSocket.outputStream
                outputStream.write(content.toByteArray())
                outputStream.flush()
                Thread.sleep(5000)
                outputStream.close()
                runOnUiThread {
                    Toast.makeText(this, "Отправляю слова...", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Упс: $e", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askForPermissions()
        receiver = Receiver()
        liveData = receiver.device
        manager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter

        setContent {
            Bluetooth_debugTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Scaffold(
                        topBar = { TopAppBar(title = { Text("Bluetooth debug") }) },
                        content = { BodyContent() },
                    )
                }
            }
        }
    }

    @Composable
    fun BodyContent() {
        var mac by remember { mutableStateOf(laptop) }
        var content by remember { mutableStateOf("Hello, world!") }
        var switched by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .padding(30.dp)
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = mac,
                onValueChange = {
                    mac = it
                },
                label = { Text("Mак") }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Switch(checked = switched, onCheckedChange = {
                mac = if (mac == laptop) pc else laptop
                this@MainActivity.mac = mac
                switched = !switched
                return@Switch
            })
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                value = content,
                onValueChange = {
                    content = it
                },
                label = { Text("Cтрока") }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                connect(mac)
            }) {
                Text("Запариться")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                send(content, mac)
            }) {
                Text("Отправить")
            }
        }
    }
}

