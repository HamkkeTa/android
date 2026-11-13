# HamkkeTa

Kotlin + Jetpack Compose Android 앱. 단일 app 모듈에 MVI Home 예제와 Navigation/Hilt 진입점을 연결했습니다. 아래 라이브러리는 기능 개발을 위한 기반 설정입니다.

## 개발 환경

- Android Studio에서 이 프로젝트를 열고 Gradle Sync를 실행합니다.
- Gradle 실행 JDK: 17 이상. Android Studio의 Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK에서 설정합니다.
- SDK Manager: Android SDK Platform 36.1, Android SDK Build-Tools 36.0.0, Platform-Tools를 설치합니다.
- JVM 바이트코드는 Java/Kotlin 모두 11입니다. Gradle 실행 JDK와 앱의 바이트코드 대상은 별개입니다.
- SDK 경로는 각 개발자의 local.properties에 두며 저장소에 포함하지 않습니다.
- Gradle Wrapper 체크섬 검증은 기존 설정을 유지합니다.

## 적용 스택

| 구분 | 설정 |
|---|---|
| 빌드 | AGP 9.1.1 / Gradle 9.3.1 / Kotlin DSL |
| Kotlin | AGP 내장 Kotlin 2.2.10 / Compose compiler plugin 2.2.10 |
| SDK | compileSdk 36.1 / targetSdk 36 / minSdk 28 |
| 바이트코드 | Java 11 / Kotlin JVM 11 |
| UI | Compose BOM 2026.02.01 / Material 3 |
| AndroidX 기반 | Core 1.17.0 / Activity 1.12.4 / Lifecycle 2.10.0 |
| 화면 이동 | Navigation Compose 2.9.0 의존성 |
| 상태 및 비동기 | Lifecycle ViewModel·Compose 연동 / Coroutines 1.10.1 / StateFlow 사용 가능 |
| DI | Hilt 2.59.2 / AndroidX Hilt 1.3.0 / KSP2 2.3.12 |
| 통신 | Retrofit 2.11.0 / converter-gson / OkHttp 4.12.0 / INTERNET 권한 |
| 저장 | DataStore Preferences 1.1.4 / Room 2.7.1 + KSP compiler |
| 이미지 | Coil Compose 2.7.0 |
| 품질 | ktlint Gradle plugin 14.2.0 / .editorconfig / JUnit / Espresso / Compose UI Test |
| 테스트 도구 | Coroutines Test / MockWebServer |

버전은 gradle/libs.versions.toml에서 관리합니다. 최신 버전 일괄 도입보다 기존 프로젝트 스택과 현재 SDK의 호환성을 우선했습니다.

## 버전 선택 이유

- 이미 구성된 AGP 9.1.1 / Gradle 9.3.1을 유지했습니다. Kotlin Android 플러그인을 별도로 추가하지 않습니다.
- Hilt 2.56 대신 AGP 9를 지원하는 2.59.2를 사용합니다. Hilt 2.60부터의 Kotlin 도구 변경을 함께 도입하지 않았습니다.
- KSP는 이전 Kotlin 버전이 접두사로 붙는 1.x 대신 KSP2를 사용합니다. 2.3.13에는 공식 릴리스 노트에 동시 실행 회귀가 공지되어 있어 2.3.12로 고정했습니다.
- Lifecycle 2.11의 Compose 모듈은 SDK 37 및 AGP 9.2 이상을 요구하므로 2.10.0을 사용합니다. Core와 Activity도 SDK 36 시기에 배포된 1.17.0 / 1.12.4로 맞췄습니다.
- AndroidX Hilt는 SDK 37을 요구하는 1.4 대신 1.3.0을 사용합니다.
- Compose BOM과 Compose compiler 버전은 별도로 관리합니다. compiler plugin 버전은 Kotlin과 맞춥니다.
- Coil은 이전 프로젝트의 2.x API를 유지하면서 2.7.0을 사용합니다.

