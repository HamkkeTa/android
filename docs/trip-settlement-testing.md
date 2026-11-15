# 주행거리 정산 개발·검증

작업 브랜치: `feature/trip-distance-settlement`

## 에뮬레이터에서 확인

1. 차량 앱의 `운행 설정 · 테스트 · 실제 장부`를 연다.
2. 휘발유 연비와 단가를 입력하고 `에뮬레이터 수동`을 눌러 저장한다.
3. 누적 거리(예: 1000 km)를 입력하고 반영한다.
4. 그룹으로 돌아가 `자동 시작`을 누른다. 그룹원 전원을 자동 탑승시키지 않는다.
5. `도어 열림 → 닫힘 테스트` 후 동승자의 `연결 / 재연결`을 누른다. RSSI -55는 통과하고 -90은 거부된다.
6. 누적 거리를 증가시키며 다른 동승자를 연결하거나 해제한다. 새 탑승에는 최근 60초 내 도어 개폐가 필요하다.
7. 연결 해제 후 30초 내 재연결하면 같은 탑승이다. 30초가 지나면 처음 끊긴 거리에서 하차한 것으로 계산한다.
8. `운행 종료` 또는 그룹 카드의 `취소하기`로 저장한다. 하차 후보가 남아 있으면 유예 만료를 기다린다.
9. `장부`에서 그룹과 기간을 선택하고 운행 행을 누르면 구간별 내역과 부담액을 확인한다.

통행료·주차비 등은 테스트 화면에서 직접 입력한다. 비용은 입력 거리의 구간 탑승자에게 배분하며 제외된 항목은 청구하지 않는다. 연비·단가·규칙은 운행 도중 변경되지 않는다. 구간 표시 금액은 원 단위 근사치이며 개인 부담액은 전체 정밀 계산 후 10원 단위 차액을 배분한다.

## 실제 Vehicle SDK 입력

`차량 SDK`를 선택하면 Vehicle SDK 2.3.2의 `getCurrentDistanceDriven`을 1초 주기로 읽고 capability 확인 후 도어 상태와 이벤트를 구독한다. 거리 API 값은 km로 사용한다. `CAR_DRIVE`, `CAR_MILEAGE`, `CAR_DOORS` 읽기 권한을 선언한다. 지원 여부와 오류를 화면에 표시하며 수동 테스트 값을 차량 수신값으로 간주하지 않는다.

SDK 거리 값/단위와 실제 증가, 도어 콜백은 대상 에뮬레이터·차량에서 추가 검증해야 한다. 에뮬레이터의 센서 제어 도구가 해당 값을 변경하지 못하면 수동 모드로 계산 흐름을 검증한다. 좌석 대응이 없으므로 착석 센서를 하차 근거로 사용하지 않는다.

주행 제한은 `CarUxRestrictionsManager`의 값을 사용한다. Automotive 환경에서 제한 상태이면 입력 화면 대신 인원 아이콘만 표시하고 기록은 서비스에서 계속한다. Automotive에서 제한 상태를 읽지 못하는 경우에도 조작 화면을 열지 않는다.

## 동승자 테스트 앱

```powershell
.\gradlew.bat :app:assembleDebug :passenger-test:assembleDebug
```

- 차량 APK: `app/build/outputs/apk/debug/app-debug.apk`
- 개발용 동승자 APK: `passenger-test/build/outputs/apk/debug/passenger-test-debug.apk`
- Android 폰의 테스트 앱에 차량 그룹원 `연결 ID`를 입력하고 차량을 검색한다.
- MTU 협상 후 등록 ID를 쓰고 RSSI를 주기적으로 전송한다.
- 앱은 전경 전용이다. 화면에서 나가거나 연결 해제를 누르면 연결을 닫는다.
- 테스트 앱은 운영 서비스의 동승자 앱이 아니다. RSSI는 클라이언트 측 테스트 측정값이며 인증 수단이 아니다.
- 일반 폰에서 앱 없이 동작하려면 차량의 표준 블루투스 연결 API/프로파일 및 다중 연결 지원을 별도로 확인해야 한다.
- 에뮬레이터 간 BLE 무선 연결을 보장하지 않는다. 무선 지원이 없을 때 수동 이벤트 테스트와 BLE 무선 검증을 구분한다.

| 항목 | UUID | 값 |
| --- | --- | --- |
| 서비스 | `6b3f1a52-2c4e-4d8a-9b61-3a0f5e7c9d10` | 광고 UUID |
| 등록 | `6b3f1a53-2c4e-4d8a-9b61-3a0f5e7c9d10` | 그룹원 ID UTF-8, 최대 64바이트 |
| 신호 | `6b3f1a54-2c4e-4d8a-9b61-3a0f5e7c9d10` | RSSI 정수 UTF-8, -127..0 |

## 저장과 정산 보류

기록은 앱 내부 SharedPreferences `trip-records-v1`에 보관한다. 프로세스가 끊긴 운행은 마지막 저장 거리까지만 남기고 정산 보류로 보관한다. 거리 수신이 오래되었거나 역전되면 정상 종료를 막고, 복구 불가능할 때 `미확인 운행 종료 · 정산 보류`를 사용할 수 있다. 보류 기록은 누적 정산 요청에서 제외한다. 카카오톡 자동 발송은 미연동이며 동의한 동승자의 요청 텍스트만 복사한다. 복사를 전송/입금 완료로 기록하지 않는다.

## 참고

### 이번 검증 결과

- 단위 테스트 26개 통과. 문서의 20km 예시(1,100원 / 1,100원 / 550원)와 정차 중 같은 거리에서 비용 입력 후 하차하는 사례를 포함한다.
- Pleos 에뮬레이터: 기존 그룹 관리 4개, 운행·저장·프로세스 복구 2개, 수동 거리 반영 UI 1개 통과.
- 두 앱 APK 빌드, ktlint 및 lint 오류 없음. 기존 라이브러리 버전 등 lint 경고는 남아 있음.
- Vehicle SDK의 `PERF_ODOMETER=0.0` 및 객실 도어 4개 닫힘 값을 실제 수신하고 화면에 표시함.
- 실제 거리 증가, 도어 개폐에 따른 탑승, 두 기기 간 BLE 무선 통신은 아직 검증 전.
- 동승자 테스트 앱의 실행·권한 요청·검색 시작을 확인함.

- [Pleos Vehicle SDK 예제](https://github.com/Pleos-Playground/pleos-sample-vehicle-app)
- [Android BLE GATT 연결](https://developer.android.com/develop/connectivity/bluetooth/ble/connect-gatt-server)
- [블루투스 페어링과 연결](https://developer.android.com/develop/connectivity/bluetooth/find-bluetooth-devices)
- [Android Automotive 주행 제한](https://developer.android.com/training/cars/parked/automotive-os)
