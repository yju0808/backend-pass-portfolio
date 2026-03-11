package ding.co.backendportfolio.chapter4._5_event_with_external_update;

import ding.co.backendportfolio.chapter2.entity.Member;
import ding.co.backendportfolio.chapter2.repository.MemberRepository;
import ding.co.backendportfolio.chapter4._3_event_with_lock.entity.EventWithLock;
import ding.co.backendportfolio.chapter4._3_event_with_lock.repository.EventWithLockRepository;
import ding.co.backendportfolio.chapter4._5_event_with_external_update.testcomponent.TestEventPublishingService;
import ding.co.backendportfolio.chapter4._5_event_with_external_update.testcomponent.TestTransactionalEventListener;
import ding.co.backendportfolio.chapter4.fixture.Chapter4Fixture;
import ding.co.backendportfolio.config.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이 테스트는 @TransactionalEventListener(AFTER_COMMIT)와 트랜잭션 경계의 
 * 복잡한 상호작용을 검증합니다.
 * 
 * 핵심 고민:
 * 1. 트랜잭션 밖에서 이벤트를 발행하면 AFTER_COMMIT 리스너가 동작할까?
 * 2. REQUIRES_NEW로 내부 트랜잭션만 커밋되면 어떤 일이 발생할까?
 */
@Slf4j
@IntegrationTest
@Import({TestEventPublishingService.class, TestTransactionalEventListener.class})
class TransactionalEventListenerIssueTest {

    @Autowired
    private EventWithLockRepository eventRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TestEventPublishingService testService;
    @Autowired
    private TestTransactionalEventListener testListener;

    private EventWithLock testEvent;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testListener.reset();
        testEvent = eventRepository.save(
                Chapter4Fixture.createEventWithLock("테스트 이벤트", 100)
        );
        testMember = memberRepository.save(
                Chapter4Fixture.createTestMember("테스트유저")
        );
    }

    @Test
    @DisplayName("시나리오1: 트랜잭션 안에서 이벤트 발행 - AFTER_COMMIT 정상 동작")
    void scenario1_publishInsideTransaction_afterCommitWorks() throws InterruptedException {
        // when
        testService.publishEventInsideTransaction(testEvent.getId(), testMember.getId());
        
        // AFTER_COMMIT은 비동기로 동작할 수 있으므로 대기
        boolean received = testListener.waitForEvent(2, TimeUnit.SECONDS);

        // then
        assertThat(received).isTrue();
        assertThat(testListener.getAfterCommitCount()).isEqualTo(1);
        log.info("✅ 시나리오1 통과: 트랜잭션 안에서 발행한 이벤트는 AFTER_COMMIT 정상 동작");
    }

    @Test
    @DisplayName("시나리오2: 트랜잭션 밖에서 이벤트 발행 - AFTER_COMMIT 동작 안함!")
    void scenario2_publishOutsideTransaction_afterCommitDoesNotWork() throws InterruptedException {
        // when - 트랜잭션 없이 이벤트 발행
        testService.publishEventOutsideTransaction(testEvent.getId(), testMember.getId());
        
        // AFTER_COMMIT 리스너가 호출되지 않음을 확인 (잠시 대기 후)
        boolean received = testListener.waitForEvent(2, TimeUnit.SECONDS);

        // then
        assertThat(received).isFalse();
        assertThat(testListener.getAfterCommitCount()).isEqualTo(0);
        log.info("⚠️ 시나리오2 확인: 트랜잭션 밖에서 발행한 이벤트는 AFTER_COMMIT 리스너가 동작하지 않음!");
    }

    @Test
    @DisplayName("시나리오3: REQUIRES_NEW 내부 트랜잭션에서 발행 - 내부 커밋 기준으로 동작")
    void scenario3_publishInsideRequiresNew_worksOnInnerCommit() throws InterruptedException {
        // when
        testService.publishEventInsideRequiresNew(testEvent.getId(), testMember.getId());
        
        boolean received = testListener.waitForEvent(2, TimeUnit.SECONDS);

        // then
        assertThat(received).isTrue();
        assertThat(testListener.getAfterCommitCount()).isEqualTo(1);
        log.info("✅ 시나리오3 통과: REQUIRES_NEW 내부 트랜잭션 커밋 시점에 AFTER_COMMIT 동작");
    }

    @Test
    @DisplayName("시나리오4: 현재 ImprovedEventJoinWithExternalApiUpdateFacade 패턴 검증")
    void scenario4_currentPatternSimulation() throws InterruptedException {
        // 현재 패턴: 
        // 1. 내부 서비스(REQUIRES_NEW 또는 별도 트랜잭션)로 DB 작업
        // 2. 외부 API 호출 (트랜잭션 밖)
        // 3. 다시 서비스로 externalId 업데이트
        // 4. 이벤트 발행 <-- 이 시점에 트랜잭션이 없음!

        // when
        testService.simulateCurrentPattern(testEvent.getId(), testMember.getId());
        
        boolean received = testListener.waitForEvent(2, TimeUnit.SECONDS);

        // then - AFTER_COMMIT 리스너는 동작하지 않음
        log.info("현재 패턴에서 AFTER_COMMIT 수신 여부: {}", received);
        log.info("AFTER_COMMIT 호출 횟수: {}", testListener.getAfterCommitCount());
        
        // 현재 코드는 @EventListener를 사용하므로 동작하지만,
        // @TransactionalEventListener(AFTER_COMMIT)을 쓰면 동작하지 않음
        assertThat(testListener.getAfterCommitCount())
                .as("트랜잭션 밖에서 발행하면 AFTER_COMMIT 리스너는 동작하지 않음")
                .isEqualTo(0);
    }
}
