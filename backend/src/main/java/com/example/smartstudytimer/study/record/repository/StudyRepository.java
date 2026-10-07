package com.example.smartstudytimer.study.record.repository;

import com.example.smartstudytimer.study.record.Entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRepository extends JpaRepository<StudyRecord, Integer> {
}