package com.jingdong.backend.service;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.profile.ProfileDtos.UserProfileResponse;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.store.DatabaseStore;
import com.jingdong.backend.store.DatabaseStore.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  // 个人中心业务层，返回当前用户基础信息和统计数据。
  private final DatabaseStore store;

  public ProfileService(DatabaseStore store) {
    this.store = store;
  }

  public UserProfileResponse profile(String userId) {
    UserRecord user = store.findUserById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    return new UserProfileResponse(
        user.id(),
        user.nickname(),
        user.mobile(),
        user.nickname().substring(0, 1),
        user.memberLevel(),
        user.profileStats()
    );
  }
}
