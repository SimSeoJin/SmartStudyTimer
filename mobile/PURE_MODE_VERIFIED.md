# 순공모드 검증본 반영 (2026-10-24)

## 범위

`dev`의 로그인(카카오·네이버·구글), 네트워크/API, 백엔드, 데이터 저장소와 다른 화면은 유지하고, 사용자 실사용 확인을 마친 순공모드 소스를 반영한다. 전역 테마도 `dev` 그대로 유지한다.

- 손바닥: 수동 정지 / 주먹: 시작·재개. 수동 정지는 얼굴 복귀로 해제하지 않는다.
- 모델 점수 0.55 이상인 같은 제스처를 연속 2회 확인하고, 정지·재개 공통 1.5초 쿨다운을 적용한다.
- 얼굴 미감지는 설정된 3/5/10초 유예 동안 타이머를 유지한다. 복귀하면 연속 미감지 시간을 초기화한다.
- 새 유효 제스처는 얼굴 판정보다 우선하며 이탈 유예를 초기화한다. 주먹을 유지하면 쿨다운 이후 명령이 재발행될 수 있는 기준본 동작을 유지한다. 중복·역순·오래된 결과는 새 명령이 아니다.
- 카메라 숨기기는 프리뷰만 숨긴다. 분석·타이머 판정은 알림이 있는 카메라 포그라운드 서비스가 소유한다.
- 저조도 보정, 부분 얼굴 검출, 제스처 ON/OFF·정확도/절약 설정, 화면 재진입 시 세션 정리와 카메라 복구를 함께 포함한다.
- 타이머 글자를 실제 가용 너비에 맞춰 한 줄로 표시한다.

더 이상 사용하지 않는 옛 `CameraPreview.kt`와 그 파일의 중복 판정기를 제거하고, 서비스와 화면에서 같은 판정기를 사용한다. 설정 화면의 도움말도 현재 제스처 방향으로 수정한다.

## 검증

반영 원본은 Debug 빌드, JVM 테스트 139개, 실제 모델 계측 테스트 11개(제스처 3·얼굴 파이프라인 8)를 통과했고 사용자가 실제 동작을 확인했다. `dev`에 옮긴 뒤의 별도 검증 결과는 PR 본문에 기록한다. 로그인과 서버 연동은 해당 프로젝트의 기존 키·서버 설정이 필요하며 이 변경에서 실서비스 인증을 검증한 것은 아니다.

Android Studio에서는 `mobile`을 연다. 프로젝트 루트에서 실행하는 경우:

```powershell
./gradlew.bat -p mobile :app:assembleDebug :app:testDebugUnitTest
./gradlew.bat -p mobile :app:connectedPureModeDiagnosticAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sm.myapplication.vision.GestureModelInstrumentedTest,com.sm.myapplication.vision.FacePipelineInstrumentedTest
```

계측 시험은 `.diagnostic` 앱 ID를 사용하므로 일반 앱의 기록을 덮어쓰지 않는다. APK·빌드 산출물·로컬 설정·API 키는 PR에 포함하지 않는다. 얼굴/손 시험 이미지의 출처는 `app/src/androidTest/assets/README.md`, 추가 얼굴 모델의 고지는 `app/src/main/assets/blaze_face_short_range.NOTICE`에 있다.
