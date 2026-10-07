package com.example.smartstudytimer.dday.repository;

import com.example.smartstudytimer.dday.entity.DDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DDayRepository extends JpaRepository<DDay, Long> {
}
