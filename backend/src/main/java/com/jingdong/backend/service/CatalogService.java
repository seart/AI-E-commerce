package com.jingdong.backend.service;

import com.jingdong.backend.dto.home.HomeDtos.HomeResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantDetailResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantResponse;
import com.jingdong.backend.store.DatabaseStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
  // 用户端目录业务层，提供首页、商家和商品浏览数据。
  private final DatabaseStore store;

  public CatalogService(DatabaseStore store) {
    this.store = store;
  }

  public HomeResponse home() {
    return store.home();
  }

  public List<MerchantResponse> searchMerchants(String keyword) {
    return store.searchMerchants(keyword);
  }

  public MerchantDetailResponse merchantDetail(String merchantId) {
    return store.merchantDetail(merchantId);
  }
}
