package com.project.tracker.internal_expsense_tracker_backend.Repo;


import com.project.tracker.internal_expsense_tracker_backend.domain.Expense;
import com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepo extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    @Lock (LockModeType.PESSIMISTIC_WRITE)
    @Query ("SELECT e FROM Expense e WHERE e.id = :id")
    Optional<Expense> findByIdWithPessimisticLock(@Param ("id") Long id);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.department.id = :departmentId " +
            "AND e.status = com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseStatus.APPROVED " +
            "AND e.updatedAt >= :startDate AND e.updatedAt <= :endDate")
    BigDecimal calculateApprovedDepartmentSpendForPeriod(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    List<Expense> findByAuthorIdAndCategoryAndCreatedAtBetween(
            Long authorId, ExpenseCategory category, LocalDateTime start, LocalDateTime end);

    List<Expense> findByAuthorIdAndCreatedAtBetween(
            Long authorId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM Expense e WHERE e.riskScore >= :threshold")
    Page<Expense> findFlaggedExpenses(@Param("threshold") int threshold, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.department.id = :deptId AND e.riskScore >= :threshold")
    Page<Expense> findFlaggedExpensesByDepartment(@Param("deptId") Long deptId, @Param("threshold") int threshold, Pageable pageable);

    @Query("SELECT AVG(e.amount) FROM Expense e WHERE e.author.id = :authorId AND e.category = :category AND e.status = 'APPROVED'")
    Double findAverageAmountByAuthorAndCategory(@Param("authorId") Long authorId, @Param("category") ExpenseCategory category);

    @Query("SELECT AVG(e.amount) FROM Expense e WHERE e.category = :category AND e.status = 'APPROVED'")
    Double findAverageAmountByCategory(@Param("category") ExpenseCategory category);


}
