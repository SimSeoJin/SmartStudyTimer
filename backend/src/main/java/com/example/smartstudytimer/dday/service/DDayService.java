package com.example.smartstudytimer.dday.service;

import com.example.smartstudytimer.dday.controller.dto.DDayRequest;
import com.example.smartstudytimer.dday.controller.dto.DDayResponse;

public interface DDayService {

    DDayResponse getDDay(Long memberId);

    DDayResponse setDDay(Long memberId, DDayRequest request);

    void deleteDDay(Long memberId);
}
