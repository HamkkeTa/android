package com.example.hamkketa.passenger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

/** Foreground-only diagnostic central. RSSI is a test measurement, not proof of identity. */
@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {
    private val service = UUID.fromString("6b3f1a52-2c4e-4d8a-9b61-3a0f5e7c9d10")
    private val join = UUID.fromString("6b3f1a53-2c4e-4d8a-9b61-3a0f5e7c9d10")
    private val signal = UUID.fromString("6b3f1a54-2c4e-4d8a-9b61-3a0f5e7c9d10")
    private val handler = Handler(Looper.getMainLooper())
    private var status by mutableStateOf("차량 앱의 연결 ID를 입력해 주세요.")
    private var member by mutableStateOf("")
    private var devices by mutableStateOf<List<BluetoothDevice>>(emptyList())
    private var gatt: BluetoothGatt? = null
    private var scan = false
    private val manager get() = getSystemService(BluetoothManager::class.java)
    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) scan() else status = "블루투스 권한이 필요합니다."
        }
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            runOnUiThread {
                if (devices.none { it.address == result.device.address }) {
                    devices =
                        devices + result.device
                }
            }
        }
        override fun onScanFailed(errorCode: Int) {
            runOnUiThread { status = "검색 실패: $errorCode" }
        }
    }
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, code: Int, state: Int) {
            if (code == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                if (!g.requestMtu(185)) {
                    runOnUiThread { status = "MTU 협상 실패: 다시 연결해 주세요." }
                    g.disconnect()
                }
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                handler.removeCallbacks(rssiLoop)
                runOnUiThread { status = "연결 해제 · 차량에서 하차 유예 후 확정" }
                g.close()
                if (gatt === g) gatt = null
            }
        }
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS &&
                mtu >= member.toByteArray(Charsets.UTF_8).size + 3
            ) {
                g.discoverServices()
            } else {
                runOnUiThread { this@MainActivity.status = "연결 ID 전송에 필요한 MTU를 확보하지 못했습니다." }
                g.disconnect()
            }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, code: Int) {
            val characteristic = g.getService(service)?.getCharacteristic(join)
            if (code != BluetoothGatt.GATT_SUCCESS || characteristic == null) {
                runOnUiThread { status = "함께타 서비스가 없습니다." }
                return
            }
            write(g, characteristic, member.toByteArray(Charsets.UTF_8))
        }
        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            code: Int
        ) {
            if (code != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread { status = "등록 실패: 그룹 ID와 시작 상태 확인 ($code)" }
                return
            }
            if (characteristic.uuid == join) {
                handler.removeCallbacks(rssiLoop)
                handler.post(rssiLoop)
            }
        }
        override fun onReadRemoteRssi(g: BluetoothGatt, rssi: Int, code: Int) {
            if (code != BluetoothGatt.GATT_SUCCESS) return
            runOnUiThread { status = "연결됨 · RSSI $rssi dBm · 도어 이벤트 후 차량에서 탑승 판정" }
            g.getService(service)?.getCharacteristic(signal)?.let {
                write(g, it, rssi.toString().toByteArray(Charsets.UTF_8))
            }
        }
    }
    private val rssiLoop = object : Runnable {
        override fun run() {
            gatt?.readRemoteRssi()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("함께타 동승자 BLE 테스트")
                    Text(status)
                    OutlinedTextField(member, {
                        if (gatt ==
                            null
                        ) {
                            member = it
                        }
                    }, label = { Text("차량 그룹의 연결 ID") })
                    Button(onClick = {
                        permission.launch(
                            if (Build.VERSION.SDK_INT >=
                                31
                            ) {
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            } else {
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        )
                    }) { Text("차량 검색 (20초)") }
                    devices.forEach { device ->
                        Button(onClick = {
                            if (member.isBlank() ||
                                member.toByteArray().size > 64
                            ) {
                                status =
                                    "64바이트 이내의 연결 ID를 입력해 주세요."
                            } else {
                                disconnect()
                                stopScan()
                                gatt =
                                    device.connectGatt(
                                        this@MainActivity,
                                        false,
                                        gattCallback,
                                        BluetoothDevice.TRANSPORT_LE
                                    )
                            }
                        }) { Text(device.address) }
                    }
                    Button(onClick = { disconnect() }) { Text("연결 해제") }
                    Text("앱이 화면에 있는 동안만 동작합니다. 에뮬레이터의 BLE 무선 지원이 없으면 차량 앱의 수동 테스트를 사용하세요.")
                }
            }
        }
    }
    private fun scan() {
        stopScan()
        devices = emptyList()
        val scanner = manager?.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) {
            status = "BLE 검색 미지원 또는 블루투스 꺼짐"
            return
        }
        scanner.startScan(
            listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(service)).build()),
            ScanSettings.Builder().build(),
            callback
        )
        scan = true
        status = "차량 검색 중"
        handler.postDelayed({ stopScan() }, 20_000)
    }
    private fun stopScan() {
        if (scan) manager?.adapter?.bluetoothLeScanner?.stopScan(callback)
        scan =
            false
    }
    private fun disconnect() {
        handler.removeCallbacks(rssiLoop)
        gatt?.let {
            it.disconnect()
            it.close()
        }
        gatt = null
    }

    @Suppress("DEPRECATION")
    private fun write(
        g: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (Build.VERSION.SDK_INT >=
            33
        ) {
            g.writeCharacteristic(
                characteristic,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            characteristic.value = value
            characteristic.writeType =
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            g.writeCharacteristic(characteristic)
        }
    }
    override fun onStop() {
        stopScan()
        disconnect()
        super.onStop()
    }
}
