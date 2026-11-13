# Domain 계층

Android 및 Retrofit/Room 구현에 의존하지 않는 비즈니스 모델, Repository 인터페이스, 필요한 UseCase를 둡니다.
feature → domain ← data 방향을 유지하고 구현체는 di에서 연결합니다.
현재는 실제 비즈니스 기능이 없어 빈 Repository나 전달만 하는 UseCase를 만들지 않았습니다.
