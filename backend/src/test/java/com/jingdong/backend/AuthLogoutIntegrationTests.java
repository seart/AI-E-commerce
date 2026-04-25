package com.jingdong.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthLogoutIntegrationTests {
  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @BeforeEach
  void resetRateLimits() {
    redisTemplate.delete(List.of(
        "rate:login:13800000000",
        "rate:login-ip:127.0.0.1",
        "rate:login-ip:0:0:0:0:0:0:0:1"
    ));
  }

  @Test
  void logoutBlacklistsCurrentAccessToken() {
    ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", "13800000000", "password", "123456"),
        Map.class
    );
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<String, Object> loginData = data(loginResponse);
    String token = (String) loginData.get("accessToken");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    ResponseEntity<Map> beforeLogout = restTemplate.exchange(
        "/profile",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        Map.class
    );
    assertThat(beforeLogout.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> logoutResponse = restTemplate.exchange(
        "/auth/logout",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        Map.class
    );
    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> afterLogout = restTemplate.exchange(
        "/profile",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        Map.class
    );
    assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void loginAfterLogoutIssuesUsableFreshToken() {
    ResponseEntity<Map> firstLoginResponse = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", "13800000000", "password", "123456"),
        Map.class
    );
    assertThat(firstLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String firstToken = (String) data(firstLoginResponse).get("accessToken");
    HttpHeaders firstHeaders = new HttpHeaders();
    firstHeaders.setBearerAuth(firstToken);
    ResponseEntity<Map> logoutResponse = restTemplate.exchange(
        "/auth/logout",
        HttpMethod.POST,
        new HttpEntity<>(firstHeaders),
        Map.class
    );
    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> secondLoginResponse = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", "13800000000", "password", "123456"),
        Map.class
    );
    assertThat(secondLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String secondToken = (String) data(secondLoginResponse).get("accessToken");
    assertThat(secondToken).isNotEqualTo(firstToken);
    HttpHeaders secondHeaders = new HttpHeaders();
    secondHeaders.setBearerAuth(secondToken);
    ResponseEntity<Map> profileResponse = restTemplate.exchange(
        "/profile",
        HttpMethod.GET,
        new HttpEntity<>(secondHeaders),
        Map.class
    );
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void refreshTokenIssuesNewAccessToken() {
    ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", "13800000000", "password", "123456"),
        Map.class
    );
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String refreshToken = (String) data(loginResponse).get("refreshToken");
    ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(
        "/auth/refresh",
        Map.of("refreshToken", refreshToken),
        Map.class
    );
    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String accessToken = (String) data(refreshResponse).get("accessToken");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<Map> profileResponse = restTemplate.exchange(
        "/profile",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        Map.class
    );
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void refreshTokenCannotAccessBusinessApis() {
    ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", "13800000000", "password", "123456"),
        Map.class
    );
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String refreshToken = (String) data(loginResponse).get("refreshToken");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(refreshToken);
    ResponseEntity<Map> profileResponse = restTemplate.exchange(
        "/profile",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        Map.class
    );
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void profileRequiresAuthentication() {
    ResponseEntity<Map> profileResponse = restTemplate.exchange(
        "/profile",
        HttpMethod.GET,
        HttpEntity.EMPTY,
        Map.class
    );
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> data(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody().get("data");
  }
}
