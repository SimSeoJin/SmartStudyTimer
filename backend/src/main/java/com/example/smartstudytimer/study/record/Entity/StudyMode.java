package com.example.smartstudytimer.study.record.Entity;

/**
 * 공부 기록의 측정 방식. 앱의 StudyMode와 이름이 같아야 한다.
 * - PURE: 순공 모드(카메라로 얼굴을 확인한 시간) → 랭킹·티어에 반영
 * - BASIC: 일반 타이머(수동 시작/정지) → 개인 기록에만 반영
 */
public enum StudyMode {
    PURE,
    BASIC
}
