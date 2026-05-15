package com.jingdong.backend.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
  // 轻量启动迁移：兼容存量表结构，不引入 Flyway。
  private final JdbcTemplate jdbcTemplate;

  public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    // 应用启动时补齐新增字段、索引、表和初始化数据，重复执行保持幂等。
    addColumn("users", "role", "VARCHAR(40) NOT NULL DEFAULT 'CUSTOMER'");
    addColumn("users", "status", "VARCHAR(40) NOT NULL DEFAULT 'ACTIVE'");
    addColumn("users", "last_login_at", "TIMESTAMP NULL");
    addColumn("categories", "parent_id", "VARCHAR(64) NULL");
    addColumn("categories", "level", "INT NOT NULL DEFAULT 1");
    addColumn("categories", "type", "VARCHAR(40) NOT NULL DEFAULT 'CHANNEL'");
    addColumn("categories", "status", "VARCHAR(40) NOT NULL DEFAULT 'ACTIVE'");
    addColumn("categories", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
    addColumn("categories", "updated_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    addColumn("merchants", "status", "VARCHAR(40) NOT NULL DEFAULT 'ACTIVE'");
    addColumn("products", "status", "VARCHAR(40) NOT NULL DEFAULT 'ON_SHELF'");
    addColumn("products", "spu_id", "VARCHAR(64) NULL");
    addColumn("products", "brand_id", "VARCHAR(64) NULL");
    addColumn("products", "sku_code", "VARCHAR(80) NOT NULL DEFAULT ''");
    addColumn("products", "specs_json", "TEXT NULL");
    addColumn("products", "main_image", "VARCHAR(255) NOT NULL DEFAULT ''");
    addColumn("orders", "status_history_json", "TEXT NULL");
    addColumn("orders", "cancel_reason", "VARCHAR(255) NULL");
    addColumn("orders", "refund_reason", "VARCHAR(255) NULL");
    addColumn("orders", "payment_status", "VARCHAR(40) NOT NULL DEFAULT 'PENDING'");
    addColumn("orders", "payment_channel", "VARCHAR(40) NULL");
    addColumn("orders", "paid_at", "TIMESTAMP NULL");
    addColumn("orders", "payment_expire_at", "TIMESTAMP NULL");
    addColumn("orders", "closed_at", "TIMESTAMP NULL");
    addIndex("products", "idx_products_spu_id", "spu_id");
    addIndex("products", "idx_products_status_sort", "status, sort_order");
    createAuditLogs();
    createPayments();
    createProductCenterTables();
    createInventoryTables();
    seedProductCenterDictionaries();
    migrateLegacyProductsToSpu();
    seedInventoryAccounts();
    seedAdmin();
  }

  private void addColumn(String table, String column, String definition) {
    // 先查 information_schema，字段不存在才执行 alter table。
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from information_schema.columns
        where table_schema = database()
          and table_name = ?
          and column_name = ?
        """,
        Integer.class,
        table,
        column
    );
    if (count == null || count == 0) {
      jdbcTemplate.execute("alter table " + table + " add column " + column + " " + definition);
    }
  }

  private void addIndex(String table, String indexName, String columns) {
    // 索引同样按名称幂等创建，避免重复启动报错。
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from information_schema.statistics
        where table_schema = database()
          and table_name = ?
          and index_name = ?
        """,
        Integer.class,
        table,
        indexName
    );
    if (count == null || count == 0) {
      jdbcTemplate.execute("alter table " + table + " add index " + indexName + " (" + columns + ")");
    }
  }

  private void createProductCenterTables() {
    jdbcTemplate.execute("""
        create table if not exists brands (
          id varchar(64) primary key,
          name varchar(120) not null,
          logo varchar(255) not null default '',
          description varchar(255) not null default '',
          status varchar(40) not null default 'ACTIVE',
          sort_order int not null default 0,
          created_at timestamp not null default current_timestamp,
          updated_at timestamp not null default current_timestamp on update current_timestamp,
          unique key uk_brands_name (name),
          index idx_brands_status_sort (status, sort_order)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
    jdbcTemplate.execute("""
        create table if not exists spec_groups (
          id varchar(64) primary key,
          name varchar(80) not null,
          status varchar(40) not null default 'ACTIVE',
          sort_order int not null default 0,
          created_at timestamp not null default current_timestamp,
          updated_at timestamp not null default current_timestamp on update current_timestamp,
          unique key uk_spec_groups_name (name),
          index idx_spec_groups_status_sort (status, sort_order)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
    jdbcTemplate.execute("""
        create table if not exists spec_options (
          id varchar(64) primary key,
          group_id varchar(64) not null,
          name varchar(80) not null,
          status varchar(40) not null default 'ACTIVE',
          sort_order int not null default 0,
          created_at timestamp not null default current_timestamp,
          updated_at timestamp not null default current_timestamp on update current_timestamp,
          constraint fk_spec_options_group foreign key (group_id) references spec_groups(id) on delete cascade,
          unique key uk_spec_options_group_name (group_id, name),
          index idx_spec_options_group_sort (group_id, sort_order)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
    jdbcTemplate.execute("""
        create table if not exists product_spus (
          id varchar(64) primary key,
          merchant_id varchar(64) not null,
          category_id varchar(64) not null,
          brand_id varchar(64) null,
          name varchar(180) not null,
          subtitle varchar(255) not null default '',
          main_image varchar(255) not null default '',
          detail varchar(1000) not null default '',
          detail_images_json text not null,
          status varchar(40) not null default 'DRAFT',
          sort_order int not null default 0,
          created_at timestamp not null default current_timestamp,
          updated_at timestamp not null default current_timestamp on update current_timestamp,
          constraint fk_product_spus_merchant foreign key (merchant_id) references merchants(id) on delete cascade,
          index idx_product_spus_merchant (merchant_id),
          index idx_product_spus_category (category_id),
          index idx_product_spus_brand (brand_id),
          index idx_product_spus_status_sort (status, sort_order)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
  }

  private void createInventoryTables() {
    jdbcTemplate.execute("""
        create table if not exists inventory_accounts (
          sku_id varchar(64) primary key,
          available_quantity int not null default 0,
          locked_quantity int not null default 0,
          sold_quantity int not null default 0,
          version int not null default 0,
          updated_at timestamp not null default current_timestamp on update current_timestamp,
          constraint fk_inventory_accounts_sku foreign key (sku_id) references products(id) on delete cascade,
          index idx_inventory_accounts_available (available_quantity),
          index idx_inventory_accounts_updated_at (updated_at)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
    jdbcTemplate.execute("""
        create table if not exists inventory_transactions (
          id varchar(64) primary key,
          sku_id varchar(64) not null,
          order_id varchar(64) null,
          order_item_id varchar(64) null,
          biz_type varchar(60) not null,
          biz_id varchar(120) not null,
          direction varchar(40) not null,
          quantity int not null,
          before_available int not null default 0,
          after_available int not null default 0,
          before_locked int not null default 0,
          after_locked int not null default 0,
          before_sold int not null default 0,
          after_sold int not null default 0,
          reason varchar(255) not null default '',
          request_id varchar(80) null,
          created_at timestamp not null default current_timestamp,
          constraint fk_inventory_transactions_sku foreign key (sku_id) references products(id) on delete cascade,
          unique key uk_inventory_transactions_biz_sku (biz_type, biz_id, sku_id),
          index idx_inventory_transactions_sku_created (sku_id, created_at),
          index idx_inventory_transactions_order (order_id),
          index idx_inventory_transactions_biz_type (biz_type)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
  }

  private void seedProductCenterDictionaries() {
    jdbcTemplate.update("""
        insert ignore into brands (id, name, logo, description, status, sort_order)
        values ('brand_jd', '自营', '', '平台自营品牌', 'ACTIVE', 10)
        """);
    jdbcTemplate.update("""
        insert ignore into spec_groups (id, name, status, sort_order)
        values ('spec_capacity', '容量', 'ACTIVE', 10)
        """);
    jdbcTemplate.update("""
        insert ignore into spec_options (id, group_id, name, status, sort_order)
        values ('spec_capacity_default', 'spec_capacity', '标准装', 'ACTIVE', 10)
        """);
  }

  private void migrateLegacyProductsToSpu() {
    List<Map<String, Object>> products = jdbcTemplate.queryForList("""
        select id, merchant_id, category_id, name, description, image_text, status, sort_order
        from products
        where spu_id is null or spu_id = ''
           or specs_json is null or specs_json = ''
        """);
    for (Map<String, Object> product : products) {
      String productId = (String) product.get("id");
      String spuId = "spu_" + productId;
      jdbcTemplate.update("""
          insert ignore into product_spus (
            id, merchant_id, category_id, brand_id, name, subtitle, main_image,
            detail, detail_images_json, status, sort_order
          )
          values (?, ?, ?, 'brand_jd', ?, ?, ?, ?, '[]', ?, ?)
          """,
          spuId,
          product.get("merchant_id"),
          product.get("category_id"),
          product.get("name"),
          product.get("description"),
          product.get("image_text"),
          product.get("description"),
          product.get("status"),
          product.get("sort_order")
      );
      jdbcTemplate.update("""
          update products
          set spu_id = ?, brand_id = 'brand_jd', sku_code = ?, specs_json = ?,
              main_image = case when main_image = '' then image_text else main_image end
          where id = ?
          """,
          spuId,
          productId,
          "[{\"groupId\":\"spec_capacity\",\"groupName\":\"容量\",\"optionId\":\"spec_capacity_default\",\"optionName\":\"标准装\"}]",
          productId
      );
    }
  }

  private void seedInventoryAccounts() {
    jdbcTemplate.update("""
        insert ignore into inventory_accounts (
          sku_id, available_quantity, locked_quantity, sold_quantity, version
        )
        select id, greatest(stock, 0), 0, 0, 0
        from products
        """);
  }

  private void createAuditLogs() {
    jdbcTemplate.execute("""
        create table if not exists audit_logs (
          id varchar(64) primary key,
          actor_id varchar(64) null,
          actor_role varchar(40) null,
          action varchar(120) not null,
          target_type varchar(80) not null,
          target_id varchar(80) null,
          detail varchar(1000) null,
          request_id varchar(80) null,
          created_at timestamp not null default current_timestamp,
          index idx_audit_logs_created_at (created_at),
          index idx_audit_logs_actor_id (actor_id),
          index idx_audit_logs_action (action)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
  }

  private void createPayments() {
    jdbcTemplate.execute("""
        create table if not exists payments (
          id varchar(64) primary key,
          order_id varchar(64) not null,
          user_id varchar(64) not null,
          channel varchar(40) not null,
          status varchar(40) not null,
          amount decimal(10,2) not null,
          out_trade_no varchar(80) not null unique,
          transaction_id varchar(120) null,
          qr_code varchar(1024) null,
          pay_url varchar(1024) null,
          gateway_order_no varchar(120) null,
          notify_payload text null,
          request_id varchar(80) null,
          expire_at timestamp not null,
          paid_at timestamp null,
          closed_at timestamp null,
          created_at timestamp not null default current_timestamp,
          updated_at timestamp not null default current_timestamp on update current_timestamp,
          index idx_payments_order_id (order_id),
          index idx_payments_user_id (user_id),
          index idx_payments_status_expire_at (status, expire_at)
        ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
        """);
  }

  private void seedAdmin() {
    jdbcTemplate.update("""
        update users
        set role = 'CUSTOMER', status = 'ACTIVE'
        where id = 'u_demo'
        """);
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from users where mobile = ?",
        Integer.class,
        "13900000000"
    );
    if (count != null && count > 0) {
      jdbcTemplate.update("""
          update users
          set role = 'ADMIN', status = 'ACTIVE', nickname = '运营管理员', member_level = '平台管理员'
          where mobile = '13900000000'
          """);
      return;
    }

    jdbcTemplate.update("""
        insert into users (
          id, mobile, password, nickname, member_level, role, status,
          coupon_count, favorite_count, points, growth_value
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        List.of(
            "u_admin",
            "13900000000",
            "123456",
            "运营管理员",
            "平台管理员",
            "ADMIN",
            "ACTIVE",
            0,
            0,
            0,
            0
        ).toArray()
    );
  }
}
