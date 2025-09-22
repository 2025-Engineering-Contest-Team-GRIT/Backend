package grit.guidance.domain.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import grit.guidance.domain.user.dto.TimetableDetailDto;
import grit.guidance.domain.user.dto.TimetableResponse;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableService {

    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;

    public TimetableResponse getTimetable(String studentId) {
        try {
            // 1. 사용자 조회
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 2. 시간표 조회 및 JSON 파싱
            String timetableJson = user.getTimetable();
            List<TimetableDetailDto> timetable;
            
            if (timetableJson != null && !timetableJson.trim().isEmpty()) {
                timetable = objectMapper.readValue(timetableJson, new TypeReference<List<TimetableDetailDto>>() {});
            } else {
                timetable = List.of(); // 빈 리스트
            }
            
            log.info("시간표 조회 완료: studentId={}, timetableSize={}", studentId, timetable.size());

            return TimetableResponse.success(timetable);

        } catch (Exception e) {
            log.error("시간표 조회 중 오류 발생: studentId={}, error={}", studentId, e.getMessage(), e);
            return TimetableResponse.serverError();
        }
    }
}
