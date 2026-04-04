# Pickify

> 인스타그램 매거진에서 추천하는 트렌디한 장소들을 지도 위에 시각화하여 Z세대 사용자들이 자신의 취향에 맞는 플레이스를 쉽게 탐색하고 저장할 수 있도록 돕는 위치 기반 큐레이션 서비스입니다.

- **개발 기간**: 2025.01.05 ~ 2025.02.23
- **팀 구성**: 기획/디자인 1명, FE 3명, BE 4명
- **배포 URL**: https://pickify.froz.cloud

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.3.5, Spring Security, Spring Data JPA |
| Database | MySQL 8.0, Redis |
| Auth | JWT (JJWT), OAuth 2.0 (Kakao) |
| Infra | AWS EC2, Docker, GitHub Actions CI/CD |

## 담당 역할

**백엔드 팀장** (BE 4명 중 1명)

### 1. JWT 기반 인증/인가 시스템 구현

Spring Security와 JJWT를 활용한 stateless 인증 시스템 설계 및 구현

- **토큰 구조**: Access Token (5시간) / Refresh Token (7일) / Email Token (5분)
- **보안 설정**: HttpOnly, Secure, SameSite=None 쿠키 정책 적용
- **토큰 저장**: Refresh Token은 Redis에 저장하여 탈취 시 무효화 가능하도록 설계
- **커스텀 필터**: `JwtAuthFilter`, `CustomLoginFilter` 구현으로 인증 흐름 제어

### 2. 카카오 OAuth 2.0 소셜 로그인 구현

Spring Security OAuth2 Client를 활용한 카카오 소셜 로그인

- `CustomOAuth2UserService`: 카카오 사용자 정보 처리 및 자동 회원가입
- `OAuth2SuccessHandler`: 로그인 성공 시 JWT 발급 및 쿠키 설정
- `CustomAuthorizationRequestResolver`: OAuth2 인증 요청 커스터마이징

### 3. 이메일 인증 시스템 구현

회원가입 전 이메일 소유권 검증을 위한 인증 코드 시스템

- **비동기 처리**: `@Async`와 `ThreadPoolTaskExecutor`로 이메일 발송 병렬 처리
- **템플릿 엔진**: Thymeleaf를 활용한 HTML 이메일 템플릿 렌더링
- **인증 코드 관리**: Redis에 6자리 코드 저장 (TTL 5분), 검증 후 Email JWT 발급

### 4. Redis 캐싱 시스템 구현

자주 조회되는 데이터의 응답 속도 개선을 위한 캐싱 적용

- **캐싱 대상**: 카테고리, 매거진, 플레이스, 사용자 저장 플레이스 목록
- **캐시 전략**: `@Cacheable` 어노테이션 기반, TTL 5초 설정
- **직렬화**: `GenericJackson2JsonRedisSerializer`로 JSON 직렬화

### 5. 구현 API 목록 (7개)

| API | 설명 |
|-----|------|
| `POST /auth/login` | 이메일/비밀번호 기반 로그인 |
| `GET /auth/oauth2/kakao` | 카카오 소셜 로그인 |
| `POST /users/signup` | 회원가입 (Email JWT 필요) |
| `POST /email-auth/send` | 이메일 인증코드 발송 |
| `POST /email-auth/verify` | 인증코드 검증 및 Email JWT 발급 |
| `POST /auth/logout` | 로그아웃 (Refresh Token 삭제) |
| `POST /auth/reissue` | Access Token 재발급 |

## 프로젝트 구조

```
src/main/java/com/pickyfy/pickyfy/
├── auth/
│   ├── filter/          # JwtAuthFilter, CustomLoginFilter
│   ├── handler/         # OAuth2 Success/Failure Handler
│   └── oauth2/          # CustomOAuth2UserService
├── common/
│   ├── config/          # Security, Redis, Mail Config
│   └── util/            # JwtUtil, RedisUtil
├── service/
│   └── EmailServiceImpl # 이메일 인증 서비스
└── web/
    └── controller/      # AsyncEmailService
```
