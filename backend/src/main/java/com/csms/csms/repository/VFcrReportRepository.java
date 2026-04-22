package com.csms.csms.repository;

import com.csms.csms.entity.VFcrReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VFcrReportRepository extends JpaRepository<VFcrReport, UUID> {
}