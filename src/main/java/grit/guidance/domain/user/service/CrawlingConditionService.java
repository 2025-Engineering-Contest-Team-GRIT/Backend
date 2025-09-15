package grit.guidance.domain.user.service;

import grit.guidance.domain.user.entity.Users;
import grit.guidance.global.common.SystemData;
import grit.guidance.global.common.SystemDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingConditionService {

    private final SystemDataRepository systemDataRepository;

    /**
     * 크롤링이 필요한지 확인
     * @param user 기존 사용자 (null이면 신규 사용자)
     * @return 크롤링 필요 여부
     */
    public boolean shouldCrawl(Users user) {
//        // 1. DB에 없던 유저가 로그인 시 -> 크롤링 필요
//        if (user == null) {
//            log.info("신규 사용자 로그인 - 크롤링 필요");
//            return true;
//        }
//
//        // 2. 기존 유저 로그인 시
//        LocalDateTime now = LocalDateTime.now();
//
//        // 2-1. last_crawl_time 확인 후 한 달 이상 지났으면 크롤링
//        LocalDateTime userLastCrawl = user.getLastCrawlTime();
//        if (userLastCrawl != null) {
//            long daysSinceLastCrawl = ChronoUnit.DAYS.between(userLastCrawl, now);
//            if (daysSinceLastCrawl >= 30) {
//                log.info("사용자 마지막 크롤링으로부터 {}일 경과 - 크롤링 필요", daysSinceLastCrawl);
//                return true;
//            }
//        } else {
//            // last_crawl_time이 null이면 크롤링 필요 (기존 데이터가 없음)
//            log.info("사용자 last_crawl_time이 null - 크롤링 필요");
//            return true;
//        }
//
//        // 2-2. system_data 테이블의 global_update_time과 비교
//        SystemData systemData = getSystemData();
//        if (systemData != null) {
//            LocalDateTime globalUpdateTime = systemData.getGlobalUpdateTime();
//            if (globalUpdateTime != null && globalUpdateTime.isAfter(userLastCrawl)) {
//                log.info("글로벌 업데이트 시간({})이 사용자 마지막 크롤링({})보다 최근 - 크롤링 필요", globalUpdateTime, userLastCrawl);
//                return true;
//            }
//        }
//
//        log.info("크롤링 불필요 - 기존 데이터 사용");
//        return false;
        return true;
    }

    /**
     * SystemData 조회 (없으면 기본값으로 생성)
     */
    private SystemData getSystemData() {
        return systemDataRepository.findAll().stream()
            .findFirst()
            .orElseGet(() -> {
                log.info("SystemData가 없어서 기본값으로 생성");
                return systemDataRepository.save(SystemData.builder().build());
            });
    }
}
