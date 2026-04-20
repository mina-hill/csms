package com.csms.csms.repository;

import com.csms.csms.entity.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, UUID> {

    Optional<SalaryPayment> findByWorkerIdAndPeriodYearAndPeriodMonth(UUID workerId, Integer periodYear, Integer periodMonth);

    List<SalaryPayment> findByPeriodYearAndPeriodMonth(Integer periodYear, Integer periodMonth);

    List<SalaryPayment> findByPeriodYear(Integer periodYear);

    List<SalaryPayment> findAllByOrderByPeriodYearDescPeriodMonthDescProcessedAtDesc();
}
