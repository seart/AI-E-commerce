package com.jingdong.backend.store;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.address.AddressDtos.AddressRequest;
import com.jingdong.backend.dto.address.AddressDtos.AddressResponse;
import com.jingdong.backend.dto.cart.CartDtos.CartItemResponse;
import com.jingdong.backend.dto.home.HomeDtos.BannerResponse;
import com.jingdong.backend.dto.home.HomeDtos.CategoryResponse;
import com.jingdong.backend.dto.home.HomeDtos.HomeResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantCategoryResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantDetailResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.ProductResponse;
import com.jingdong.backend.dto.order.OrderDtos.CheckoutItemRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderLineResponse;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.dto.profile.ProfileDtos.UserProfileStatsResponse;
import com.jingdong.backend.entity.DataEntities.AddressEntity;
import com.jingdong.backend.entity.DataEntities.BannerEntity;
import com.jingdong.backend.entity.DataEntities.CartItemEntity;
import com.jingdong.backend.entity.DataEntities.CategoryEntity;
import com.jingdong.backend.entity.DataEntities.MerchantCategoryEntity;
import com.jingdong.backend.entity.DataEntities.MerchantEntity;
import com.jingdong.backend.entity.DataEntities.OrderEntity;
import com.jingdong.backend.entity.DataEntities.OrderItemEntity;
import com.jingdong.backend.entity.DataEntities.ProductEntity;
import com.jingdong.backend.entity.DataEntities.UserEntity;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.mapper.AddressMapper;
import com.jingdong.backend.mapper.BannerMapper;
import com.jingdong.backend.mapper.CartItemMapper;
import com.jingdong.backend.mapper.CategoryMapper;
import com.jingdong.backend.mapper.MerchantCategoryMapper;
import com.jingdong.backend.mapper.MerchantMapper;
import com.jingdong.backend.mapper.OrderItemMapper;
import com.jingdong.backend.mapper.OrderMapper;
import com.jingdong.backend.mapper.ProductMapper;
import com.jingdong.backend.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseStore {
  private static final DateTimeFormatter ORDER_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
  private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;
  private final UserMapper userMapper;
  private final BannerMapper bannerMapper;
  private final CategoryMapper categoryMapper;
  private final MerchantMapper merchantMapper;
  private final MerchantCategoryMapper merchantCategoryMapper;
  private final ProductMapper productMapper;
  private final AddressMapper addressMapper;
  private final CartItemMapper cartItemMapper;
  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;

  public DatabaseStore(
      ObjectMapper objectMapper,
      UserMapper userMapper,
      BannerMapper bannerMapper,
      CategoryMapper categoryMapper,
      MerchantMapper merchantMapper,
      MerchantCategoryMapper merchantCategoryMapper,
      ProductMapper productMapper,
      AddressMapper addressMapper,
      CartItemMapper cartItemMapper,
      OrderMapper orderMapper,
      OrderItemMapper orderItemMapper
  ) {
    this.objectMapper = objectMapper;
    this.userMapper = userMapper;
    this.bannerMapper = bannerMapper;
    this.categoryMapper = categoryMapper;
    this.merchantMapper = merchantMapper;
    this.merchantCategoryMapper = merchantCategoryMapper;
    this.productMapper = productMapper;
    this.addressMapper = addressMapper;
    this.cartItemMapper = cartItemMapper;
    this.orderMapper = orderMapper;
    this.orderItemMapper = orderItemMapper;
  }

  public Optional<UserRecord> findUserByMobile(String mobile) {
    UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
        .eq(UserEntity::getMobile, mobile)
        .last("limit 1"));
    return Optional.ofNullable(user).map(this::toUserRecord);
  }

  public Optional<UserRecord> findUserById(String userId) {
    return Optional.ofNullable(userMapper.selectById(userId)).map(this::toUserRecord);
  }

  public UserRecord createUser(String mobile, String password) {
    if (findUserByMobile(mobile).isPresent()) {
      throw new BusinessException(ErrorCode.MOBILE_EXISTS);
    }

    UserEntity user = new UserEntity();
    user.setId(uid("u"));
    user.setMobile(mobile);
    user.setPassword(password);
    user.setNickname("用户" + mobile.substring(mobile.length() - 4));
    user.setMemberLevel("普通会员");
    user.setCouponCount(0);
    user.setFavoriteCount(0);
    user.setPoints(0);
    user.setGrowthValue(0);
    userMapper.insert(user);
    return toUserRecord(user);
  }

  public HomeResponse home() {
    List<BannerResponse> banners = bannerMapper.selectList(Wrappers.<BannerEntity>lambdaQuery()
            .orderByAsc(BannerEntity::getSortOrder))
        .stream()
        .map(this::toBannerResponse)
        .toList();
    List<CategoryResponse> categories = categoryMapper.selectList(Wrappers.<CategoryEntity>lambdaQuery()
            .orderByAsc(CategoryEntity::getSortOrder))
        .stream()
        .map(this::toCategoryResponse)
        .toList();
    List<MerchantResponse> merchants = merchantMapper.selectList(Wrappers.<MerchantEntity>lambdaQuery()
            .orderByAsc(MerchantEntity::getSortOrder))
        .stream()
        .map(this::toMerchantResponse)
        .toList();
    return new HomeResponse(banners, categories, merchants);
  }

  public List<MerchantResponse> searchMerchants(String keyword) {
    String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    List<MerchantEntity> merchants = merchantMapper.selectList(Wrappers.<MerchantEntity>lambdaQuery()
        .orderByAsc(MerchantEntity::getSortOrder));
    if (normalized.isEmpty()) {
      return merchants.stream().map(this::toMerchantResponse).toList();
    }

    Map<String, List<ProductEntity>> productsByMerchantId = productMapper.selectList(
            Wrappers.<ProductEntity>lambdaQuery().orderByAsc(ProductEntity::getSortOrder))
        .stream()
        .collect(Collectors.groupingBy(ProductEntity::getMerchantId));

    return merchants.stream()
        .filter(merchant -> merchantMatches(merchant, productsByMerchantId.getOrDefault(merchant.getId(), List.of()), normalized))
        .map(this::toMerchantResponse)
        .toList();
  }

  public MerchantDetailResponse merchantDetail(String merchantId) {
    MerchantEntity merchant = merchantMapper.selectById(merchantId);
    if (merchant == null) {
      throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
    }

    List<ProductResponse> products = productMapper.selectList(Wrappers.<ProductEntity>lambdaQuery()
            .eq(ProductEntity::getMerchantId, merchantId)
            .orderByAsc(ProductEntity::getSortOrder))
        .stream()
        .map(product -> toProductResponse(product, merchant))
        .toList();
    return new MerchantDetailResponse(toMerchantResponse(merchant), products);
  }

  public List<CartItemResponse> getCart(String userId) {
    ensureUser(userId);
    return cartItemMapper.selectList(Wrappers.<CartItemEntity>lambdaQuery()
            .eq(CartItemEntity::getUserId, userId)
            .orderByAsc(CartItemEntity::getCreatedAt))
        .stream()
        .map(this::toCartItemResponse)
        .toList();
  }

  @Transactional
  public List<CartItemResponse> addCartItem(String userId, String productId) {
    ensureUser(userId);
    product(productId);
    CartItemEntity existing = findCartItem(userId, productId);
    if (existing == null) {
      CartItemEntity cartItem = new CartItemEntity();
      cartItem.setId(uid("cart"));
      cartItem.setUserId(userId);
      cartItem.setProductId(productId);
      cartItem.setQuantity(1);
      cartItem.setChecked(true);
      cartItemMapper.insert(cartItem);
    } else {
      existing.setQuantity(existing.getQuantity() + 1);
      cartItemMapper.updateById(existing);
    }
    return getCart(userId);
  }

  @Transactional
  public List<CartItemResponse> updateCartQuantity(String userId, String productId, int quantity) {
    ensureUser(userId);
    CartItemEntity existing = findCartItem(userId, productId);
    if (existing == null) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "购物车商品不存在");
    }

    if (quantity <= 0) {
      cartItemMapper.deleteById(existing.getId());
    } else {
      existing.setQuantity(quantity);
      cartItemMapper.updateById(existing);
    }
    return getCart(userId);
  }

  @Transactional
  public List<CartItemResponse> setCartItemChecked(String userId, String productId, boolean checked) {
    ensureUser(userId);
    CartItemEntity existing = findCartItem(userId, productId);
    if (existing == null) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "购物车商品不存在");
    }

    existing.setChecked(checked);
    cartItemMapper.updateById(existing);
    return getCart(userId);
  }

  @Transactional
  public List<CartItemResponse> setMerchantChecked(String userId, String merchantId, boolean checked) {
    ensureUser(userId);
    List<String> productIds = productMapper.selectList(Wrappers.<ProductEntity>lambdaQuery()
            .select(ProductEntity::getId)
            .eq(ProductEntity::getMerchantId, merchantId))
        .stream()
        .map(ProductEntity::getId)
        .toList();
    if (!productIds.isEmpty()) {
      cartItemMapper.update(null, Wrappers.<CartItemEntity>lambdaUpdate()
          .eq(CartItemEntity::getUserId, userId)
          .in(CartItemEntity::getProductId, productIds)
          .set(CartItemEntity::getChecked, checked));
    }
    return getCart(userId);
  }

  @Transactional
  public List<CartItemResponse> setAllChecked(String userId, boolean checked) {
    ensureUser(userId);
    cartItemMapper.update(null, Wrappers.<CartItemEntity>lambdaUpdate()
        .eq(CartItemEntity::getUserId, userId)
        .set(CartItemEntity::getChecked, checked));
    return getCart(userId);
  }

  @Transactional
  public List<CartItemResponse> clearChecked(String userId) {
    ensureUser(userId);
    cartItemMapper.delete(Wrappers.<CartItemEntity>lambdaQuery()
        .eq(CartItemEntity::getUserId, userId)
        .eq(CartItemEntity::getChecked, true));
    return getCart(userId);
  }

  @Transactional
  public List<AddressResponse> getAddresses(String userId) {
    ensureUser(userId);
    List<AddressEntity> addresses = addresses(userId);
    if (!addresses.isEmpty() && addresses.stream().noneMatch(item -> Boolean.TRUE.equals(item.getIsDefault()))) {
      AddressEntity first = addresses.get(0);
      first.setIsDefault(true);
      addressMapper.updateById(first);
      addresses = addresses(userId);
    }
    return addresses.stream().map(this::toAddressResponse).toList();
  }

  @Transactional
  public AddressResponse createAddress(String userId, AddressRequest request) {
    ensureUser(userId);
    boolean makeDefault = request.isDefault() || addresses(userId).isEmpty();
    if (makeDefault) {
      clearDefaultAddress(userId);
    }

    AddressEntity address = new AddressEntity();
    address.setId(uid("addr"));
    address.setUserId(userId);
    address.setCity(request.city());
    address.setDistrict(request.district());
    address.setStreet(request.street());
    address.setDetail(request.detail());
    address.setContactName(request.contactName());
    address.setPhone(request.phone());
    address.setTag(request.tag());
    address.setIsDefault(makeDefault);
    addressMapper.insert(address);
    return toAddressResponse(address);
  }

  @Transactional
  public AddressResponse updateAddress(String userId, String addressId, AddressRequest request) {
    ensureUser(userId);
    AddressEntity address = addressMapper.selectOne(Wrappers.<AddressEntity>lambdaQuery()
        .eq(AddressEntity::getUserId, userId)
        .eq(AddressEntity::getId, addressId)
        .last("limit 1"));
    if (address == null) {
      throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
    }

    if (request.isDefault()) {
      clearDefaultAddress(userId);
    }
    address.setCity(request.city());
    address.setDistrict(request.district());
    address.setStreet(request.street());
    address.setDetail(request.detail());
    address.setContactName(request.contactName());
    address.setPhone(request.phone());
    address.setTag(request.tag());
    address.setIsDefault(request.isDefault());
    addressMapper.updateById(address);
    return toAddressResponse(address);
  }

  public List<OrderResponse> getOrders(String userId) {
    ensureUser(userId);
    return orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
            .eq(OrderEntity::getUserId, userId)
            .orderByDesc(OrderEntity::getCreatedAt))
        .stream()
        .map(this::toOrderResponse)
        .toList();
  }

  @Transactional
  public OrderResponse createOrder(
      String userId,
      String addressId,
      List<CheckoutItemRequest> items
  ) {
    ensureUser(userId);
    AddressEntity address = addressMapper.selectOne(Wrappers.<AddressEntity>lambdaQuery()
        .eq(AddressEntity::getUserId, userId)
        .eq(AddressEntity::getId, addressId)
        .last("limit 1"));
    if (address == null) {
      throw new BusinessException(ErrorCode.INVALID_ADDRESS);
    }

    List<OrderLineResponse> lines = items.stream()
        .map(this::toOrderLine)
        .toList();
    BigDecimal totalAmount = lines.stream()
        .map(OrderLineResponse::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    LocalDateTime now = LocalDateTime.now();

    OrderEntity order = new OrderEntity();
    order.setId(uid("order"));
    order.setOrderNo("JD" + ORDER_FORMATTER.format(now) + randomDigits());
    order.setUserId(userId);
    order.setAddressId(address.getId());
    order.setAddressCity(address.getCity());
    order.setAddressDistrict(address.getDistrict());
    order.setAddressStreet(address.getStreet());
    order.setAddressDetail(address.getDetail());
    order.setAddressContactName(address.getContactName());
    order.setAddressPhone(address.getPhone());
    order.setAddressTag(address.getTag());
    order.setAddressIsDefault(Boolean.TRUE.equals(address.getIsDefault()));
    order.setTotalAmount(totalAmount);
    order.setStatus("PAID");
    order.setStatusText("支付成功");
    order.setCreatedAt(now);
    orderMapper.insert(order);

    for (OrderLineResponse line : lines) {
      OrderItemEntity item = new OrderItemEntity();
      item.setId(uid("oi"));
      item.setOrderId(order.getId());
      item.setProductId(line.id());
      item.setMerchantId(line.merchantId());
      item.setMerchantName(line.merchantName());
      item.setCategoryId(line.categoryId());
      item.setName(line.name());
      item.setSales(line.sales());
      item.setPrice(line.price());
      item.setOriginalPrice(line.originalPrice());
      item.setImageText(line.imageText());
      item.setUnit(line.unit());
      item.setDescription(line.description());
      item.setStock(line.stock());
      item.setQuantity(line.quantity());
      item.setChecked(line.checked());
      item.setAmount(line.amount());
      orderItemMapper.insert(item);
    }

    List<String> purchasedProductIds = items.stream()
        .map(CheckoutItemRequest::productId)
        .toList();
    if (!purchasedProductIds.isEmpty()) {
      cartItemMapper.delete(Wrappers.<CartItemEntity>lambdaQuery()
          .eq(CartItemEntity::getUserId, userId)
          .in(CartItemEntity::getProductId, purchasedProductIds));
    }

    return toOrderResponse(order);
  }

  private boolean merchantMatches(
      MerchantEntity merchant,
      List<ProductEntity> products,
      String keyword
  ) {
    boolean merchantHit = (merchant.getName()
        + " " + merchant.getDescription()
        + " " + merchant.getNotice()
        + " " + String.join(" ", tags(merchant.getTagsJson())))
        .toLowerCase(Locale.ROOT)
        .contains(keyword);
    boolean productHit = products.stream()
        .anyMatch(product -> (product.getName() + " " + product.getDescription())
            .toLowerCase(Locale.ROOT)
            .contains(keyword));
    return merchantHit || productHit;
  }

  private OrderLineResponse toOrderLine(CheckoutItemRequest request) {
    ProductEntity product = product(request.productId());
    MerchantEntity merchant = merchantMapper.selectById(product.getMerchantId());
    BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
    return new OrderLineResponse(
        product.getId(),
        product.getMerchantId(),
        merchant.getName(),
        product.getCategoryId(),
        product.getName(),
        product.getSales(),
        product.getPrice(),
        product.getOriginalPrice(),
        product.getImageText(),
        product.getUnit(),
        product.getDescription(),
        product.getStock(),
        request.quantity(),
        true,
        amount
    );
  }

  private OrderResponse toOrderResponse(OrderEntity order) {
    List<OrderLineResponse> items = orderItemMapper.selectList(Wrappers.<OrderItemEntity>lambdaQuery()
            .eq(OrderItemEntity::getOrderId, order.getId()))
        .stream()
        .map(this::toOrderLineResponse)
        .toList();
    AddressResponse address = new AddressResponse(
        order.getAddressId(),
        order.getAddressCity(),
        order.getAddressDistrict(),
        order.getAddressStreet(),
        order.getAddressDetail(),
        order.getAddressContactName(),
        order.getAddressPhone(),
        order.getAddressTag(),
        Boolean.TRUE.equals(order.getAddressIsDefault())
    );
    return new OrderResponse(
        order.getId(),
        order.getOrderNo(),
        toInstantString(order.getCreatedAt()),
        order.getTotalAmount(),
        order.getStatus(),
        order.getStatusText(),
        items,
        address
    );
  }

  private OrderLineResponse toOrderLineResponse(OrderItemEntity item) {
    return new OrderLineResponse(
        item.getProductId(),
        item.getMerchantId(),
        item.getMerchantName(),
        item.getCategoryId(),
        item.getName(),
        item.getSales(),
        item.getPrice(),
        item.getOriginalPrice(),
        item.getImageText(),
        item.getUnit(),
        item.getDescription(),
        item.getStock(),
        item.getQuantity(),
        Boolean.TRUE.equals(item.getChecked()),
        item.getAmount()
    );
  }

  private CartItemResponse toCartItemResponse(CartItemEntity cartItem) {
    ProductEntity product = product(cartItem.getProductId());
    MerchantEntity merchant = merchantMapper.selectById(product.getMerchantId());
    return new CartItemResponse(
        product.getId(),
        product.getMerchantId(),
        merchant.getName(),
        product.getCategoryId(),
        product.getName(),
        product.getSales(),
        product.getPrice(),
        product.getOriginalPrice(),
        product.getImageText(),
        product.getUnit(),
        product.getDescription(),
        product.getStock(),
        cartItem.getQuantity(),
        Boolean.TRUE.equals(cartItem.getChecked())
    );
  }

  private AddressResponse toAddressResponse(AddressEntity address) {
    return new AddressResponse(
        address.getId(),
        address.getCity(),
        address.getDistrict(),
        address.getStreet(),
        address.getDetail(),
        address.getContactName(),
        address.getPhone(),
        address.getTag(),
        Boolean.TRUE.equals(address.getIsDefault())
    );
  }

  private BannerResponse toBannerResponse(BannerEntity banner) {
    return new BannerResponse(
        banner.getId(),
        banner.getTitle(),
        banner.getSubtitle(),
        banner.getBackground()
    );
  }

  private CategoryResponse toCategoryResponse(CategoryEntity category) {
    return new CategoryResponse(category.getId(), category.getName(), category.getIcon());
  }

  private MerchantResponse toMerchantResponse(MerchantEntity merchant) {
    return new MerchantResponse(
        merchant.getId(),
        merchant.getName(),
        merchant.getSales(),
        merchant.getMinOrderPrice(),
        merchant.getDeliveryFee(),
        merchant.getDeliveryMinutes(),
        tags(merchant.getTagsJson()),
        merchant.getDescription(),
        merchant.getNotice(),
        merchant.getRating(),
        merchant.getLogoBackground(),
        merchant.getLogoText(),
        merchantCategoryMapper.selectList(Wrappers.<MerchantCategoryEntity>lambdaQuery()
                .eq(MerchantCategoryEntity::getMerchantId, merchant.getId())
                .orderByAsc(MerchantCategoryEntity::getSortOrder))
            .stream()
            .map(item -> new MerchantCategoryResponse(item.getCategoryId(), item.getName()))
            .toList()
    );
  }

  private ProductResponse toProductResponse(ProductEntity product, MerchantEntity merchant) {
    return new ProductResponse(
        product.getId(),
        product.getMerchantId(),
        merchant.getName(),
        product.getCategoryId(),
        product.getName(),
        product.getSales(),
        product.getPrice(),
        product.getOriginalPrice(),
        product.getImageText(),
        product.getUnit(),
        product.getDescription(),
        product.getStock()
    );
  }

  private List<AddressEntity> addresses(String userId) {
    return addressMapper.selectList(Wrappers.<AddressEntity>lambdaQuery()
        .eq(AddressEntity::getUserId, userId)
        .orderByDesc(AddressEntity::getIsDefault)
        .orderByAsc(AddressEntity::getCreatedAt));
  }

  private void clearDefaultAddress(String userId) {
    addressMapper.update(null, Wrappers.<AddressEntity>lambdaUpdate()
        .eq(AddressEntity::getUserId, userId)
        .set(AddressEntity::getIsDefault, false));
  }

  private CartItemEntity findCartItem(String userId, String productId) {
    return cartItemMapper.selectOne(Wrappers.<CartItemEntity>lambdaQuery()
        .eq(CartItemEntity::getUserId, userId)
        .eq(CartItemEntity::getProductId, productId)
        .last("limit 1"));
  }

  private void ensureUser(String userId) {
    if (userMapper.selectById(userId) == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }

  private ProductEntity product(String productId) {
    ProductEntity product = productMapper.selectById(productId);
    if (product == null) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }
    return product;
  }

  private List<String> tags(String tagsJson) {
    try {
      return objectMapper.readValue(tagsJson, TAGS_TYPE);
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid merchant tags JSON", exception);
    }
  }

  private UserRecord toUserRecord(UserEntity user) {
    return new UserRecord(
        user.getId(),
        user.getMobile(),
        user.getPassword(),
        user.getNickname(),
        user.getMemberLevel(),
        new UserProfileStatsResponse(
            user.getCouponCount(),
            user.getFavoriteCount(),
            user.getPoints(),
            user.getGrowthValue()
        )
    );
  }

  private String toInstantString(LocalDateTime dateTime) {
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toString();
  }

  private String randomDigits() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
  }

  private String uid(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  public record UserRecord(
      String id,
      String mobile,
      String password,
      String nickname,
      String memberLevel,
      UserProfileStatsResponse profileStats
  ) {}
}
