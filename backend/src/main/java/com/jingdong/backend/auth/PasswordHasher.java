package com.jingdong.backend.auth;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PasswordHasher {
  // 密码使用 PBKDF2 存储，格式为 {pbkdf2}迭代次数:盐:哈希。
  private static final String PREFIX = "{pbkdf2}";
  private static final int ITERATIONS = 120_000;
  private static final int KEY_LENGTH = 256;
  private final SecureRandom secureRandom = new SecureRandom();

  public String hash(String password) {
    byte[] salt = new byte[16];
    secureRandom.nextBytes(salt);
    byte[] hash = pbkdf2(password, salt, ITERATIONS);
    return PREFIX + ITERATIONS + ":"
        + Base64.getEncoder().encodeToString(salt) + ":"
        + Base64.getEncoder().encodeToString(hash);
  }

  public boolean matches(String rawPassword, String storedPassword) {
    if (!isHashed(storedPassword)) {
      // 兼容历史明文密码，登录成功后 AuthService 会自动升级为哈希。
      return rawPassword.equals(storedPassword);
    }
    String[] parts = storedPassword.substring(PREFIX.length()).split(":");
    int iterations = Integer.parseInt(parts[0]);
    byte[] salt = Base64.getDecoder().decode(parts[1]);
    byte[] expected = Base64.getDecoder().decode(parts[2]);
    byte[] actual = pbkdf2(rawPassword, salt, iterations);
    // 哈希比较使用常量时间比较，避免根据比较耗时泄露密码信息。
    if (expected.length != actual.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < expected.length; i++) {
      result |= expected[i] ^ actual[i];
    }
    return result == 0;
  }

  public boolean isHashed(String password) {
    return password != null && password.startsWith(PREFIX);
  }

  private byte[] pbkdf2(String password, byte[] salt, int iterations) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to hash password", exception);
    }
  }
}
