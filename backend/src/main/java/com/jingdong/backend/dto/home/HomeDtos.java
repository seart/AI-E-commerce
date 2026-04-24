package com.jingdong.backend.dto.home;

import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantResponse;
import java.util.List;

public final class HomeDtos {
  private HomeDtos() {}

  public record BannerResponse(
      String id,
      String title,
      String subtitle,
      String background
  ) {}

  public record CategoryResponse(
      String id,
      String name,
      String icon
  ) {}

  public record HomeResponse(
      List<BannerResponse> banners,
      List<CategoryResponse> categories,
      List<MerchantResponse> featuredMerchants
  ) {}
}
