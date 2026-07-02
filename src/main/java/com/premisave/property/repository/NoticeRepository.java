package com.premisave.property.repository;

import com.premisave.property.entity.Notice;
import com.premisave.property.enums.NoticeType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends MongoRepository<Notice, String> {

    List<Notice> findByTenantId(String tenantId);

    List<Notice> findByLeaseId(String leaseId);

    List<Notice> findByTenantIdAndNoticeType(String tenantId, NoticeType noticeType);
}