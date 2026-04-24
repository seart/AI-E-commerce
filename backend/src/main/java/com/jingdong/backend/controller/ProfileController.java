package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.auth.UserContext;
import com.jingdong.backend.dto.profile.ProfileDtos.UserProfileResponse;
import com.jingdong.backend.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {
  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping("/profile")
  public ApiResponse<UserProfileResponse> profile() {
    return ApiResponse.success(profileService.profile(UserContext.userId()));
  }
}
