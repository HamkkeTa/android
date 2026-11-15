package com.example.hamkketa.data.ride

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.example.hamkketa.domain.ride.RiderEvent
import com.example.hamkketa.domain.ride.TripBoardingPolicy
import com.example.hamkketa.domain.ride.TripCalculator
import com.example.hamkketa.domain.ride.TripExpense
import com.example.hamkketa.domain.ride.TripRecord
import com.example.hamkketa.domain.ride.TripSettings
import com.example.hamkketa.domain.ride.activePassengerIds
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TripStore(
    val settings: TripSettings = TripSettings(),
    val current: TripRecord? = null,
    val records: List<TripRecord> = emptyList()
)

@Singleton
class TripRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val groups: LocalRideGroupRepository
) {
    private val prefs = context.getSharedPreferences("trip-records-v1", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val loaded = runCatching {
        gson.fromJson(prefs.getString("store", null), TripStore::class.java)
    }.getOrNull() ?: TripStore()

    // A process gap cannot be reconstructed from the final odometer reading.
    private val mutable = MutableStateFlow(
        loaded.copy(
            current = null,
            records =
            loaded.records +
                listOfNotNull(
                    loaded.current?.copy(interrupted = true, endedAt = System.currentTimeMillis())
                )
        )
    )
    val state = mutable.asStateFlow()
    private val mutableMessage = MutableStateFlow("운행 설정에서 거리 입력 방식을 확인해 주세요.")
    val message = mutableMessage.asStateFlow()
    private val mutableKm = MutableStateFlow<Double?>(null)
    val km = mutableKm.asStateFlow()
    private var readingAt = 0L
    private var readingValid = false
    private var policy: TripBoardingPolicy? = null
    private val handler = Handler(Looper.getMainLooper())
    private val vehicle = Vehicle(context)
    private var initialized = false
    private var polling = false
    private val openDoors = mutableSetOf<DoorArea>()
    private val doorListeners = mutableMapOf<DoorArea, DoorPositionListener>()
    private val mutableDoorStatus = MutableStateFlow("도어 센서 확인 전")
    val doorStatus = mutableDoorStatus.asStateFlow()

    init {
        persist(mutable.value)
    }

    @Synchronized
    private fun persist(value: TripStore): Boolean {
        if (!prefs.edit().putString("store", gson.toJson(value)).commit()) {
            mutableMessage.value = "저장 실패: 운행을 계속하기 전에 저장 공간을 확인해 주세요."
            return false
        }
        mutable.value = value
        return true
    }

    @Synchronized
    fun configure(settings: TripSettings): Boolean {
        if (state.value.current != null) return false
        settings.validate()
        val sourceChanged = state.value.settings.simulation != settings.simulation
        if (!persist(state.value.copy(settings = settings))) return false
        if (sourceChanged) {
            stopPolling()
            openDoors.clear()
            mutableKm.value = null
            readingAt = 0
            readingValid = false
        }
        startPolling()
        return true
    }

    @Synchronized
    fun startPolling() {
        if (polling) return
        polling = true
        handler.post(ticker)
    }

    @Synchronized
    fun stopPolling() {
        polling = false
        handler.removeCallbacks(ticker)
        if (initialized) {
            doorListeners.forEach { (area, listener) ->
                runCatching { vehicle.getDoor().unregisterDoorPosition(area, listener) }
            }
            doorListeners.clear()
            vehicle.release()
            initialized = false
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            synchronized(this@TripRepository) {
                append(policy?.tick(SystemClock.elapsedRealtime()).orEmpty())
                if (!state.value.settings.simulation) {
                    runCatching {
                        if (!initialized) {
                            vehicle.initialize()
                            initialized = true
                        }
                        vehicle.getOdometer().checkCurrentDistanceDrivenCapabilities(
                            { config ->
                                if (config.accessType == AccessType.READ ||
                                    config.accessType == AccessType.READ_WRITE
                                ) {
                                    vehicle.getOdometer().getCurrentDistanceDriven(
                                        { value -> value?.let { acceptKm(it.toDouble(), false) } },
                                        { error ->
                                            mutableMessage.value =
                                                "차량 거리 수신 실패: ${error.message}"
                                        }
                                    )
                                } else {
                                    mutableMessage.value = "차량 거리 읽기 미지원"
                                }
                            },
                            { error -> mutableMessage.value = "거리 지원 확인 실패: ${error.message}" }
                        )
                        vehicle.getDoor().checkDoorPositionCapabilities(
                            { config, areas ->
                                if (config.accessType != AccessType.READ &&
                                    config.accessType != AccessType.READ_WRITE
                                ) {
                                    mutableDoorStatus.value = "도어 읽기 미지원: 탑승 판정을 진행할 수 없어요."
                                    return@checkDoorPositionCapabilities
                                }
                                areas.keys.filter { area ->
                                    area.values.any { it.name.startsWith("ROW_") }
                                }
                                    .forEach { area ->
                                        if (area !in doorListeners) {
                                            val listener = object : DoorPositionListener {
                                                override fun onPositionUpdated(pos: Int?) {
                                                    observeDoor(area, pos)
                                                }
                                                override fun onFailed(e: Exception) {
                                                    mutableDoorStatus.value =
                                                        "도어 이벤트 수신 실패: ${e.message}"
                                                }
                                            }
                                            doorListeners[area] = listener
                                            vehicle.getDoor().registerDoorPosition(area, listener)
                                        }
                                        vehicle.getDoor().getCurrentDoorPosition(area, { position ->
                                            observeDoor(area, position)
                                        }, { error ->
                                            mutableDoorStatus.value =
                                                "도어 수신 실패: ${error.message}"
                                        })
                                    }
                            },
                            { error ->
                                mutableDoorStatus.value = "도어 센서 미지원/권한 오류: ${error.message}"
                            }
                        )
                    }.onFailure { mutableMessage.value = "Vehicle SDK 연결 불가: ${it.message}" }
                }
            }
            if (polling) handler.postDelayed(this, 1000)
        }
    }

    @Synchronized
    private fun observeDoor(area: DoorArea, position: Int?) {
        if (state.value.settings.simulation || !polling) return
        if (position == null) {
            mutableDoorStatus.value = "도어 값 미수신"
            return
        }
        mutableDoorStatus.value = "차량 도어 이벤트 수신 중 · 좌석 지정 없음"
        if (position > 0) {
            openDoors.add(area)
        } else if (openDoors.remove(area)) {
            doorCycle()
        }
    }

    @Synchronized
    fun acceptKm(value: Double, manual: Boolean = true): Boolean {
        if (manual != state.value.settings.simulation) return false
        val previous = mutableKm.value
        if (!value.isFinite() || value < 0 || (previous != null && value < previous)) {
            readingValid = false
            if (!manual) {
                state.value.current?.let {
                    persist(state.value.copy(current = it.copy(interrupted = true)))
                }
            }
            mutableMessage.value = "누적 거리는 감소할 수 없어요. 입력값을 확인해 주세요."
            return false
        }
        val current = state.value.current
        if (current != null &&
            !manual &&
            readingAt != 0L &&
            SystemClock.elapsedRealtime() - readingAt > 5000
        ) {
            persist(state.value.copy(current = current.copy(interrupted = true)))
        }
        if (current != null &&
            !persist(state.value.copy(current = state.value.current!!.copy(endKm = value)))
        ) {
            return false
        }
        mutableKm.value = value
        readingAt = SystemClock.elapsedRealtime()
        readingValid = true
        mutableMessage.value = if (manual) "에뮬레이터 수동 거리 입력" else "Pleos Vehicle SDK 거리 수신"
        return true
    }

    private fun fresh(): Boolean = readingValid &&
        km.value != null &&
        (state.value.settings.simulation || SystemClock.elapsedRealtime() - readingAt <= 5000)

    @Synchronized
    fun start(groupId: String): Boolean {
        if (state.value.current != null) return false
        if (!fresh()) {
            mutableMessage.value = "운행 설정에서 유효한 누적 거리를 먼저 받아 주세요."
            return false
        }
        val group = groups.data.value.groups.find { it.id == groupId } ?: return false
        val settings = state.value.settings
        settings.validate()
        val record =
            TripRecord(
                UUID.randomUUID().toString(),
                group,
                settings,
                System.currentTimeMillis(),
                km.value!!
            )
        if (!persist(state.value.copy(current = record))) return false
        policy = TripBoardingPolicy(settings)
        startPolling()
        return true
    }

    @Synchronized
    fun stop(): Boolean {
        val current = state.value.current ?: return false
        if (!fresh()) {
            mutableMessage.value = "거리 데이터가 오래됐어요. 최신 거리를 받은 뒤 종료해 주세요."
            return false
        }
        append(policy?.tick(SystemClock.elapsedRealtime()).orEmpty())
        if (policy?.pendingIds()?.isNotEmpty() == true) {
            mutableMessage.value = "하차 유예 중입니다. 재연결하거나 유예 시간이 지난 뒤 종료해 주세요."
            return false
        }
        val record = state.value.current!!.copy(endedAt = System.currentTimeMillis())
        TripCalculator.calculate(record)
        if (!persist(
                state.value.copy(current = null, records = state.value.records + record)
            )
        ) {
            return false
        }
        policy = null
        mutableMessage.value = "${current.group.name} 운행을 장부에 저장했어요."
        return true
    }

    @Synchronized
    fun finishInterrupted(): Boolean {
        val current = state.value.current ?: return false
        val record = current.copy(interrupted = true, endedAt = System.currentTimeMillis())
        if (!persist(
                state.value.copy(current = null, records = state.value.records + record)
            )
        ) {
            return false
        }
        policy = null
        mutableMessage.value = "미확인 구간이 있어 정산 보류 상태로 저장했어요."
        return true
    }

    @Synchronized
    private fun append(events: List<RiderEvent>) {
        if (events.isEmpty()) return
        val current = state.value.current ?: return
        persist(state.value.copy(current = current.copy(events = current.events + events)))
    }

    @Synchronized
    fun doorCycle() {
        policy?.doorCycle(SystemClock.elapsedRealtime())
    }

    @Synchronized
    fun signal(id: String, rssi: Int) {
        val current = state.value.current ?: return
        if (current.group.findMember(id) == null || !fresh()) return
        append(policy?.connect(id, rssi, km.value!!, SystemClock.elapsedRealtime()).orEmpty())
    }

    @Synchronized
    fun disconnected(id: String) {
        if (!fresh()) {
            mutableMessage.value = "하차 시점 거리 누락: 최신 거리 수신이 필요합니다."
            state.value.current?.let {
                persist(state.value.copy(current = it.copy(interrupted = true)))
            }
            return
        }
        policy?.disconnect(id, km.value!!, SystemClock.elapsedRealtime())
    }

    @Synchronized
    fun expense(id: String, won: Long): Boolean {
        append(policy?.tick(SystemClock.elapsedRealtime()).orEmpty())
        val current = state.value.current ?: return false
        if (policy?.pendingIds()?.isNotEmpty() == true) {
            mutableMessage.value = "하차 유예가 끝난 뒤 추가 비용을 입력해 주세요."
            return false
        }
        if (won < 0 ||
            !fresh() ||
            current.group.costs.none { it.id == id && it.included && id != "fuel" }
        ) {
            return false
        }
        return persist(
            state.value.copy(
                current = current.copy(
                    expenses =
                    current.expenses +
                        TripExpense(id, won, km.value!!, current.activePassengerIds().toList())
                )
            )
        )
    }
}
