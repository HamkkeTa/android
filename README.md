# 함께타 (HamkkeTa)

미리 등록한 동승자의 탑승 구간에 따라 차량 운행비를 계산하고 나누는 차량용 카풀 정산 앱입니다.

## 실행 방법

### Android Studio에서 실행

1. 프로젝트를 열고 Gradle Sync를 실행합니다.
2. Device Manager에서 **Pleos Connect v2.1.0** 에뮬레이터를 실행합니다.
3. 실행 모듈을 `app`으로 선택하고 에뮬레이터에 설치·실행합니다.
4. 앱에서 요청하는 근처 기기(블루투스) 권한을 허용합니다.

### APK로 실행

저장소의 [차량 앱 APK](apk/Beyond_HamkkeTa_release.apk)를 내려받아 실행 중인 에뮬레이터에 설치합니다.
아래 명령은 Android SDK의 `platform-tools`가 PATH에 등록된 환경을 기준으로 합니다.

```powershell
adb install -r apk/Beyond_HamkkeTa_release.apk
adb shell am start -n com.example.hamkketa/.MainActivity
```

### 직접 빌드

프로젝트 루트에서 실행합니다. 아래는 Windows PowerShell 기준이며, macOS/Linux에서는 `./gradlew`를 사용합니다.

```powershell
.\gradlew.bat :app:assembleDebug
```

빌드 결과는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

## 테스트 환경 및 필요한 설정

### 개발 환경

- Android Studio, Gradle 실행 JDK **17 이상**
- Android SDK Platform **36.1**, Build-Tools **36.0.0**, Platform-Tools
- SDK 경로는 Android Studio에서 설정하거나 각 개발자의 `local.properties`에 지정합니다.
- 빌드 도구: Gradle **9.3.1**, Android Gradle Plugin **9.1.1**
- 앱 SDK 설정: `minSdk 28`, `targetSdk 36`, `compileSdk 36.1`

### 에뮬레이터에서 정산 테스트

1. 앱의 **운행 설정 · 테스트 · 실제 장부** 화면을 엽니다.
2. 휘발유 연비와 단가를 입력하고 **에뮬레이터 수동**을 눌러 저장합니다.
3. 누적 주행거리(예: `1000 km`)를 입력하고 **누적 거리 반영**을 누릅니다.
4. 그룹 화면으로 돌아가 해당 그룹의 **자동 시작**을 누릅니다.
5. 테스트 화면에서 **도어 열림 → 닫힘 테스트** 후 동승자의 **연결 / 재연결**을 누릅니다.
6. 누적 거리를 증가시키며 중간 탑승·연결 해제·재연결을 시험합니다.
7. **운행 종료** 또는 그룹의 **취소하기**를 누르고 장부에서 구간별 내역과 개인 부담액을 확인합니다.

- 그룹에 등록된 사람을 모두 탑승으로 간주하지 않으며, 탑승 판정을 통과한 사람만 계산에 포함합니다.
- 기본 판정값: RSSI **-65 dBm 이상**, 최근 **60초 이내 도어 개폐**, 연결 해제 후 **30초 유예**입니다. 파일럿용 값이며 실차 보정 전입니다.
- 유예 안에 재연결하면 같은 탑승을 유지합니다. 하차가 확정되면 처음 연결이 끊긴 거리를 기준으로 계산합니다.
- 좌석을 지정하지 않으므로 착석 센서 조건은 제외합니다.
- 유가 API는 미연동이며 연비·단가를 직접 입력합니다. 통행료·주차비 등 추가 비용도 수동 입력합니다.
- 앱 중단이나 거리 누락이 있는 기록은 정산 보류로 표시하고 누적 정산 요청에서 제외합니다.

### 차량 SDK 및 BLE 검증 범위

**차량 SDK** 모드에서는 Pleos Vehicle SDK로 주행거리와 도어 상태를 받습니다. 개발 에뮬레이터에서 누적 거리 `0.0 km`와 객실 도어 닫힘 상태 수신을 확인했습니다. 실제 거리 증가·도어 개폐에 따른 탑승 및 기기 간 BLE 무선 통신은 추가 검증이 필요합니다.

서비스는 **동승자 앱 설치 없음**을 목표로 하지만, 현재 BLE GATT 검증에는 개발용 `passenger-test` 앱이 필요합니다. 일반 휴대폰 페어링만으로 앱 없는 자동 탑승 인식이 구현된 상태는 아닙니다. 카카오톡 자동 발송도 미연동이며 정산 요청 문구 복사까지만 제공합니다.

```powershell
# 개발용 동승자 테스트 앱 빌드
.\gradlew.bat :passenger-test:assembleDebug

# 정산 로직 단위 테스트
.\gradlew.bat :app:testDebugUnitTest
```

개발용 동승자 앱에는 차량 테스트 화면에 표시된 그룹원의 **연결 ID**를 입력합니다. 앱은 전경에서만 동작하며, 에뮬레이터의 BLE 무선 지원이 없으면 차량 앱의 수동 이벤트로 계산 흐름을 확인합니다.

## 개발에 사용한 에뮬레이터 및 화면 규격

| 항목 | 개발·테스트 기준 |
| --- | --- |
| 에뮬레이터 | Pleos Connect **v2.1.0** |
| AVD ID | `Pleos_Connect_v2.1.0` |
| 운영체제 | Android **14 (API 34)** |
| 시스템 이미지 | `android-34 / pleos_car_emulator / x86_64` |
| 전체 화면 해상도 | **2560 × 1440 px** |
| 화면 방향 | 가로(Landscape) |
| 화면 밀도 | **160 dpi** |
| Pleos Vehicle SDK | **2.3.2** |

해상도는 에뮬레이터 전체 화면 기준입니다. 앱은 시스템의 차량 정보 패널과 하단 바를 제외한 영역에 표시됩니다. 주행 제한 상태에서는 조작 화면 대신 탑승 현황 아이콘과 인원수만 표시합니다.

## APK 파일 위치

| 구분 | 경로 |
| --- | --- |
| 저장소에 포함된 차량 앱 APK | [apk/Beyond_HamkkeTa_release.apk](apk/Beyond_HamkkeTa_release.apk) |
| 차량 앱 직접 빌드 결과 | `app/build/outputs/apk/debug/app-debug.apk` |
| 개발용 동승자 앱 빌드 결과 | `passenger-test/build/outputs/apk/debug/passenger-test-debug.apk` |

현재 저장소 APK는 파일명에 `release`가 포함되어 있으나 실제로는 **디버그 빌드**입니다. 배포용 릴리스 서명 APK와 구분해 테스트에 사용합니다. 직접 빌드해도 저장소의 `apk/` 파일이 자동으로 갱신되지는 않습니다.

## 최종 커밋 번호(commit hash)

74567f0
