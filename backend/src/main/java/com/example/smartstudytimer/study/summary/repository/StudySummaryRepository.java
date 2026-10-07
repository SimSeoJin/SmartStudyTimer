package com.example.smartstudytimer.study.summary.repository;

import com.example.smartstudytimer.study.summary.Entity.StudySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudySummaryRepository extends JpaRepository<StudySummary, Long> {
}