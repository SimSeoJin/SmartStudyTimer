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

## 3. MySQL 실행 (개발 모드 사용하지 않을 때만)

개발 모드(`DEV_MODE=true`)를 사용하면 MySQL이 없어도 됩니다.

**MySQL이 필요할 경우:**
```bash
# Windows
# MySQL을 Windows 서비스로 설치하면 자동 실행
# 또는 MySQL Server 5.7/8.0 설치 후 실행

# macOS (Homebrew)
brew services start mysql

# Linux
sudo systemctl start mysql
```

## 4. 백엔드 실행

```bash
# 백엔드 실행 (IntelliJ IDE 권장)
./gradlew bootRun
```

또는 IntelliJ에서:
1. File → Open → 프로젝트 루트 선택
2. "Run 'bootRun'" 버튼 클릭 또는 Shift+F10

**실행 결과:**
```
Tomcat initialized with port 8080 (http)
o.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed
```

이 메시지가 보이면 성공 ✅

## 5. 모바일 앱 빌드 & 실행

**사전 요구사항:**
- Android Studio 최신 버전
- Android SDK 35 (자동 설치)
- Android 에뮬레이터 또는 실기기

### Android Studio에서 실행:

1. **프로젝트 열기**
   - Android Studio → "Open" → `SmartStudyTimer` 폴더 선택
   - 또는 `SmartStudyTimer/mobile` 폴더 오픈

2. **에뮬레이터 또는 실기기 준비**
   - **에뮬레이터:** Tools → Device Manager → Create Device → Pixel 6 (API 35) 생성 → 시작
   - **실기기:** USB로 연결 후 개발자 모드 활성화

3. **앱 실행**
   - `Run 'app'` 클릭 (또는 Shift+F10)
   - 또는 터미널에서:
     ```bash
     cd mobile
     ./gradlew installDebug
     ```

### 🎯 로그인 화면 테스트

**개발 모드일 때 (DEV_MODE=true):**
```
1. 앱 실행 → 로그인 화면 표시
2. "[개발 모드] 테스트 계정으로 로그인" 텍스트 표시
3. 텍스트 필드에 사용자명 입력 (예: testuser, john, alice)
4. "테스트 로그인" 버튼 클릭
5. → 로그인 완료! 메인 화면으로 이동
```

**실제 카카오 로그인 테스트 (DEV_MODE=false):**
```
1. 텍스트 필드에 카카오 API 키 설정
2. "카카오계정으로 로그인" 버튼 클릭
3. 카카오 로그인 창 표시
4. 카카오 계정으로 로그인
5. → 로그인 완료!
```

## 6. API 테스트 (선택사항)

Postman 또는 curl로 개발 로그인 엔드포인트 테스트:

```bash
# 개발 모드 로그인 테스트
curl -X POST http://localhost:8080/auth/dev-login \
  -H "Content-Type: application/json" \
  -d '{"testUserName": "testuser"}'

# 응답:
# {
#   "accessToken": "eyJhbGc...",
#   "memberId": 1,
#   "name": "testuser"
# }
```

또는 Postman에서:
1. POST 요청
2. URL: `http://localhost:8080/auth/dev-login`
3. Body (JSON):
   ```json
   {
     "testUserName": "testuser"
   }
   ```
4. Send

**성공 응답 (200):**
```json
{
  "accessToken": "eyJhbGc...",
  "memberId": 1,
  "name": "testuser"
}
```

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