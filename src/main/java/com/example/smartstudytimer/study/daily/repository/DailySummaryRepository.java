package com.example.smartstudytimer.study.daily.repository;

import com.example.smartstudytimer.study.daily.Entity.DailySummary;
import com.example.smartstudytimer.study.daily.Entity.DailySummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, DailySummaryId> {
}