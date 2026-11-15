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
