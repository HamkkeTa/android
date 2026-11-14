package com.example.hamkketa.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import com.example.hamkketa.domain.ride.BoardingState
import com.example.hamkketa.domain.ride.RideGroupRepository
import com.example.hamkketa.domain.ride.SessionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The car acts as a BLE peripheral. A passenger app connects and writes its member id
 * (UTF-8) to [JOIN_CHARACTERISTIC_UUID]; members of the active group are boarded, and a
 * disconnect alights them after a reconnect grace period.
 */
// Every BLE call runs only after open() has checked BlePermissions.
@SuppressLint("MissingPermission")
@Singleton
class BleBoardingSessionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val groupRepository: RideGroupRepository
) : BoardingSessionRepository {
    private val applicationContext = context
    private val mutableState = MutableStateFlow(BoardingState())
    override val state: StateFlow<BoardingState> = mutableState.asStateFlow()

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pendingAlights = mutableMapOf<String, Runnable>()

    @Synchronized
    override fun open() {
        if (gattServer != null) return
        val leAdvertiser =
            bluetoothManager
                ?.adapter
                ?.takeIf { BlePermissions.granted(applicationContext) && it.isEnabled }
                ?.bluetoothLeAdvertiser
        val server = leAdvertiser?.let {
            bluetoothManager.openGattServer(applicationContext, gattCallback)
        }
        if (server == null) {
            Log.w(TAG, "BLE session unavailable: permission, adapter or advertiser missing")
            mutableState.update { it.copy(status = SessionStatus.Unavailable) }
            return
        }
        gattServer = server
        advertiser = leAdvertiser
        if (!server.addService(boardingService())) {
            close()
            mutableState.update { it.copy(status = SessionStatus.Unavailable) }
        }
    }

    @Synchronized
    override fun close() {
        pendingAlights.values.forEach(handler::removeCallbacks)
        pendingAlights.clear()
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        advertiser = null
        gattServer = null
        mutableState.value = BoardingState()
    }

    private val gattCallback =
        object : BluetoothGattServerCallback() {
            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                synchronized(this@BleBoardingSessionRepository) {
                    if (gattServer == null) return
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        advertiser?.startAdvertising(
                            advertiseSettings,
                            advertiseData,
                            advertiseCallback
                        )
                    } else {
                        close()
                        mutableState.update { it.copy(status = SessionStatus.Unavailable) }
                    }
                }
            }

            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    synchronized(this@BleBoardingSessionRepository) {
                        val address = device.address
                        if (mutableState.value.ridersByConnection[address] == null) return
                        pendingAlights.remove(address)?.let(handler::removeCallbacks)
                        val task = object : Runnable {
                            override fun run() {
                                synchronized(this@BleBoardingSessionRepository) {
                                    if (pendingAlights[address] !== this) return
                                    pendingAlights.remove(address)
                                    mutableState.update { it.alighted(address) }
                                }
                            }
                        }
                        pendingAlights[address] = task
                        handler.postDelayed(task, ALIGHT_GRACE_MS)
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
                value: ByteArray?
            ) {
                // Untrusted input from any nearby device: accept only a short id that is in the group.
                val member =
                    value
                        ?.takeIf {
                            characteristic.uuid == JOIN_CHARACTERISTIC_UUID &&
                                !preparedWrite &&
                                offset == 0 &&
                                it.size <= MAX_MEMBER_ID_BYTES
                        }?.let {
                            groupRepository.activeGroup.value?.findMember(it.decodeToString())
                        }
                if (member != null) {
                    synchronized(this@BleBoardingSessionRepository) {
                        if (gattServer == null) return
                        val oldConnections = mutableState.value.ridersByConnection
                            .filterValues { it.id == member.id }.keys + device.address
                        oldConnections.forEach { address ->
                            pendingAlights.remove(address)?.let(handler::removeCallbacks)
                        }
                        mutableState.update { current ->
                            current.copy(
                                ridersByConnection =
                                current.ridersByConnection - oldConnections
                            )
                                .boarded(device.address, member)
                        }
                    }
                }
                if (responseNeeded) {
                    val status = if (member !=
                        null
                    ) {
                        BluetoothGatt.GATT_SUCCESS
                    } else {
                        BluetoothGatt.GATT_FAILURE
                    }
                    gattServer?.sendResponse(device, requestId, status, 0, null)
                }
            }
        }

    private val advertiseCallback =
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                synchronized(this@BleBoardingSessionRepository) {
                    if (gattServer !=
                        null
                    ) {
                        mutableState.update { it.copy(status = SessionStatus.Open) }
                    }
                }
            }

            override fun onStartFailure(errorCode: Int) {
                Log.w(TAG, "BLE advertising failed: $errorCode")
                synchronized(this@BleBoardingSessionRepository) {
                    close()
                    mutableState.update { it.copy(status = SessionStatus.Unavailable) }
                }
            }
        }

    private fun boardingService() =
        BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).apply {
            addCharacteristic(
                BluetoothGattCharacteristic(
                    JOIN_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE
                )
            )
        }

    private val advertiseSettings =
        AdvertiseSettings
            .Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .build()

    // Device name is left out: it does not fit next to a 128-bit UUID and identifies the car.
    private val advertiseData =
        AdvertiseData
            .Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()

    companion object {
        // Shared with the passenger app; changing these breaks pairing with released apps.
        val SERVICE_UUID: UUID = UUID.fromString("6b3f1a52-2c4e-4d8a-9b61-3a0f5e7c9d10")
        val JOIN_CHARACTERISTIC_UUID: UUID = UUID.fromString("6b3f1a53-2c4e-4d8a-9b61-3a0f5e7c9d10")
        private const val MAX_MEMBER_ID_BYTES = 64
        private const val TAG = "BoardingSession"

        // Pilot value; door/seat confirmation still requires a vehicle-sensor integration.
        const val ALIGHT_GRACE_MS = 30_000L
    }
}