## PowerShell 명령어

프로젝트 루트에서 실행합니다. 첫 실행은 Gradle 및 Maven 의존성을 다운로드하므로 인터넷 연결이 필요합니다.

~~~powershell
# JDK 및 Gradle 환경 확인
java -version
.\gradlew.bat --version

# 기존 템플릿까지 코드 스타일 자동 정리
.\gradlew.bat ktlintFormat

# 코드 스타일, 로컬 단위 테스트, Android Lint, Debug APK
.\gradlew.bat ktlintCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

# 실행 중인 에뮬레이터 또는 USB 디버깅 기기 필요
.\gradlew.bat :app:connectedDebugAndroidTest

# 기기에 Debug 앱 설치
.\gradlew.bat :app:installDebug
~~~

SDK command-line tools가 PATH에 있는 경우 SDK Manager 대신 다음을 사용할 수 있습니다.

~~~powershell
sdkmanager "platforms;android-36.1" "build-tools;36.0.0" "platform-tools"
sdkmanager --licenses
~~~

## 기능 개발 시 연결할 부분

MainActivity → HamkkeTaNavHost → HomeRoute → HomeScreen을 연결했습니다. HomeViewModel은 Hilt로 생성하며 StateFlow를 생명주기에 맞춰 수집합니다. Home의 이름 입력은 MVI 연결을 보여 주는 작은 예제이며, SavedStateHandle로 화면 재생성 시 복원합니다. 영구 저장 기능은 아닙니다.

