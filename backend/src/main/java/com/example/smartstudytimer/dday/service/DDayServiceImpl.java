package com.example.smartstudytimer.dday.service;

import com.example.smartstudytimer.dday.controller.dto.DDayRequest;
import com.example.smartstudytimer.dday.controller.dto.DDayResponse;
import com.example.smartstudytimer.dday.entity.DDay;
import com.example.smartstudytimer.dday.repository.DDayRepository;
import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DDayServiceImpl implements DDayService {

    private final DDayRepository ddayRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public DDayResponse getDDay(Long memberId) {
        DDay dday = ddayRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("설정된 D-Day가 없습니다."));
        return toResponse(dday);
    }

    /** 설정/수정 통합(upsert). 이미 있으면 값만 갱신, 없으면 새로 생성. */
    @Override
    @Transactional
    public DDayResponse setDDay(Long memberId, DDayRequest request) {
        if (request.getTargetDate() == null || request.getLabel() == null || request.getLabel().isBlank()) {
            throw new IllegalArgumentException("targetDate 와 label 은 필수입니다.");
        }

        DDay dday = ddayRepository.findById(memberId).orElse(null);
        if (dday == null) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
            dday = DDay.builder()
                    .member(member)
                    .targetDate(request.getTargetDate())
                    .label(request.getLabel().trim())
                    .build();
            ddayRepository.save(dday);
        } else {
            dday.setTargetDate(request.getTargetDate());
            dday.setLabel(request.getLabel().trim());
            // dirty checking 으로 flush 시 UPDATE
        }
        return toResponse(dday);
    }

    @Override
    @Transactional
    public void deleteDDay(Long memberId) {
        // 없으면 그냥 무시(idempotent). deleteById 는 대상이 없으면 예외를 던지므로 존재 확인 후 삭제.
        if (ddayRepository.existsById(memberId)) {
            ddayRepository.deleteById(memberId);
        }
    }

    private DDayResponse toResponse(DDay dday) {
        long remaining = ChronoUnit.DAYS.between(LocalDate.now(), dday.getTargetDate());
        return DDayResponse.builder()
                .targetDate(dday.getTargetDate())
                .label(dday.getLabel())
                .dDayCount(remaining)
                .build();
    }
}
