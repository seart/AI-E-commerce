package com.jingdong.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.admin.AdminDtos.BrandAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.BrandUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.CategoryAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.CategoryUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.ProductAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSkuAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSkuUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSpuAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSpuUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.ProductUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.SkuSpecRequest;
import com.jingdong.backend.dto.admin.AdminDtos.SpecGroupAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.SpecGroupUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.SpecOptionAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.SpecOptionUpsertRequest;
import com.jingdong.backend.dto.product.ProductDtos.ProductCardResponse;
import com.jingdong.backend.dto.product.ProductDtos.ProductDetailResponse;
import com.jingdong.backend.dto.product.ProductDtos.ProductSkuResponse;
import com.jingdong.backend.dto.product.ProductDtos.SkuSpecResponse;
import com.jingdong.backend.entity.DataEntities.BrandEntity;
import com.jingdong.backend.entity.DataEntities.CategoryEntity;
import com.jingdong.backend.entity.DataEntities.MerchantCategoryEntity;
import com.jingdong.backend.entity.DataEntities.MerchantEntity;
import com.jingdong.backend.entity.DataEntities.ProductEntity;
import com.jingdong.backend.entity.DataEntities.ProductSpuEntity;
import com.jingdong.backend.entity.DataEntities.SpecGroupEntity;
import com.jingdong.backend.entity.DataEntities.SpecOptionEntity;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.mapper.BrandMapper;
import com.jingdong.backend.mapper.CategoryMapper;
import com.jingdong.backend.mapper.MerchantCategoryMapper;
import com.jingdong.backend.mapper.MerchantMapper;
import com.jingdong.backend.mapper.ProductMapper;
import com.jingdong.backend.mapper.ProductSpuMapper;
import com.jingdong.backend.mapper.SpecGroupMapper;
import com.jingdong.backend.mapper.SpecOptionMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCenterService {
  private static final String ACTIVE = "ACTIVE";
  private static final String DISABLED = "DISABLED";
  private static final String CHANNEL = "CHANNEL";
  private static final String PRODUCT = "PRODUCT";
  private static final String DRAFT = "DRAFT";
  private static final String ON_SHELF = "ON_SHELF";
  private static final String OFF_SHELF = "OFF_SHELF";
  private static final TypeReference<List<SkuSpecRequest>> SKU_SPEC_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;
  private final CategoryMapper categoryMapper;
  private final BrandMapper brandMapper;
  private final SpecGroupMapper specGroupMapper;
  private final SpecOptionMapper specOptionMapper;
  private final ProductSpuMapper productSpuMapper;
  private final ProductMapper productMapper;
  private final MerchantMapper merchantMapper;
  private final MerchantCategoryMapper merchantCategoryMapper;
  private final AuditLogService auditLogService;
  private final InventoryService inventoryService;

  public ProductCenterService(
      ObjectMapper objectMapper,
      CategoryMapper categoryMapper,
      BrandMapper brandMapper,
      SpecGroupMapper specGroupMapper,
      SpecOptionMapper specOptionMapper,
      ProductSpuMapper productSpuMapper,
      ProductMapper productMapper,
      MerchantMapper merchantMapper,
      MerchantCategoryMapper merchantCategoryMapper,
      AuditLogService auditLogService,
      InventoryService inventoryService
  ) {
    this.objectMapper = objectMapper;
    this.categoryMapper = categoryMapper;
    this.brandMapper = brandMapper;
    this.specGroupMapper = specGroupMapper;
    this.specOptionMapper = specOptionMapper;
    this.productSpuMapper = productSpuMapper;
    this.productMapper = productMapper;
    this.merchantMapper = merchantMapper;
    this.merchantCategoryMapper = merchantCategoryMapper;
    this.auditLogService = auditLogService;
    this.inventoryService = inventoryService;
  }

  public List<CategoryAdminResponse> categories() {
    return categoryMapper.selectList(Wrappers.<CategoryEntity>lambdaQuery()
            .orderByAsc(CategoryEntity::getLevel)
            .orderByAsc(CategoryEntity::getSortOrder))
        .stream()
        .map(this::toCategoryResponse)
        .toList();
  }

  @Transactional
  public CategoryAdminResponse saveCategory(CategoryUpsertRequest request) {
    if (isBlank(request.name())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "类目名称不能为空");
    }
    String id = value(request.id(), uid("cat"));
    CategoryEntity category = Optional.ofNullable(categoryMapper.selectById(id)).orElseGet(CategoryEntity::new);
    category.setId(id);
    category.setName(request.name().trim());
    category.setIcon(value(request.icon(), "/api/static/category/" + id + ".png"));
    category.setParentId(blankToNull(request.parentId()));
    category.setLevel(value(request.level(), category.getParentId() == null ? 1 : 2));
    category.setType(normalizeCategoryType(request.type()));
    category.setStatus(normalizeCommonStatus(request.status()));
    category.setSortOrder(value(request.sortOrder(), 100));
    upsertCategory(category);
    auditLogService.record("ADMIN_SAVE_CATEGORY", "CATEGORY", id, category.getName());
    return toCategoryResponse(categoryMapper.selectById(id));
  }

  @Transactional
  public CategoryAdminResponse updateCategoryStatus(String categoryId, String status) {
    CategoryEntity category = Optional.ofNullable(categoryMapper.selectById(categoryId))
        .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    category.setStatus(normalizeCommonStatus(status));
    categoryMapper.updateById(category);
    auditLogService.record("ADMIN_UPDATE_CATEGORY_STATUS", "CATEGORY", categoryId, category.getStatus());
    return toCategoryResponse(category);
  }

  public List<BrandAdminResponse> brands() {
    return brandMapper.selectList(Wrappers.<BrandEntity>lambdaQuery()
            .orderByAsc(BrandEntity::getSortOrder))
        .stream()
        .map(this::toBrandResponse)
        .toList();
  }

  @Transactional
  public BrandAdminResponse saveBrand(BrandUpsertRequest request) {
    if (isBlank(request.name())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "品牌名称不能为空");
    }
    String id = value(request.id(), uid("brand"));
    BrandEntity brand = Optional.ofNullable(brandMapper.selectById(id)).orElseGet(BrandEntity::new);
    brand.setId(id);
    brand.setName(request.name().trim());
    brand.setLogo(value(request.logo(), ""));
    brand.setDescription(value(request.description(), ""));
    brand.setStatus(normalizeCommonStatus(request.status()));
    brand.setSortOrder(value(request.sortOrder(), 100));
    upsertBrand(brand);
    auditLogService.record("ADMIN_SAVE_BRAND", "BRAND", id, brand.getName());
    return toBrandResponse(brandMapper.selectById(id));
  }

  @Transactional
  public BrandAdminResponse updateBrandStatus(String brandId, String status) {
    BrandEntity brand = Optional.ofNullable(brandMapper.selectById(brandId))
        .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
    brand.setStatus(normalizeCommonStatus(status));
    brandMapper.updateById(brand);
    auditLogService.record("ADMIN_UPDATE_BRAND_STATUS", "BRAND", brandId, brand.getStatus());
    return toBrandResponse(brand);
  }

  public List<SpecGroupAdminResponse> specGroups() {
    Map<String, List<SpecOptionAdminResponse>> optionsByGroupId = specOptionMapper.selectList(
            Wrappers.<SpecOptionEntity>lambdaQuery().orderByAsc(SpecOptionEntity::getSortOrder))
        .stream()
        .map(this::toSpecOptionResponse)
        .collect(Collectors.groupingBy(SpecOptionAdminResponse::groupId, LinkedHashMap::new, Collectors.toList()));

    return specGroupMapper.selectList(Wrappers.<SpecGroupEntity>lambdaQuery()
            .orderByAsc(SpecGroupEntity::getSortOrder))
        .stream()
        .map(group -> new SpecGroupAdminResponse(
            group.getId(),
            group.getName(),
            group.getStatus(),
            group.getSortOrder(),
            optionsByGroupId.getOrDefault(group.getId(), List.of())
        ))
        .toList();
  }

  @Transactional
  public SpecGroupAdminResponse saveSpecGroup(SpecGroupUpsertRequest request) {
    if (isBlank(request.name())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "规格组名称不能为空");
    }
    String id = value(request.id(), uid("specg"));
    SpecGroupEntity group = Optional.ofNullable(specGroupMapper.selectById(id)).orElseGet(SpecGroupEntity::new);
    group.setId(id);
    group.setName(request.name().trim());
    group.setStatus(normalizeCommonStatus(request.status()));
    group.setSortOrder(value(request.sortOrder(), 100));
    upsertSpecGroup(group);
    auditLogService.record("ADMIN_SAVE_SPEC_GROUP", "SPEC_GROUP", id, group.getName());
    return specGroups().stream()
        .filter(item -> item.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  @Transactional
  public SpecOptionAdminResponse saveSpecOption(String groupId, SpecOptionUpsertRequest request) {
    SpecGroupEntity group = Optional.ofNullable(specGroupMapper.selectById(groupId))
        .orElseThrow(() -> new BusinessException(ErrorCode.SPEC_NOT_FOUND));
    if (isBlank(request.name())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "规格值名称不能为空");
    }
    String id = value(request.id(), uid("speco"));
    SpecOptionEntity option = Optional.ofNullable(specOptionMapper.selectById(id)).orElseGet(SpecOptionEntity::new);
    option.setId(id);
    option.setGroupId(group.getId());
    option.setName(request.name().trim());
    option.setStatus(normalizeCommonStatus(request.status()));
    option.setSortOrder(value(request.sortOrder(), 100));
    upsertSpecOption(option);
    auditLogService.record("ADMIN_SAVE_SPEC_OPTION", "SPEC_OPTION", id, option.getName());
    return toSpecOptionResponse(specOptionMapper.selectById(id));
  }

  @Transactional
  public SpecOptionAdminResponse updateSpecOption(String optionId, SpecOptionUpsertRequest request) {
    SpecOptionEntity existing = Optional.ofNullable(specOptionMapper.selectById(optionId))
        .orElseThrow(() -> new BusinessException(ErrorCode.SPEC_NOT_FOUND));
    return saveSpecOption(existing.getGroupId(), new SpecOptionUpsertRequest(
        optionId,
        request.name(),
        request.status(),
        request.sortOrder()
    ));
  }

  @Transactional
  public SpecOptionAdminResponse updateSpecOptionStatus(String optionId, String status) {
    SpecOptionEntity option = Optional.ofNullable(specOptionMapper.selectById(optionId))
        .orElseThrow(() -> new BusinessException(ErrorCode.SPEC_NOT_FOUND));
    option.setStatus(normalizeCommonStatus(status));
    specOptionMapper.updateById(option);
    auditLogService.record("ADMIN_UPDATE_SPEC_OPTION_STATUS", "SPEC_OPTION", optionId, option.getStatus());
    return toSpecOptionResponse(option);
  }

  public List<ProductSpuAdminResponse> productSpus() {
    return productSpuMapper.selectList(Wrappers.<ProductSpuEntity>lambdaQuery()
            .orderByAsc(ProductSpuEntity::getSortOrder))
        .stream()
        .map(spu -> toProductSpuAdminResponse(spu, false))
        .toList();
  }

  public ProductSpuAdminResponse productSpuDetail(String spuId) {
    ProductSpuEntity spu = Optional.ofNullable(productSpuMapper.selectById(spuId))
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    return toProductSpuAdminResponse(spu, true);
  }

  @Transactional
  public ProductSpuAdminResponse saveProductSpu(ProductSpuUpsertRequest request) {
    validateSpuRequest(request);
    String id = value(request.id(), uid("spu"));
    String requestedStatus = normalizeSpuStatus(request.status());

    ProductSpuEntity spu = Optional.ofNullable(productSpuMapper.selectById(id)).orElseGet(ProductSpuEntity::new);
    spu.setId(id);
    spu.setMerchantId(request.merchantId());
    spu.setCategoryId(request.categoryId());
    spu.setBrandId(blankToNull(request.brandId()));
    spu.setName(request.name().trim());
    spu.setSubtitle(value(request.subtitle(), ""));
    spu.setMainImage(value(request.mainImage(), ""));
    spu.setDetail(value(request.detail(), ""));
    spu.setDetailImagesJson(writeStringList(request.detailImages()));
    spu.setStatus(ON_SHELF.equals(requestedStatus) ? DRAFT : requestedStatus);
    spu.setSortOrder(value(request.sortOrder(), 100));
    upsertSpu(spu);

    if (request.skus() != null) {
      for (ProductSkuUpsertRequest sku : request.skus()) {
        saveSkuInternal(spu, sku);
      }
    }

    if (ON_SHELF.equals(requestedStatus)) {
      ensureSpuCanBeOnShelf(id);
    }
    spu.setStatus(requestedStatus);
    productSpuMapper.updateById(spu);
    auditLogService.record("ADMIN_SAVE_PRODUCT_SPU", "PRODUCT_SPU", id, spu.getName());
    return toProductSpuAdminResponse(productSpuMapper.selectById(id), true);
  }

  @Transactional
  public ProductSpuAdminResponse updateProductSpuStatus(String spuId, String status) {
    ProductSpuEntity spu = Optional.ofNullable(productSpuMapper.selectById(spuId))
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    String nextStatus = normalizeSpuStatus(status);
    if (ON_SHELF.equals(nextStatus)) {
      ensureSpuCanBeOnShelf(spuId);
    }
    spu.setStatus(nextStatus);
    productSpuMapper.updateById(spu);
    auditLogService.record("ADMIN_UPDATE_PRODUCT_SPU_STATUS", "PRODUCT_SPU", spuId, nextStatus);
    return toProductSpuAdminResponse(spu, true);
  }

  @Transactional
  public ProductSkuAdminResponse saveProductSku(String spuId, ProductSkuUpsertRequest request) {
    ProductSpuEntity spu = Optional.ofNullable(productSpuMapper.selectById(spuId))
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    ProductEntity sku = saveSkuInternal(spu, request);
    auditLogService.record("ADMIN_SAVE_PRODUCT_SKU", "PRODUCT_SKU", sku.getId(), sku.getName());
    return toProductSkuAdminResponse(sku);
  }

  @Transactional
  public ProductSkuAdminResponse updateProductSkuStatus(String spuId, String skuId, String status) {
    ProductEntity sku = Optional.ofNullable(productMapper.selectById(skuId))
        .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
    if (!spuId.equals(sku.getSpuId())) {
      throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
    }
    sku.setStatus(normalizeSkuStatus(status));
    productMapper.updateById(sku);
    auditLogService.record("ADMIN_UPDATE_PRODUCT_SKU_STATUS", "PRODUCT_SKU", skuId, sku.getStatus());
    return toProductSkuAdminResponse(sku);
  }

  @Transactional
  public ProductAdminResponse saveLegacyProduct(ProductUpsertRequest request) {
    ProductEntity existing = request.id() == null ? null : productMapper.selectById(request.id());
    String spuId = existing == null ? null : existing.getSpuId();
    String brandId = existing != null && !isBlank(existing.getBrandId()) ? existing.getBrandId() : "brand_jd";
    ProductSpuUpsertRequest spuRequest = new ProductSpuUpsertRequest(
        spuId,
        request.merchantId(),
        legacyCategoryId(request.merchantId(), request.categoryId()),
        brandId,
        value(request.name(), "未命名商品"),
        value(request.description(), ""),
        value(request.imageText(), ""),
        value(request.description(), ""),
        List.of(),
        normalizeLegacyProductStatus(request.status()),
        value(request.sortOrder(), 100),
        List.of(new ProductSkuUpsertRequest(
            request.id(),
            request.id(),
            List.of(defaultSkuSpec()),
            value(request.price(), BigDecimal.ZERO),
            value(request.originalPrice(), value(request.price(), BigDecimal.ZERO)),
            value(request.unit(), "件"),
            value(request.stock(), 0),
            normalizeSkuStatus(request.status())
        ))
    );
    ProductSpuAdminResponse saved = saveProductSpu(spuRequest);
    ProductSkuAdminResponse sku = saved.skus().isEmpty() ? null : saved.skus().get(0);
    if (sku == null) {
      throw new BusinessException(ErrorCode.PRODUCT_CENTER_INVALID_STATE);
    }
    ProductEntity product = productMapper.selectById(sku.skuId());
    product.setSales(value(request.sales(), 0));
    product.setImageText(value(request.imageText(), "商品"));
    product.setDescription(value(request.description(), "后台新增商品"));
    product.setSortOrder(value(request.sortOrder(), 100));
    productMapper.updateById(product);
    MerchantEntity merchant = merchantMapper.selectById(product.getMerchantId());
    return toLegacyProductResponse(product, merchant);
  }

  public List<ProductCardResponse> searchProducts(
      String keyword,
      String merchantId,
      String categoryId,
      String brandId
  ) {
    List<ProductEntity> skus = visibleSkus().stream()
        .filter(sku -> isBlank(merchantId) || merchantId.equals(sku.getMerchantId()))
        .filter(sku -> isBlank(categoryId) || categoryId.equals(sku.getCategoryId()))
        .filter(sku -> isBlank(brandId) || brandId.equals(sku.getBrandId()))
        .toList();
    Map<String, List<ProductEntity>> skusBySpuId = groupSkusBySpu(skus);
    String normalized = normalizeKeyword(keyword);
    return skusBySpuId.entrySet().stream()
        .map(entry -> toProductCardResponse(productSpuMapper.selectById(entry.getKey()), entry.getValue()))
        .filter(Objects::nonNull)
        .filter(card -> normalized.isBlank() || productCardMatches(card, normalized))
        .sorted(Comparator.comparing(ProductCardResponse::sales).reversed())
        .toList();
  }

  public ProductDetailResponse productDetail(String productId) {
    ProductSpuEntity spu = productSpuMapper.selectById(productId);
    if (spu == null) {
      ProductEntity sku = productMapper.selectById(productId);
      if (sku != null) {
        spu = productSpuMapper.selectById(sku.getSpuId());
      }
    }
    if (spu == null || !ON_SHELF.equals(spu.getStatus())) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }
    MerchantEntity merchant = merchantMapper.selectById(spu.getMerchantId());
    if (merchant == null || !ACTIVE.equals(merchant.getStatus())) {
      throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
    }
    List<ProductEntity> skus = visibleSkusBySpu(spu.getId());
    if (skus.isEmpty()) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }
    return new ProductDetailResponse(
        toProductCardResponse(spu, skus),
        skus.stream().map(this::toProductSkuResponse).toList(),
        readStringList(spu.getDetailImagesJson()),
        spu.getDetail()
    );
  }

  private void validateSpuRequest(ProductSpuUpsertRequest request) {
    if (isBlank(request.name())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "商品名称不能为空");
    }
    ensureActiveMerchant(request.merchantId());
    ensureActiveCategory(request.merchantId(), request.categoryId());
    ensureActiveBrand(request.brandId());
  }

  private ProductEntity saveSkuInternal(ProductSpuEntity spu, ProductSkuUpsertRequest request) {
    ensureValidMoney(request.price(), request.originalPrice());
    ensureValidStock(request.stock());
    ensureNoDuplicateSkuSpecs(spu.getId(), request.skuId(), request.specs());

    String skuId = value(request.skuId(), uid("sku"));
    ProductEntity sku = Optional.ofNullable(productMapper.selectById(skuId)).orElseGet(ProductEntity::new);
    sku.setId(skuId);
    sku.setMerchantId(spu.getMerchantId());
    sku.setCategoryId(spu.getCategoryId());
    sku.setSpuId(spu.getId());
    sku.setBrandId(spu.getBrandId());
    sku.setSkuCode(value(request.skuCode(), skuId));
    sku.setSpecsJson(writeSpecs(request.specs()));
    sku.setName(spu.getName());
    sku.setSales(value(sku.getSales(), 0));
    sku.setPrice(request.price());
    sku.setOriginalPrice(request.originalPrice());
    sku.setImageText(value(spu.getMainImage(), "商品"));
    sku.setMainImage(value(spu.getMainImage(), ""));
    sku.setUnit(value(request.unit(), "件"));
    sku.setDescription(value(spu.getSubtitle(), value(spu.getDetail(), "")));
    sku.setStock(request.stock());
    sku.setStatus(normalizeSkuStatus(request.status()));
    sku.setSortOrder(value(sku.getSortOrder(), 100));
    upsertSku(sku);
    inventoryService.syncAvailableFromProductUpdate(
        skuId,
        request.stock(),
        "商品中心保存 SKU 库存"
    );
    return productMapper.selectById(skuId);
  }

  private void ensureActiveMerchant(String merchantId) {
    MerchantEntity merchant = merchantMapper.selectById(merchantId);
    if (merchant == null || !ACTIVE.equals(merchant.getStatus())) {
      throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
    }
  }

  private void ensureActiveCategory(String merchantId, String categoryId) {
    if (isBlank(categoryId)) {
      throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
    }
    CategoryEntity category = categoryMapper.selectById(categoryId);
    if (category != null && ACTIVE.equals(category.getStatus())) {
      return;
    }
    MerchantCategoryEntity merchantCategory = merchantCategoryMapper.selectOne(
        Wrappers.<MerchantCategoryEntity>lambdaQuery()
            .eq(MerchantCategoryEntity::getMerchantId, merchantId)
            .eq(MerchantCategoryEntity::getCategoryId, categoryId)
            .last("limit 1"));
    if (merchantCategory == null) {
      throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
    }
  }

  private String legacyCategoryId(String merchantId, String categoryId) {
    if (!isBlank(categoryId) && !"general".equals(categoryId)) {
      return categoryId;
    }
    MerchantCategoryEntity merchantCategory = merchantCategoryMapper.selectOne(
        Wrappers.<MerchantCategoryEntity>lambdaQuery()
            .eq(MerchantCategoryEntity::getMerchantId, merchantId)
            .orderByAsc(MerchantCategoryEntity::getSortOrder)
            .last("limit 1"));
    return merchantCategory == null ? value(categoryId, "general") : merchantCategory.getCategoryId();
  }

  private void ensureActiveBrand(String brandId) {
    if (isBlank(brandId)) {
      return;
    }
    BrandEntity brand = brandMapper.selectById(brandId);
    if (brand == null || !ACTIVE.equals(brand.getStatus())) {
      throw new BusinessException(ErrorCode.BRAND_NOT_FOUND);
    }
  }

  private void ensureValidMoney(BigDecimal price, BigDecimal originalPrice) {
    if (price == null || originalPrice == null || price.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "价格不能小于 0");
    }
    if (originalPrice.compareTo(price) < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "原价不能小于现价");
    }
  }

  private void ensureValidStock(Integer stock) {
    if (stock == null || stock < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "库存不能小于 0");
    }
  }

  private void ensureNoDuplicateSkuSpecs(String spuId, String skuId, List<SkuSpecRequest> specs) {
    String specKey = specKey(specs);
    List<ProductEntity> existingSkus = productMapper.selectList(Wrappers.<ProductEntity>lambdaQuery()
        .eq(ProductEntity::getSpuId, spuId));
    for (ProductEntity existing : existingSkus) {
      if (existing.getId().equals(skuId)) {
        continue;
      }
      if (specKey(readSpecs(existing.getSpecsJson())).equals(specKey)) {
        throw new BusinessException(ErrorCode.DUPLICATE_SKU_SPEC);
      }
    }
  }

  private void ensureSpuCanBeOnShelf(String spuId) {
    Long count = productMapper.selectCount(Wrappers.<ProductEntity>lambdaQuery()
        .eq(ProductEntity::getSpuId, spuId)
        .eq(ProductEntity::getStatus, ON_SHELF));
    if (count == null || count == 0) {
      throw new BusinessException(ErrorCode.PRODUCT_CENTER_INVALID_STATE, "上架 SPU 至少需要一个上架 SKU");
    }
  }

  private List<ProductEntity> visibleSkus() {
    Map<String, MerchantEntity> merchants = merchantMapper.selectList(Wrappers.<MerchantEntity>lambdaQuery()
            .eq(MerchantEntity::getStatus, ACTIVE))
        .stream()
        .collect(Collectors.toMap(MerchantEntity::getId, Function.identity()));
    Map<String, ProductSpuEntity> spus = productSpuMapper.selectList(Wrappers.<ProductSpuEntity>lambdaQuery()
            .eq(ProductSpuEntity::getStatus, ON_SHELF))
        .stream()
        .collect(Collectors.toMap(ProductSpuEntity::getId, Function.identity()));
    return productMapper.selectList(Wrappers.<ProductEntity>lambdaQuery()
            .eq(ProductEntity::getStatus, ON_SHELF)
            .orderByAsc(ProductEntity::getSortOrder))
        .stream()
        .filter(sku -> merchants.containsKey(sku.getMerchantId()))
        .filter(sku -> !isBlank(sku.getSpuId()) && spus.containsKey(sku.getSpuId()))
        .toList();
  }

  private List<ProductEntity> visibleSkusBySpu(String spuId) {
    return productMapper.selectList(Wrappers.<ProductEntity>lambdaQuery()
            .eq(ProductEntity::getSpuId, spuId)
            .eq(ProductEntity::getStatus, ON_SHELF)
            .orderByAsc(ProductEntity::getSortOrder))
        .stream()
        .toList();
  }

  private Map<String, List<ProductEntity>> groupSkusBySpu(List<ProductEntity> skus) {
    return skus.stream()
        .collect(Collectors.groupingBy(ProductEntity::getSpuId, LinkedHashMap::new, Collectors.toList()));
  }

  private ProductCardResponse toProductCardResponse(ProductSpuEntity spu, List<ProductEntity> skus) {
    if (spu == null || skus.isEmpty()) {
      return null;
    }
    MerchantEntity merchant = merchantMapper.selectById(spu.getMerchantId());
    BrandEntity brand = isBlank(spu.getBrandId()) ? null : brandMapper.selectById(spu.getBrandId());
    ProductEntity first = skus.stream().min(Comparator.comparing(ProductEntity::getPrice)).orElseThrow();
    BigDecimal minPrice = skus.stream().map(ProductEntity::getPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    BigDecimal maxPrice = skus.stream().map(ProductEntity::getPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    int stock = skus.stream().map(ProductEntity::getStock).filter(Objects::nonNull).reduce(0, Integer::sum);
    int sales = skus.stream().map(ProductEntity::getSales).filter(Objects::nonNull).reduce(0, Integer::sum);
    boolean singleSku = skus.size() == 1;
    return new ProductCardResponse(
        spu.getId(),
        spu.getId(),
        singleSku ? first.getId() : null,
        spu.getMerchantId(),
        merchant == null ? "" : merchant.getName(),
        spu.getCategoryId(),
        spu.getBrandId(),
        brand == null ? "" : brand.getName(),
        spu.getName(),
        spu.getSubtitle(),
        sales,
        minPrice,
        maxPrice,
        first.getOriginalPrice(),
        value(first.getImageText(), spu.getMainImage()),
        value(spu.getMainImage(), first.getMainImage()),
        first.getUnit(),
        value(spu.getDetail(), first.getDescription()),
        stock,
        singleSku
    );
  }

  private ProductSkuResponse toProductSkuResponse(ProductEntity sku) {
    List<SkuSpecRequest> specs = readSpecs(sku.getSpecsJson());
    return new ProductSkuResponse(
        sku.getId(),
        sku.getId(),
        sku.getSpuId(),
        sku.getSkuCode(),
        specs.stream()
            .map(item -> new SkuSpecResponse(item.groupId(), item.groupName(), item.optionId(), item.optionName()))
            .toList(),
        specText(specs),
        sku.getPrice(),
        sku.getOriginalPrice(),
        sku.getUnit(),
        sku.getStock(),
        sku.getStatus()
    );
  }

  private ProductSpuAdminResponse toProductSpuAdminResponse(ProductSpuEntity spu, boolean includeSkus) {
    MerchantEntity merchant = merchantMapper.selectById(spu.getMerchantId());
    BrandEntity brand = isBlank(spu.getBrandId()) ? null : brandMapper.selectById(spu.getBrandId());
    List<ProductEntity> skus = productMapper.selectList(Wrappers.<ProductEntity>lambdaQuery()
        .eq(ProductEntity::getSpuId, spu.getId())
        .orderByAsc(ProductEntity::getSortOrder));
    BigDecimal minPrice = skus.stream().map(ProductEntity::getPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    BigDecimal maxPrice = skus.stream().map(ProductEntity::getPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    int totalStock = skus.stream().map(ProductEntity::getStock).filter(Objects::nonNull).reduce(0, Integer::sum);
    return new ProductSpuAdminResponse(
        spu.getId(),
        spu.getMerchantId(),
        merchant == null ? "" : merchant.getName(),
        spu.getCategoryId(),
        categoryName(spu.getMerchantId(), spu.getCategoryId()),
        spu.getBrandId(),
        brand == null ? "" : brand.getName(),
        spu.getName(),
        spu.getSubtitle(),
        spu.getMainImage(),
        spu.getDetail(),
        readStringList(spu.getDetailImagesJson()),
        spu.getStatus(),
        spu.getSortOrder(),
        skus.size(),
        totalStock,
        minPrice,
        maxPrice,
        includeSkus ? skus.stream().map(this::toProductSkuAdminResponse).toList() : List.of()
    );
  }

  private ProductSkuAdminResponse toProductSkuAdminResponse(ProductEntity sku) {
    List<SkuSpecRequest> specs = readSpecs(sku.getSpecsJson());
    return new ProductSkuAdminResponse(
        sku.getId(),
        sku.getId(),
        sku.getSpuId(),
        sku.getSkuCode(),
        specs,
        specText(specs),
        sku.getPrice(),
        sku.getOriginalPrice(),
        sku.getUnit(),
        sku.getStock(),
        sku.getStatus()
    );
  }

  private ProductAdminResponse toLegacyProductResponse(ProductEntity product, MerchantEntity merchant) {
    return new ProductAdminResponse(
        product.getId(),
        product.getMerchantId(),
        merchant == null ? "" : merchant.getName(),
        product.getCategoryId(),
        product.getName(),
        product.getSales(),
        product.getPrice(),
        product.getOriginalPrice(),
        product.getImageText(),
        product.getUnit(),
        product.getDescription(),
        product.getStock(),
        product.getStatus(),
        product.getSortOrder()
    );
  }

  private CategoryAdminResponse toCategoryResponse(CategoryEntity category) {
    return new CategoryAdminResponse(
        category.getId(),
        category.getName(),
        category.getIcon(),
        category.getParentId(),
        category.getLevel(),
        category.getType(),
        category.getStatus(),
        category.getSortOrder()
    );
  }

  private BrandAdminResponse toBrandResponse(BrandEntity brand) {
    return new BrandAdminResponse(
        brand.getId(),
        brand.getName(),
        brand.getLogo(),
        brand.getDescription(),
        brand.getStatus(),
        brand.getSortOrder()
    );
  }

  private SpecOptionAdminResponse toSpecOptionResponse(SpecOptionEntity option) {
    return new SpecOptionAdminResponse(
        option.getId(),
        option.getGroupId(),
        option.getName(),
        option.getStatus(),
        option.getSortOrder()
    );
  }

  private String categoryName(String merchantId, String categoryId) {
    CategoryEntity category = categoryMapper.selectById(categoryId);
    if (category != null) {
      return category.getName();
    }
    MerchantCategoryEntity merchantCategory = merchantCategoryMapper.selectOne(
        Wrappers.<MerchantCategoryEntity>lambdaQuery()
            .eq(MerchantCategoryEntity::getMerchantId, merchantId)
            .eq(MerchantCategoryEntity::getCategoryId, categoryId)
            .last("limit 1"));
    return merchantCategory == null ? "" : merchantCategory.getName();
  }

  private boolean productCardMatches(ProductCardResponse card, String keyword) {
    return (card.name()
        + " " + card.subtitle()
        + " " + card.description()
        + " " + card.merchantName()
        + " " + card.brandName())
        .toLowerCase(Locale.ROOT)
        .contains(keyword);
  }

  private String normalizeKeyword(String keyword) {
    return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeCommonStatus(String status) {
    String value = isBlank(status) ? ACTIVE : status.trim().toUpperCase(Locale.ROOT);
    if (!List.of(ACTIVE, DISABLED).contains(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 ACTIVE 或 DISABLED");
    }
    return value;
  }

  private String normalizeCategoryType(String type) {
    String value = isBlank(type) ? PRODUCT : type.trim().toUpperCase(Locale.ROOT);
    if (!List.of(CHANNEL, PRODUCT).contains(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "类目类型仅支持 CHANNEL 或 PRODUCT");
    }
    return value;
  }

  private String normalizeSpuStatus(String status) {
    String value = isBlank(status) ? DRAFT : status.trim().toUpperCase(Locale.ROOT);
    if (!List.of(DRAFT, ON_SHELF, OFF_SHELF).contains(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "SPU 状态仅支持 DRAFT、ON_SHELF 或 OFF_SHELF");
    }
    return value;
  }

  private String normalizeSkuStatus(String status) {
    String value = isBlank(status) ? ON_SHELF : status.trim().toUpperCase(Locale.ROOT);
    if (!List.of(ON_SHELF, OFF_SHELF).contains(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 状态仅支持 ON_SHELF 或 OFF_SHELF");
    }
    return value;
  }

  private String normalizeLegacyProductStatus(String status) {
    String value = normalizeSkuStatus(status);
    return ON_SHELF.equals(value) ? ON_SHELF : OFF_SHELF;
  }

  private SkuSpecRequest defaultSkuSpec() {
    return new SkuSpecRequest("spec_capacity", "容量", "spec_capacity_default", "标准装");
  }

  private List<SkuSpecRequest> readSpecs(String json) {
    try {
      if (isBlank(json)) {
        return List.of();
      }
      return objectMapper.readValue(json, SKU_SPEC_TYPE);
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid SKU specs JSON", exception);
    }
  }

  private String writeSpecs(List<SkuSpecRequest> specs) {
    try {
      return objectMapper.writeValueAsString(specs == null ? List.of() : specs);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize SKU specs", exception);
    }
  }

  private List<String> readStringList(String json) {
    try {
      if (isBlank(json)) {
        return List.of();
      }
      return objectMapper.readValue(json, STRING_LIST_TYPE);
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid detail images JSON", exception);
    }
  }

  private String writeStringList(List<String> values) {
    try {
      return objectMapper.writeValueAsString(values == null ? List.of() : values);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize detail images", exception);
    }
  }

  private String specText(List<SkuSpecRequest> specs) {
    return specs == null || specs.isEmpty()
        ? ""
        : specs.stream()
            .map(SkuSpecRequest::optionName)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" / "));
  }

  private String specKey(List<SkuSpecRequest> specs) {
    if (specs == null || specs.isEmpty()) {
      return "";
    }
    return specs.stream()
        .map(item -> value(item.groupId(), "") + ":" + value(item.optionId(), ""))
        .sorted()
        .collect(Collectors.joining("|"));
  }

  private void upsertCategory(CategoryEntity category) {
    if (categoryMapper.selectById(category.getId()) == null) {
      categoryMapper.insert(category);
    } else {
      categoryMapper.updateById(category);
    }
  }

  private void upsertBrand(BrandEntity brand) {
    if (brandMapper.selectById(brand.getId()) == null) {
      brandMapper.insert(brand);
    } else {
      brandMapper.updateById(brand);
    }
  }

  private void upsertSpecGroup(SpecGroupEntity group) {
    if (specGroupMapper.selectById(group.getId()) == null) {
      specGroupMapper.insert(group);
    } else {
      specGroupMapper.updateById(group);
    }
  }

  private void upsertSpecOption(SpecOptionEntity option) {
    if (specOptionMapper.selectById(option.getId()) == null) {
      specOptionMapper.insert(option);
    } else {
      specOptionMapper.updateById(option);
    }
  }

  private void upsertSpu(ProductSpuEntity spu) {
    if (productSpuMapper.selectById(spu.getId()) == null) {
      productSpuMapper.insert(spu);
    } else {
      productSpuMapper.updateById(spu);
    }
  }

  private void upsertSku(ProductEntity sku) {
    if (productMapper.selectById(sku.getId()) == null) {
      productMapper.insert(sku);
    } else {
      productMapper.updateById(sku);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private String value(String value, String fallback) {
    return isBlank(value) ? fallback : value;
  }

  private <T> T value(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private String uid(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
}
