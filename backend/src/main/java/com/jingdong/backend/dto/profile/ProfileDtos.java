package com.jingdong.backend.dto.profile;

public final class ProfileDtos {
  private ProfileDtos() {}

  public record UserProfileStatsResponse(
      int redPackets,
      int coupons,
      int points,
      int credit
  ) {}

  public record UserProfileResponse(
      String id,
      String nickname,
      String mobile,
      String avatarText,
      String memberLevel,
      UserProfileStatsResponse stats
  ) {}
}
