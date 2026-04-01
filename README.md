<br>

# 🌐 Lilling

**🗓️ 기간:** 2025.10.25 ~　　|　　**👤 유형:** 개인 프로젝트　　|　　**🔗 상태:** 기능 확장중

Little Linking의 의미를 담고 있는 Lilling은 URL Shortener 웹 서비스입니다.    
[「가상 면접 사례로 배우는 대규모 시스템 설계 기초」](https://product.kyobobook.co.kr/detail/S000001033116) 책의 8장 「URL 단축기 설계」 챕터에서 접한 아이디어를 출발점으로, URL 단축 서비스의 핵심 설계 요소를 실제로 구현하고, 사용자 관점의 기능 제공은 물론 성능과 확장성을 고려한 기술적 고민, 배포와 운영까지 포함한 하나의 서비스로 발전시키는 것을 목표로 개발하고 있습니다.

<br>

## 🔗 바로가기

- 🌍 **서비스 바로가기** → [https://www.lill.ing](https://www.lill.ing)
- ⚙️ **Backend Repository** → [https://github.com/LeeEuyJoon/lilling-be](https://github.com/LeeEuyJoon/lilling-be)
- 🧩 **KGS Repository** → [https://github.com/LeeEuyJoon/lilling-kgs](https://github.com/LeeEuyJoon/lilling-kgs)
- 💻 **Frontend Repository** → [https://github.com/LeeEuyJoon/lilling-fe](https://github.com/LeeEuyJoon/lilling-fe)

<br>

## 주요 화면

<table border="0">
  <tr>
    <td>
<img width="100%" height="1180" alt="스크린샷 2026-03-02 오후 6 51 06" src="https://github.com/user-attachments/assets/be632861-26a2-4c20-98c0-8d5af3bce988" />
    </td>
    <td>
      <img width="100%" height="1180" alt="스크린샷 2026-03-02 오후 6 51 01" src="https://github.com/user-attachments/assets/77a7c351-87f3-465d-a73e-514cddd4dbec" />
    </td>
    <td>
      <img width="100%" height="1180" alt="스크린샷 2026-03-02 오후 6 53 01" src="https://github.com/user-attachments/assets/ab81bd27-170e-4697-b868-b391dc8ece25" />
    </td>
        <td>

<img width="1822" height="1180" alt="스크린샷 2026-03-02 오후 6 52 34" src="https://github.com/user-attachments/assets/5cb8a003-31b3-4cd2-9d33-9e904097f2f2" />
    </td>
  </tr>
</table>



<br>

# 📑 문서


## 📘 Ⅰ. 시스템 개요
- [프로젝트 목표](https://github.com/LeeEuyJoon/lilling-be/wiki/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EB%AA%A9%ED%91%9C)
- [계략적 추정치](https://github.com/LeeEuyJoon/lilling-be/wiki/%EA%B3%84%EB%9E%B5%EC%A0%81-%EC%B6%94%EC%A0%95%EC%B9%98)
- [기술 스택](https://github.com/LeeEuyJoon/lilling-be/wiki/%EA%B8%B0%EC%88%A0-%EC%8A%A4%ED%83%9D)
- [도메인 및 라우팅 구조](https://github.com/LeeEuyJoon/lilling-be/wiki/%EB%8F%84%EB%A9%94%EC%9D%B8-%EB%B0%8F-%EB%9D%BC%EC%9A%B0%ED%8C%85-%EA%B5%AC%EC%A1%B0)
- [백엔드 아키텍처](https://github.com/LeeEuyJoon/lilling-be/wiki/%EB%B0%B1%EC%97%94%EB%93%9C-%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98)

## 📙 Ⅱ. 핵심 로직 설계
- [URL 단축 방식](https://github.com/LeeEuyJoon/lilling-be/wiki/URL-%EB%8B%A8%EC%B6%95-%EB%B0%A9%EC%8B%9D)
- [리다이렉트 처리 설계](https://github.com/LeeEuyJoon/lilling-be/wiki/%EB%A6%AC%EB%8B%A4%EC%9D%B4%EB%A0%89%ED%8A%B8-%EC%B2%98%EB%A6%AC-%EC%84%A4%EA%B3%84-%EB%AC%B8%EC%84%9C)

## 📗 Ⅲ. KGS (Key Generation Service)
- [KGS 설계](https://github.com/LeeEuyJoon/lilling-be/wiki/KGS-(Key-Generation-Service)-%EC%84%A4%EA%B3%84)
- [KGS 키 할당 테스트](https://github.com/LeeEuyJoon/lilling-be/wiki/KGS-%ED%82%A4-%ED%95%A0%EB%8B%B9-%ED%85%8C%EC%8A%A4%ED%8A%B8)

<br>

# 🆕 기능 업데이트 히스토리

**2026-04-02**
- 모바일 최적화: 반응형 디자인 적용

**2026-03-28**
- MyURLs 태그 기능 추가
- MyURLs 태그 기반 필터링 기능 추가

**2026-02-28**
- Keyword 기반 단축 기능 추가: 커스텀 short url 생성

**2026-01-27**
- URL 분석 기능 추가: 시간별 / 일별 / 주별 / 월별 통계

**2026-01-22**
- 미니 차트 기능 추가: 최근 일자별 클릭 수 추이 모니터링

**2026-01-09**
- Add Existing 기능 추가: 주인 없는 short url을 클레임 (회원가입 전 단축한 url 관리를 위한 등록)

**2026-01-06**
- 로그인 기능 추가: 구글 소셜 로그인

**2025-11-04**
- 리다이렉트 성능 개선

**2025-10-27**
- 출시: 기본 short url 생성 기능 제공

<br>

<!--

## 📕 Ⅳ. 테스트 및 검증
- [유닛 & 속성 기반 테스트](https://github.com/LeeEuyJoon/lilling-be/wiki/%EC%9C%A0%EB%8B%9B-&-%EC%86%8D%EC%84%B1-%EA%B8%B0%EB%B0%98-%ED%85%8C%EC%8A%A4%ED%8A%B8)
- [통합 테스트](https://github.com/LeeEuyJoon/lilling-be/wiki/%ED%86%B5%ED%95%A9-%ED%85%8C%EC%8A%A4%ED%8A%B8)

-->

<br>

---

📝 **관련 작성 글**

- [고유하면서도 랜덤한 7자리 인코딩을 찾아가는 여정 (Base62, 선형식, XORShift, Feistel Cipher)](https://luti-dev.tistory.com/21)
- [Spring Boot 로깅의 표준 Logback과 SLF4J 알아보기](https://luti-dev.tistory.com/23)
