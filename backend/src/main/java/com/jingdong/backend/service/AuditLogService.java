package com.jingdong.backend.service;

import com.jingdong.backend.auth.UserContext;
import com.jingdong.backend.entity.DataEntities.AuditLogEntity;
import com.jingdong.backend.mapper.AuditLogMapper;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
  // 审计日志服务，后台高危操作和关键用户行为都从这里落库。
  private final AuditLogMapper auditLogMapper;

  public AuditLogService(AuditLogMapper auditLogMapper) {
    this.auditLogMapper = auditLogMapper;
  }

  public void record(String action, String targetType, String targetId, String detail) {
    AuditLogEntity log = new AuditLogEntity();
    log.setId("audit_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    try {
      log.setActorId(UserContext.userId());
      log.setActorRole(UserContext.role());
    } catch (Exception ignored) {
      log.setActorId(null);
      log.setActorRole(null);
    }
    log.setAction(action);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDetail(detail);
    log.setRequestId(MDC.get("requestId"));
    auditLogMapper.insert(log);
  }
}
