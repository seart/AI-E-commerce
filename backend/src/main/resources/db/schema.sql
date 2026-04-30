CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(64) PRIMARY KEY,
  mobile VARCHAR(20) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(80) NOT NULL,
  member_level VARCHAR(40) NOT NULL,
  role VARCHAR(40) NOT NULL DEFAULT 'CUSTOMER',
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  coupon_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  points INT NOT NULL DEFAULT 0,
  growth_value INT NOT NULL DEFAULT 0,
  last_login_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS banners (
  id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  subtitle VARCHAR(200) NOT NULL,
  background VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  icon VARCHAR(255) NOT NULL,
  parent_id VARCHAR(64) NULL,
  level INT NOT NULL DEFAULT 1,
  type VARCHAR(40) NOT NULL DEFAULT 'CHANNEL',
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brands (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  logo VARCHAR(255) NOT NULL DEFAULT '',
  description VARCHAR(255) NOT NULL DEFAULT '',
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_brands_name (name),
  INDEX idx_brands_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spec_groups (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_spec_groups_name (name),
  INDEX idx_spec_groups_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spec_options (
  id VARCHAR(64) PRIMARY KEY,
  group_id VARCHAR(64) NOT NULL,
  name VARCHAR(80) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_spec_options_group
    FOREIGN KEY (group_id) REFERENCES spec_groups(id)
    ON DELETE CASCADE,
  UNIQUE KEY uk_spec_options_group_name (group_id, name),
  INDEX idx_spec_options_group_sort (group_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchants (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  sales INT NOT NULL DEFAULT 0,
  min_order_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  delivery_minutes INT NOT NULL DEFAULT 0,
  tags_json TEXT NOT NULL,
  description VARCHAR(255) NOT NULL,
  notice VARCHAR(255) NOT NULL,
  rating DECIMAL(3,1) NOT NULL DEFAULT 5.0,
  logo_background VARCHAR(255) NOT NULL,
  logo_text VARCHAR(40) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_spus (
  id VARCHAR(64) PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL,
  category_id VARCHAR(64) NOT NULL,
  brand_id VARCHAR(64) NULL,
  name VARCHAR(180) NOT NULL,
  subtitle VARCHAR(255) NOT NULL DEFAULT '',
  main_image VARCHAR(255) NOT NULL DEFAULT '',
  detail VARCHAR(1000) NOT NULL DEFAULT '',
  detail_images_json TEXT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_spus_merchant
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE,
  INDEX idx_product_spus_merchant (merchant_id),
  INDEX idx_product_spus_category (category_id),
  INDEX idx_product_spus_brand (brand_id),
  INDEX idx_product_spus_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_categories (
  id VARCHAR(64) PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL,
  category_id VARCHAR(64) NOT NULL,
  name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_merchant_categories_merchant
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
  id VARCHAR(64) PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL,
  category_id VARCHAR(64) NOT NULL,
  spu_id VARCHAR(64) NULL,
  brand_id VARCHAR(64) NULL,
  sku_code VARCHAR(80) NOT NULL DEFAULT '',
  specs_json TEXT NULL,
  name VARCHAR(180) NOT NULL,
  sales INT NOT NULL DEFAULT 0,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NOT NULL,
  image_text VARCHAR(80) NOT NULL,
  main_image VARCHAR(255) NOT NULL DEFAULT '',
  unit VARCHAR(40) NOT NULL,
  description VARCHAR(255) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  status VARCHAR(40) NOT NULL DEFAULT 'ON_SHELF',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_merchant
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE,
  INDEX idx_products_merchant_id (merchant_id),
  INDEX idx_products_category_id (category_id),
  INDEX idx_products_spu_id (spu_id),
  INDEX idx_products_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS addresses (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  city VARCHAR(80) NOT NULL,
  district VARCHAR(80) NOT NULL,
  street VARCHAR(120) NOT NULL,
  detail VARCHAR(255) NOT NULL,
  contact_name VARCHAR(80) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  tag VARCHAR(40) NOT NULL,
  is_default TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_addresses_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  INDEX idx_addresses_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_items (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  product_id VARCHAR(64) NOT NULL,
  quantity INT NOT NULL,
  checked TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cart_items_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_cart_items_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE,
  UNIQUE KEY uk_cart_user_product (user_id, product_id),
  INDEX idx_cart_items_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
  id VARCHAR(64) PRIMARY KEY,
  order_no VARCHAR(80) NOT NULL UNIQUE,
  user_id VARCHAR(64) NOT NULL,
  address_id VARCHAR(64) NOT NULL,
  address_city VARCHAR(80) NOT NULL,
  address_district VARCHAR(80) NOT NULL,
  address_street VARCHAR(120) NOT NULL,
  address_detail VARCHAR(255) NOT NULL,
  address_contact_name VARCHAR(80) NOT NULL,
  address_phone VARCHAR(20) NOT NULL,
  address_tag VARCHAR(40) NOT NULL,
  address_is_default TINYINT(1) NOT NULL DEFAULT 0,
  total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(40) NOT NULL,
  status_text VARCHAR(40) NOT NULL,
  payment_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  payment_channel VARCHAR(40) NULL,
  paid_at TIMESTAMP NULL,
  payment_expire_at TIMESTAMP NULL,
  closed_at TIMESTAMP NULL,
  status_history_json TEXT NULL,
  cancel_reason VARCHAR(255) NULL,
  refund_reason VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  INDEX idx_orders_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payments (
  id VARCHAR(64) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  channel VARCHAR(40) NOT NULL,
  status VARCHAR(40) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  out_trade_no VARCHAR(80) NOT NULL UNIQUE,
  transaction_id VARCHAR(120) NULL,
  qr_code VARCHAR(1024) NULL,
  pay_url VARCHAR(1024) NULL,
  gateway_order_no VARCHAR(120) NULL,
  notify_payload TEXT NULL,
  request_id VARCHAR(80) NULL,
  expire_at TIMESTAMP NOT NULL,
  paid_at TIMESTAMP NULL,
  closed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_payments_order
    FOREIGN KEY (order_id) REFERENCES orders(id)
    ON DELETE CASCADE,
  INDEX idx_payments_order_id (order_id),
  INDEX idx_payments_user_id (user_id),
  INDEX idx_payments_status_expire_at (status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
  id VARCHAR(64) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL,
  product_id VARCHAR(64) NOT NULL,
  merchant_id VARCHAR(64) NOT NULL,
  merchant_name VARCHAR(120) NOT NULL,
  category_id VARCHAR(64) NOT NULL,
  name VARCHAR(180) NOT NULL,
  sales INT NOT NULL DEFAULT 0,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NOT NULL,
  image_text VARCHAR(80) NOT NULL,
  unit VARCHAR(40) NOT NULL,
  description VARCHAR(255) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  quantity INT NOT NULL,
  checked TINYINT(1) NOT NULL DEFAULT 1,
  amount DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_order_items_order
    FOREIGN KEY (order_id) REFERENCES orders(id)
    ON DELETE CASCADE,
  INDEX idx_order_items_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
  id VARCHAR(64) PRIMARY KEY,
  actor_id VARCHAR(64) NULL,
  actor_role VARCHAR(40) NULL,
  action VARCHAR(120) NOT NULL,
  target_type VARCHAR(80) NOT NULL,
  target_id VARCHAR(80) NULL,
  detail VARCHAR(1000) NULL,
  request_id VARCHAR(80) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_logs_created_at (created_at),
  INDEX idx_audit_logs_actor_id (actor_id),
  INDEX idx_audit_logs_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
