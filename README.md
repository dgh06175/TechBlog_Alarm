# Tech Blog Notification

기술 블로그 새로운 글 알림

매일 아침 8시~8시30분 사이에 아래 블로그중 새로운 게시글이 올라왔을 경우 Issues 를 발생시켜 알림을 보냅니다.

[Toss](https://toss.tech)

[Woowahan](https://techblog.woowahan.com)

[Kakao](https://tech.kakao.com/blog)

`iOS 17.0+`

`SwiftUI` / `AWS` / `Java Spring`

## 비즈니스 요구사항

사용자는 관심 있는 기술 블로그의 게시글 업데이트 소식을 편리하게 접할 수 있다.

기술 블로그의 게시글중 원하는 키워드를 검색하거나 필터링 해서 게시글을 파악할 수 있다.

## 기능 요구사항

> MVP

### iOS APP

- [ ] 블로그들의 최신 게시글의 정보를 표시한다.
  - 블로그 이름
  - 업로드 날짜
  - 게시글 제목
- [ ] 클릭 시 해당 게시글의 웹뷰로 이동한다.
- [ ] 새로운 게시글이 올라왔을 경우 익일 사용자가 정한 시간에 알림 전송 (백그라운드)

### 스크래핑 스크립트

- [ ] 매일 새벽 5시에 새로운 게시글 DB 업데이트
  - [ ] 웹 스크래핑
    - [ ] 카카오
    - [ ] 토스
    - [ ] 우아한형제들
  - [ ] 스크래핑 실패 시 재시도
  - [ ] 스크래핑 로직 모니터링 및 로그 수집
  - [ ] AWS EC2 - MySQL 에 저장

### Rest API & DB

- [ ] 사용자가 앱을 실행할 경우 최근 게시글 정보 불러오기
  - [ ] 서버의 DB에서 앱으로 데이터 가져오기
    - 페이지네이션 지원 (10개씩)

<details>
<summary>추가 기능</summary>
<div markdown="1">

### Notifications

- [ ] 사용자가 지정한 알림 시간에 새로운 게시글 올라왔을 경우 앱 알림 전송
  - [ ] AWS SNS 설정
    - [ ] 토픽 생성 및 구독 추가
      - 모바일 푸시 노티피케이션을 위한 엔드포인트 사용
      - 구독 엔드포인트로는 앱의 디바이스 토큰 사용

### Auth

- [ ] 사용자 인증
  - [ ] 회원가입 / 로그인
  - [ ] 프로필 관리
  - [ ] 사용자별 알람 설정 저장

### Features

- [ ] 북마크
    - [ ] 게시글 북마크 가능
    - [ ] 북마크 화면에서 북마크한 게시글 모아 볼 수 있다.
    - [ ] 북마크한 게시글은 간단한 메모 가능

- [ ] 조회한 게시글 표시

- [ ] 게시글 키워드 태그 추가
  - [ ] 태그 있는 블로그는 스크래핑 하기
    - [ ] 없는 블로그는 표시 안하기
    - [ ] 또는 CreateML 로 키워드 추출하기

- [ ] 블로그 대표 아이콘 추가

- [ ] 전체 유저의 조회수 또는 북마크한 횟수 보이도록 추가

- [ ] 게시글 검색
    - [ ] 제목
    - [ ] 태그

- [ ] 블로그 종류 추가
  - [ ] 블로그 필터링 기능

</div>
</details>

## 서버 아키텍쳐

<img src="https://drive.google.com/uc?export=download&id=1uiO_O5tHSMn_gWIUN4yRNU0bA7DDQAgT"/>

### AWS Lambda (Web Scrapping Script)

매일 정해진 시간에 실행되어 카카오, 토스, 우아한형제들의 기술 블로그에서 최신 게시글을 스크래핑한다.

스크래핑한 데이터를 AWS EC2에 배포된 Spring Boot 애플리케이션으로 전송

### AWS EC2 (Spring Boot REST API)

사용자 요청을 처리하고 데이터베이스와 상호작용하여 필요한 데이터 제공

AWS Lambda로부터 받은 스크래핑 데이터를 처리하여 AWS RDS(MySQL)에 저장
iOS 앱으로부터 API 요청을 받아서 AWS RDS(MySQL)에서 데이터를 조회하고 응답을 반환한다.

### AWS RDS (MySQL)

스크래핑된 게시글 데이터를 저장하는 데이터베이스.

Spring Boot 애플리케이션이 데이터베이스와 상호작용하여 데이터를 읽고 쓴다.

### iOS App

사용자 인터페이스를 제공하여 사용자가 최신 게시글을 조회할 수 있도록 한다.
AWS EC2에 배포된 Spring Boot 애플리케이션의 REST API를 호출하여 최신 게시글 데이터를 받아온다.

## 프로그래밍 요구사항

- 간소화
- 기능 분리 및 로직 추상화하여 유지보수 시에 변경하는 부분 최소화 하기

## API 설계

- GET /posts: 최신 게시글 불러오기
- 프로토콜 및 포맷 결정
- 테스트 코드 작성
  - 스크래핑 로직 수정 했을때 제대로 된건지 한번에 테스트 가능하도록
  - 핵심 로직 제대로 작동 하는지 테스트

## 데이터베이스 설계

### 데이터 모델링

- 데이터베이스: MySQL
- 스키마 설계
```sql
CREATE TABLE posts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    link VARCHAR(2048) NOT NULL,
    blog_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL
);
```

## 인프라 및 배포 계획

- 호스팅 환경
  - AWS
- 서버 및 네트워크 구성
- 배포 전략
  - CI/CD

## 로그 및 모니터링 계획

- 알림 시스템
  - 스크래핑 로직에 오류가 발생했을 경우 관리자 화면에 표시되고 알림 오도록 구성
