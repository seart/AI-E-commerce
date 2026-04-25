package com.jingdong.backend.config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;

  public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    addColumn("users", "role", "VARCHAR(40) NOT NULL DEFAULT 'CUSTOMER'");
    addColumn("users", "status", "VARCHAR(40) NOT NULL DEFAULT 'ACTIVE'");
    addColumn("users", "last_login_at", "TIMESTAMP NULL");
    addColumn("merchants", "status", "VARCHAR(40) NOT NULL DEFAULT 'ACTIVE'");
    addColumn("products", "status", "VARCHAR(40) NOT NULL DEFAULT 'ON_SHELF'");
    addColumn("orders", "status_history_json", "TEXT NULL");
    addColumn("orders", "cancel_reason", "VARCHAR(255) NULL");
    addColumn("orders", "refund_reason", "VARCHAR(255) NULL");
    addColumn("orders", "payment_status", "VARCHAR(40) NOT NULL DEFAULT 'PENDING'");
    addColumn("orders", "payment_channel", "VARCHAR(40) NULL");
    addColumn("orders", "paid_at", "TIMESTAMP NULL");
    addColumn("orders", "payment_expire_at", "TIMESTAMP NULL");
    addColumn("orders", "closed_at", "TIMESTAMP NULL");
    createAuditLogs();
    createPayments();
    seedAdmin();
  }

  private void addColumn(String table, String column, String definition) {
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
