# 로컬 개발 환경 설정

## 사전 요구사항
- JDK 17 이상
- Android SDK (Android 앱 개발 시)
- MySQL 서버 실행 중

## 개발 모드 사용 (카카오 API 키 없이 테스트)

**가장 간단한 방법:** `local.properties`에서 `DEV_MODE=true`로 설정하면 카카오 API 키 없이 테스트 사용자로 바로 로그인할 수 있습니다.

```properties
DEV_MODE=true
```

이 경우 카카오 애플리케이션 등록이 필요 없습니다.

## 1. 카카오 애플리케이션 등록 (DEV_MODE=false일 때만)

`DEV_MODE=false`로 설정했을 경우에만 필요합니다:

1. [Kakao Developers](https://developers.kakao.com/)에서 로그인
2. 새 애플리케이션 등록
3. **네이티브 앱 키** 발급받기
4. **클라이언트 ID** 복사 (OAuth 2.0 설정에서)

## 2. 로컬 설정 파일 생성

프로젝트 루트에 `local.properties` 파일을 생성합니다:

```bash
cp local.properties.example local.properties
```

그리고 파일을 열어서 값을 수정합니다:

### 개발 모드 사용 (권장)
```properties
# 개발 모드 활성화 (카카오 API 키 필요 없음)
DEV_MODE=true

# 백엔드 서버 주소
API_BASE_URL=http://10.0.2.2:8080/

# 카카오 키는 빈 값으로 둬도 됨
KAKAO_NATIVE_APP_KEY=dummy
```

### 실제 카카오 로그인 테스트
```properties
# 개발 모드 비활성화
DEV_MODE=false

# 네이티브 앱 키 (위에서 발급받은 것)
KAKAO_NATIVE_APP_KEY=your_native_app_key_here

# 백엔드 서버 주소
# - 에뮬레이터에서: http://10.0.2.2:8080/
# - 실기기에서: http://[PC_LAN_IP]:8080/ (같은 Wi-Fi 필요)
API_BASE_URL=http://10.0.2.2:8080/
```

## 3. 백엔드 실행

```bash
# 환경변수 설정 (선택사항 - 기본값 사용 시 생략 가능)
export KAKAO_CLIENT_ID=your_client_id_here
export DB_USERNAME=root
export DB_PASSWORD=

# 백엔드 실행
./gradlew bootRun
```

또는 IDE (IntelliJ, VS Code)의 실행 구성에서 환경변수 설정:
- `KAKAO_CLIENT_ID`: Kakao Developers에서 발급받은 클라이언트 ID
- `DB_USERNAME`: MySQL 계정
- `DB_PASSWORD`: MySQL 비밀번호

## 4. 모바일 앱 빌드 & 실행

Android Studio에서 프로젝트 열기:
1. `mobile` 폴더 선택
2. Android Studio가 자동으로 인식할 때까지 대기
3. `Run 'app'` 실행

앱이 빌드될 때 `local.properties`의 카카오 네이티브 앱 키를 읽어서 자동으로 설정됩니다.

## 5. 테스트

1. 백엔드가 `http://localhost:8080`에서 실행 중인지 확인
2. 모바일 앱에서 카카오 로그인 버튼 클릭
3. 카카오 로그인 창이 뜨면 성공

## 문제 해결

### "KAKAO_NATIVE_APP_KEY를 찾을 수 없습니다" 오류
- `local.properties` 파일이 프로젝트 루트에 있는지 확인
- `KAKAO_NATIVE_APP_KEY` 값이 비어있지 않은지 확인
- Android Studio를 재시작하고 `Invalidate Caches / Restart` 실행

### 백엔드에서 카카오 인증 실패
- `KAKAO_CLIENT_ID` 환경변수가 설정되어 있는지 확인
- Kakao Developers에서 리다이렉트 URI가 `http://localhost:8080/login/oauth2/code/kakao`로 등록되어 있는지 확인

### 실기기에서 네트워크 연결 안 됨
- PC와 실기기가 같은 Wi-Fi 네트워크에 연결되어 있는지 확인
- `API_BASE_URL`을 PC의 실제 LAN IP로 설정 (예: `http://192.168.0.10:8080/`)
- 명령어: `ipconfig` (Windows) 또는 `ifconfig` (Mac/Linux)로 LAN IP 확인