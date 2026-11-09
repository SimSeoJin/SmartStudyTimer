# EC2 테스트 서버 배포

백엔드와 MySQL을 EC2 한 대에 Docker Compose로 올려서, 실기기가 같은 Wi-Fi가 아니어도 접속할 수 있게 함.
테스트 서버 용도이며 HTTPS·백업은 아직 없음.

## 0. AWS 계정 준비
- Billing → Budgets에서 월 예산 알림 설정 (예: $5)
- 루트 계정 MFA 활성화
- Billing → Credits에서 크레딧 잔액과 만료일 확인

## 1. EC2 생성
| 항목 | 값 |
|---|---|
| 리전 | 서울 `ap-northeast-2` |
| AMI | Ubuntu Server 24.04 LTS |
| 인스턴스 유형 | `t3.small` (2GB, Spring + MySQL을 같이 돌리려면 최소 이 정도) |
| 스토리지 | 20GB gp3 |
| 키 페어 | 새로 생성 후 `.pem` 보관 |
| 보안 그룹 | SSH 22 → 내 IP, TCP 8080 → 0.0.0.0/0 |

생성 후 **탄력적 IP**를 할당해 인스턴스에 연결 (재시작해도 주소 유지).

## 2. 서버 접속
```bash
ssh -i <키파일>.pem ubuntu@<탄력적IP>
```

## 3. 서버 초기 설정 (최초 1회)
```bash
# 메모리 부족 대비 스왑 2GB
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker 설치
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit   # 다시 접속해야 docker 그룹이 적용됨
```

## 4. 배포 (최초 1회)
```bash
git clone https://github.com/SimSeoJin/SmartStudyTimer.git
cd SmartStudyTimer
git checkout dev

cp .env.example .env
openssl rand -base64 48   # 출력값을 JWT_SECRET에 넣기
nano .env                 # DB 비밀번호, JWT_SECRET, 소셜 로그인 키 입력

docker compose up -d --build
docker compose logs -f backend   # "Started ..." 로그가 나오면 성공 (Ctrl+C로 빠져나옴)
```

## 5. 앱 연결
PC의 `mobile/local.properties`:
```properties
API_BASE_URL=http://<탄력적IP>:8080/
```
**debug 빌드**로 설치 (http 허용 설정이 debug에만 있음).

## 코드 업데이트
```bash
cd SmartStudyTimer
git pull
docker compose up -d --build
```

## 자주 쓰는 명령
```bash
docker compose ps                 # 상태 확인
docker compose logs -f backend    # 백엔드 로그
docker compose restart backend    # 백엔드만 재시작
docker compose down               # 중지 (DB 데이터는 볼륨에 유지)
docker compose down -v            # 중지 + DB 데이터 삭제 (주의)
```

## 비용 관리
- 한동안 안 쓸 때는 EC2 콘솔에서 인스턴스 **중지** (재시작 시 `restart: unless-stopped`로 컨테이너 자동 실행)
- 중지 중에도 디스크(EBS)와 탄력적 IP는 소액 과금됨

## 다음 단계
- 도메인 + HTTPS (Caddy 등) → release 빌드 지원
- GitHub Actions로 push 시 자동 배포
