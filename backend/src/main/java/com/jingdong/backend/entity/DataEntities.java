package com.jingdong.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class DataEntities {
  private DataEntities() {}

  @TableName("users")
  public static class UserEntity {
    @TableId
    private String id;
    private String mobile;
    private String password;
    private String nickname;
    private String memberLevel;
    private String role;
    private String status;
    private Integer couponCount;
    private Integer favoriteCount;
    private Integer points;
    private Integer growthValue;
    private LocalDateTime lastLoginAt;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getMobile() {
      return mobile;
    }

    public void setMobile(String mobile) {
      this.mobile = mobile;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getNickname() {
      return nickname;
    }

    public void setNickname(String nickname) {
      this.nickname = nickname;
    }

    public String getMemberLevel() {
      return memberLevel;
    }

    public void setMemberLevel(String memberLevel) {
      this.memberLevel = memberLevel;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Integer getCouponCount() {
      return couponCount;
    }

    public void setCouponCount(Integer couponCount) {
      this.couponCount = couponCount;
    }

    public Integer getFavoriteCount() {
      return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
      this.favoriteCount = favoriteCount;
    }

    public Integer getPoints() {
      return points;
    }

    public void setPoints(Integer points) {
      this.points = points;
    }

    public Integer getGrowthValue() {
      return growthValue;
    }

    public void setGrowthValue(Integer growthValue) {
      this.growthValue = growthValue;
    }

    public LocalDateTime getLastLoginAt() {
      return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
      this.lastLoginAt = lastLoginAt;
    }
  }

  @TableName("banners")
  public static class BannerEntity {
    @TableId
    private String id;
    private String title;
    private String subtitle;
    private String background;
    private Integer sortOrder;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getSubtitle() {
      return subtitle;
    }

    public void setSubtitle(String subtitle) {
      this.subtitle = subtitle;
    }

    public String getBackground() {
      return background;
    }

    public void setBackground(String background) {
      this.background = background;
    }

    public Integer getSortOrder() {
      return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
    }
  }

  @TableName("categories")
  public static class CategoryEntity {
    @TableId
    private String id;
    private String name;
    private String icon;
    private Integer sortOrder;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getIcon() {
      return icon;
    }

    public void setIcon(String icon) {
      this.icon = icon;
    }

    public Integer getSortOrder() {
      return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
    }
  }

  @TableName("merchants")
  public static class MerchantEntity {
    @TableId
    private String id;
    private String name;
    private Integer sales;
    private BigDecimal minOrderPrice;
    private BigDecimal deliveryFee;
    private Integer deliveryMinutes;
    private String tagsJson;
    private String description;
    private String notice;
    private BigDecimal rating;
    private String logoBackground;
    private String logoText;
    private String status;
    private Integer sortOrder;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getSales() {
      return sales;
    }

    public void setSales(Integer sales) {
      this.sales = sales;
    }

    public BigDecimal getMinOrderPrice() {
      return minOrderPrice;
    }

    public void setMinOrderPrice(BigDecimal minOrderPrice) {
      this.minOrderPrice = minOrderPrice;
    }

    public BigDecimal getDeliveryFee() {
      return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
      this.deliveryFee = deliveryFee;
    }

    public Integer getDeliveryMinutes() {
      return deliveryMinutes;
    }

    public void setDeliveryMinutes(Integer deliveryMinutes) {
      this.deliveryMinutes = deliveryMinutes;
    }

    public String getTagsJson() {
      return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
      this.tagsJson = tagsJson;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getNotice() {
      return notice;
    }

    public void setNotice(String notice) {
      this.notice = notice;
    }

    public BigDecimal getRating() {
      return rating;
    }

    public void setRating(BigDecimal rating) {
      this.rating = rating;
    }

    public String getLogoBackground() {
      return logoBackground;
    }

    public void setLogoBackground(String logoBackground) {
      this.logoBackground = logoBackground;
    }

    public String getLogoText() {
      return logoText;
    }

    public void setLogoText(String logoText) {
      this.logoText = logoText;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Integer getSortOrder() {
      return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
    }
  }

  @TableName("merchant_categories")
  public static class MerchantCategoryEntity {
    @TableId
    private String id;
    private String merchantId;
    private String categoryId;
    private String name;
    private Integer sortOrder;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getMerchantId() {
      return merchantId;
    }

    public void setMerchantId(String merchantId) {
      this.merchantId = merchantId;
    }

    public String getCategoryId() {
      return categoryId;
    }

    public void setCategoryId(String categoryId) {
      this.categoryId = categoryId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getSortOrder() {
      return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
    }
  }

  @TableName("products")
  public static class ProductEntity {
    @TableId
    private String id;
    private String merchantId;
    private String categoryId;
    private String name;
    private Integer sales;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageText;
    private String unit;
    private String description;
    private Integer stock;
    private String status;
    private Integer sortOrder;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getMerchantId() {
      return merchantId;
    }

    public void setMerchantId(String merchantId) {
      this.merchantId = merchantId;
    }

    public String getCategoryId() {
      return categoryId;
    }

    public void setCategoryId(String categoryId) {
      this.categoryId = categoryId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getSales() {
      return sales;
    }

    public void setSales(Integer sales) {
      this.sales = sales;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public void setPrice(BigDecimal price) {
      this.price = price;
    }

    public BigDecimal getOriginalPrice() {
      return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
      this.originalPrice = originalPrice;
    }

    public String getImageText() {
      return imageText;
    }

    public void setImageText(String imageText) {
      this.imageText = imageText;
    }

    public String getUnit() {
      return unit;
    }

    public void setUnit(String unit) {
      this.unit = unit;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getStock() {
      return stock;
    }

    public void setStock(Integer stock) {
      this.stock = stock;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Integer getSortOrder() {
      return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
    }
  }

  @TableName("addresses")
  public static class AddressEntity {
    @TableId
    private String id;
    private String userId;
    private String city;
    private String district;
    private String street;
    private String detail;
    private String contactName;
    private String phone;
    private String tag;
    private Boolean isDefault;
    private LocalDateTime createdAt;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }

    public String getDistrict() {
      return district;
    }

    public void setDistrict(String district) {
      this.district = district;
    }

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }

    public String getContactName() {
      return contactName;
    }

    public void setContactName(String contactName) {
      this.contactName = contactName;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public Boolean getIsDefault() {
      return isDefault;
    }

    public void setIsDefault(Boolean defaultAddress) {
      isDefault = defaultAddress;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  @TableName("cart_items")
  public static class CartItemEntity {
    @TableId
    private String id;
    private String userId;
    private String productId;
    private Integer quantity;
    private Boolean checked;
    private LocalDateTime createdAt;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getProductId() {
      return productId;
    }

    public void setProductId(String productId) {
      this.productId = productId;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }

    public Boolean getChecked() {
      return checked;
    }

    public void setChecked(Boolean checked) {
      this.checked = checked;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  @TableName("orders")
  public static class OrderEntity {
    @TableId
    private String id;
    private String orderNo;
    private String userId;
    private String addressId;
    private String addressCity;
    private String addressDistrict;
    private String addressStreet;
    private String addressDetail;
    private String addressContactName;
    private String addressPhone;
    private String addressTag;
    private Boolean addressIsDefault;
    private BigDecimal totalAmount;
    private String status;
    private String statusText;
    private String paymentStatus;
    private String paymentChannel;
    private LocalDateTime paidAt;
    private LocalDateTime paymentExpireAt;
    private LocalDateTime closedAt;
    private String statusHistoryJson;
    private String cancelReason;
    private String refundReason;
    private LocalDateTime createdAt;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getOrderNo() {
      return orderNo;
    }

    public void setOrderNo(String orderNo) {
      this.orderNo = orderNo;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getAddressId() {
      return addressId;
    }

    public void setAddressId(String addressId) {
      this.addressId = addressId;
    }

    public String getAddressCity() {
      return addressCity;
    }

    public void setAddressCity(String addressCity) {
      this.addressCity = addressCity;
    }

    public String getAddressDistrict() {
      return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
      this.addressDistrict = addressDistrict;
    }

    public String getAddressStreet() {
      return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
      this.addressStreet = addressStreet;
    }

    public String getAddressDetail() {
      return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
      this.addressDetail = addressDetail;
    }

    public String getAddressContactName() {
      return addressContactName;
    }

    public void setAddressContactName(String addressContactName) {
      this.addressContactName = addressContactName;
    }

    public String getAddressPhone() {
      return addressPhone;
    }

    public void setAddressPhone(String addressPhone) {
      this.addressPhone = addressPhone;
    }

    public String getAddressTag() {
      return addressTag;
    }

    public void setAddressTag(String addressTag) {
      this.addressTag = addressTag;
    }

    public Boolean getAddressIsDefault() {
      return addressIsDefault;
    }

    public void setAddressIsDefault(Boolean addressIsDefault) {
      this.addressIsDefault = addressIsDefault;
    }

    public BigDecimal getTotalAmount() {
      return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
      this.totalAmount = totalAmount;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getStatusText() {
      return statusText;
    }

    public void setStatusText(String statusText) {
      this.statusText = statusText;
    }

    public String getPaymentStatus() {
      return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
      this.paymentStatus = paymentStatus;
    }

    public String getPaymentChannel() {
      return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
      this.paymentChannel = paymentChannel;
    }

    public LocalDateTime getPaidAt() {
      return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
      this.paidAt = paidAt;
    }

    public LocalDateTime getPaymentExpireAt() {
      return paymentExpireAt;
    }

    public void setPaymentExpireAt(LocalDateTime paymentExpireAt) {
      this.paymentExpireAt = paymentExpireAt;
    }

    public LocalDateTime getClosedAt() {
      return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
      this.closedAt = closedAt;
    }

    public String getStatusHistoryJson() {
      return statusHistoryJson;
    }

    public void setStatusHistoryJson(String statusHistoryJson) {
      this.statusHistoryJson = statusHistoryJson;
    }

    public String getCancelReason() {
      return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
      this.cancelReason = cancelReason;
    }

    public String getRefundReason() {
      return refundReason;
    }

    public void setRefundReason(String refundReason) {
      this.refundReason = refundReason;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  @TableName("order_items")
  public static class OrderItemEntity {
    @TableId
    private String id;
    private String orderId;
    private String productId;
    private String merchantId;
    private String merchantName;
    private String categoryId;
    private String name;
    private Integer sales;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageText;
    private String unit;
    private String description;
    private Integer stock;
    private Integer quantity;
    private Boolean checked;
    private BigDecimal amount;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getOrderId() {
      return orderId;
    }

    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }

    public String getProductId() {
      return productId;
    }

    public void setProductId(String productId) {
      this.productId = productId;
    }

    public String getMerchantId() {
      return merchantId;
    }

    public void setMerchantId(String merchantId) {
      this.merchantId = merchantId;
    }

    public String getMerchantName() {
      return merchantName;
    }

    public void setMerchantName(String merchantName) {
      this.merchantName = merchantName;
    }

    public String getCategoryId() {
      return categoryId;
    }

    public void setCategoryId(String categoryId) {
      this.categoryId = categoryId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getSales() {
      return sales;
    }

    public void setSales(Integer sales) {
      this.sales = sales;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public void setPrice(BigDecimal price) {
      this.price = price;
    }

    public BigDecimal getOriginalPrice() {
      return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
      this.originalPrice = originalPrice;
    }

    public String getImageText() {
      return imageText;
    }

    public void setImageText(String imageText) {
      this.imageText = imageText;
    }

    public String getUnit() {
      return unit;
    }

    public void setUnit(String unit) {
      this.unit = unit;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getStock() {
      return stock;
    }

    public void setStock(Integer stock) {
      this.stock = stock;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }

    public Boolean getChecked() {
      return checked;
    }

    public void setChecked(Boolean checked) {
      this.checked = checked;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }
  }

  @TableName("payments")
  public static class PaymentEntity {
    @TableId
    private String id;
    private String orderId;
    private String userId;
    private String channel;
    private String status;
    private BigDecimal amount;
    private String outTradeNo;
    private String transactionId;
    private String qrCode;
    private String payUrl;
    private String gatewayOrderNo;
    private String notifyPayload;
    private String requestId;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getOrderId() {
      return orderId;
    }

    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getChannel() {
      return channel;
    }

    public void setChannel(String channel) {
      this.channel = channel;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    public String getOutTradeNo() {
      return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
      this.outTradeNo = outTradeNo;
    }

    public String getTransactionId() {
      return transactionId;
    }

    public void setTransactionId(String transactionId) {
      this.transactionId = transactionId;
    }

    public String getQrCode() {
      return qrCode;
    }

    public void setQrCode(String qrCode) {
      this.qrCode = qrCode;
    }

    public String getPayUrl() {
      return payUrl;
    }

    public void setPayUrl(String payUrl) {
      this.payUrl = payUrl;
    }

    public String getGatewayOrderNo() {
      return gatewayOrderNo;
    }

    public void setGatewayOrderNo(String gatewayOrderNo) {
      this.gatewayOrderNo = gatewayOrderNo;
    }

    public String getNotifyPayload() {
      return notifyPayload;
    }

    public void setNotifyPayload(String notifyPayload) {
      this.notifyPayload = notifyPayload;
    }

    public String getRequestId() {
      return requestId;
    }

    public void setRequestId(String requestId) {
      this.requestId = requestId;
    }

    public LocalDateTime getExpireAt() {
      return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
      this.expireAt = expireAt;
    }

    public LocalDateTime getPaidAt() {
      return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
      this.paidAt = paidAt;
    }

    public LocalDateTime getClosedAt() {
      return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
      this.closedAt = closedAt;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }

  @TableName("audit_logs")
  public static class AuditLogEntity {
    @TableId
    private String id;
    private String actorId;
    private String actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String requestId;
    private LocalDateTime createdAt;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getActorId() {
      return actorId;
    }

    public void setActorId(String actorId) {
      this.actorId = actorId;
    }

    public String getActorRole() {
      return actorRole;
    }

    public void setActorRole(String actorRole) {
      this.actorRole = actorRole;
    }

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }

    public String getTargetType() {
      return targetType;
    }

    public void setTargetType(String targetType) {
      this.targetType = targetType;
    }

    public String getTargetId() {
      return targetId;
    }

    public void setTargetId(String targetId) {
      this.targetId = targetId;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }

    public String getRequestId() {
      return requestId;
    }

    public void setRequestId(String requestId) {
      this.requestId = requestId;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }
}