- ViewModel: 공통 MviViewModel을 상속하고 UiState / UiIntent를 정의합니다. updateState의 reducer에는 부수 효과를 넣지 않습니다. 입력 처리와 SavedState 복원 예제를 HomeViewModel에 연결했습니다.
- Compose Hilt ViewModel: androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel을 사용합니다.
- Navigation: Home 시작 경로와 NavHost를 연결했습니다. 두 번째 화면을 구현할 때 경로를 추가합니다. 타입 안전한 라우트가 필요하면 Kotlin serialization compiler plugin과 runtime도 함께 추가합니다.
- Retrofit: NetworkModule에서 OkHttpClient와 Gson singleton을 제공합니다. 실제 HTTPS base URL과 API 인터페이스가 정해지면 같은 모듈에 Retrofit provider를 추가합니다. Gson DTO는 @SerializedName으로 서버 필드명을 명시하는 편이 좋습니다. 릴리스 축소를 켤 때 DTO와 실제 API 응답을 함께 검증합니다.
- DataStore: data/local에 preferencesDataStore를 한 번 선언하고 StorageModule에서 DataStore<Preferences> singleton을 제공합니다. 저장할 키와 Repository는 실제 기능에 맞춰 추가합니다.
- Room: Entity / Dao / Database를 만들고 Hilt에서 DB singleton을 제공합니다. 생성되는 app/schemas/*.json은 버전 관리하고 마이그레이션을 검증합니다. 스키마 출력 경로와 compiler는 설정되어 있지만 DB는 아직 없습니다.
- Kakao 로그인: 실제 앱 등록, 패키지명, 키 해시, 네이티브 앱 키, 리다이렉트 스킴 확인 후 SDK와 manifest를 함께 추가합니다. 현재 com.example.hamkketa는 출시 전에 확정할 패키지명입니다.
- Media3: 재생 기능이 정해지면 ExoPlayer/UI를 추가하고 백그라운드 재생이 필요할 때만 MediaSessionService·foreground service 권한을 설정합니다.
- SplashScreen: 현재 플랫폼 기본 시작 화면을 사용합니다. 커스텀 스플래시가 필요하면 core-splashscreen, 시작 테마, postSplashScreenTheme, super.onCreate 이전 installSplashScreen()을 함께 연결합니다.
- 릴리스: 앱 ID·서명 키·백업 정책을 확정하고 R8 축소는 실제 로그인/API/Room 릴리스 동작 검증과 함께 활성화합니다. 서명 파일과 비밀번호는 저장소에 넣지 않습니다.

## 코드 구조와 의존 방향

단일 app 모듈 안에서 다음 패키지를 사용합니다.

~~~text
com.example.hamkketa/
├─ core/mvi/                  # UiState, UiIntent, UiEffect, MviViewModel
├─ data/local/                # DataStore 파일 선언
├─ domain/                    # 비즈니스 모델/Repository/UseCase 확장 위치
├─ di/                        # NetworkModule, StorageModule
├─ feature/home/presentation/ # Contract, ViewModel, Route, Screen
├─ navigation/                # NavHost와 화면 경로
└─ ui/theme/                  # 공통 테마
~~~

- 상태 흐름: HomeIntent → HomeViewModel → 읽기 전용 StateFlow<HomeState> → HomeScreen.
- Route는 ViewModel과 생명주기 수집을 담당하고, Screen은 State와 콜백만 받아 Preview나 UI 테스트에서 독립적으로 사용할 수 있습니다.
- UiEffect는 공통 타입만 준비했습니다. 현재 기능에는 일회성 효과가 없어 전달 채널은 만들지 않았습니다. 결제 결과·완료 여부처럼 유실되면 안 되는 결과는 State로 모델링합니다.
- 도메인 규칙은 feature → domain ← data 방향으로 둡니다. 현재 예제는 UI 로컬 상태만 사용하므로 불필요한 Repository / UseCase를 생성하지 않았습니다.
- domain/README.md에 계층 원칙을 남겼습니다. 공통 UI가 생기면 ui/components로 추출하고 실제 모듈 경계가 생길 때 멀티 모듈로 분리합니다.
- HomeViewModelTest는 입력 전 상태 불변성과 새 SavedStateHandle을 통한 ViewModel 복원을 검사합니다. Android 프로세스 종료/재시작을 실제로 실행하는 기기 테스트는 아닙니다.
- BuildConfig는 아직 참조하는 코드가 없어 생성 기능을 추가하지 않았습니다. 실제 API URL을 Gradle 설정에서 전달할 때 활성화하면 됩니다.
-  MVI 기반: 공통 State·Intent·Effect 타입과 ViewModel
- Home 화면 분리: Contract·ViewModel·Route·Screen
- Navigation 연결: MainActivity → NavHost → Home 
- 상태 관리: Hilt ViewModel·StateFlow·생명주기 수집·SavedStateHandle 복원 
- 저장소 DI: DataStore singleton 제공 
- 통신 DI: OkHttp·Gson singleton 제공 
- 단위 테스트: 상태 불변성과 저장 상태 복원 검사 
- 계층 구조: core·data·domain·di·feature·navigation 구성 

## 검증 상태

설정 파일의 참조와 Hilt manifest 연결을 정적으로 확인했습니다. Home MVI 및 DI 기반 코드와 상태 복원 단위 테스트를 추가했습니다. 이 작업 환경에서는 Gradle 9.3.1 배포 파일 다운로드가 java.net.SocketException: Permission denied로 실패했습니다. 따라서 Gradle Sync, 컴파일, ktlint 자동 정리, 단위 테스트, Lint, 기기 테스트는 통과 여부를 확인하지 못했습니다. 위 명령어를 네트워크가 허용된 로컬 터미널에서 실행해야 합니다.

## 공식 참고 문서

- [AGP 9.1 호환성](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [내장 Kotlin 설정](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Hilt AGP 9 지원 릴리스](https://github.com/google/dagger/releases/tag/dagger-2.59.2)
- [Lifecycle 릴리스 및 SDK 요구 사항](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- [AndroidX Hilt 릴리스](https://developer.android.com/jetpack/androidx/releases/hilt)
- [KSP 릴리스 및 알려진 문제](https://github.com/google/ksp/releases)
- [ktlint Gradle 플러그인](https://github.com/JLLeitschuh/ktlint-gradle)
