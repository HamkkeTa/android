package com.example.hamkketa.boarding

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
import android.os.ParcelUuid
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hamkketa.data.ble.BleBoardingSessionRepository.Companion.JOIN_CHARACTERISTIC_UUID
import com.example.hamkketa.data.ble.BleBoardingSessionRepository.Companion.SERVICE_UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acts as a passenger phone against a car that has an open boarding session.
 * Skipped unless run with -Pandroid.testInstrumentationRunnerArguments.passengerSim=true,
 * because it needs a second device (the car) advertising nearby.
 */
@SuppressLint("MissingPermission")
// writeCharacteristic(characteristic, value, type) needs API 33.
@SdkSuppress(minSdkVersion = 33)
@RunWith(AndroidJUnit4::class)
class PassengerSimulatorTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()
    private val events = LinkedBlockingQueue<Event>()

    @Before
    fun setUp() {
        assumeTrue("passengerSim=true 인자로만 실행", arguments.getString("passengerSim") == "true")
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT).forEach {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it)
        }
    }

    @Test
    fun groupMemberBoardsAndStrangerIsRejected() {
        val holdMillis = (arguments.getString("holdSeconds") ?: "8").toLong() * 1000
        val gatt = scanForCar().connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        try {
            assertEquals(Event.Connected, events.next())
            gatt.discoverServices()
            assertEquals(Event.ServicesDiscovered, events.next())
            val join = gatt.getService(SERVICE_UUID)?.getCharacteristic(JOIN_CHARACTERISTIC_UUID)
            assertNotNull("함께타 서비스/특성 없음", join)

            assertEquals(BluetoothGatt.GATT_SUCCESS, gatt.write(join!!, "member-1"))
            // Time to look at the car screen: one rider icon should be visible.
            Thread.sleep(holdMillis)

            assertNotEquals(BluetoothGatt.GATT_SUCCESS, gatt.write(join, "stranger"))
        } finally {
            gatt.disconnect()
            gatt.close()
        }
    }

    private fun scanForCar(): BluetoothDevice {
        val scanner = context.getSystemService(
            BluetoothManager::class.java
        ).adapter.bluetoothLeScanner
        val found = LinkedBlockingQueue<BluetoothDevice>()
        val scanCallback =
            object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    found.offer(result.device)
                }
            }
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(
            ScanSettings.SCAN_MODE_LOW_LATENCY
        ).build()
        scanner.startScan(listOf(filter), settings, scanCallback)
        try {
            return found.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) ?: error("차량 광고를 찾지 못함")
        } finally {
            scanner.stopScan(scanCallback)
        }
    }

    private fun BluetoothGatt.write(
        characteristic: BluetoothGattCharacteristic,
        text: String
    ): Int {
        writeCharacteristic(
            characteristic,
            text.encodeToByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        return (events.next() as Event.Written).status
    }

    private fun LinkedBlockingQueue<Event>.next(): Event =
        poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) ?: error("BLE 응답 시간 초과")

    private val callback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) events.offer(Event.Connected)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                events.offer(Event.ServicesDiscovered)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                events.offer(Event.Written(status))
            }
        }

    private sealed interface Event {
        data object Connected : Event

        data object ServicesDiscovered : Event

        data class Written(val status: Int) : Event
    }

    private companion object {
        const val TIMEOUT_SECONDS = 20L
    }
}
