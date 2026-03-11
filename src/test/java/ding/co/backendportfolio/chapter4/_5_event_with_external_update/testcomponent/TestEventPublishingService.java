package ding.co.backendportfolio.chapter4._5_event_with_external_update.testcomponent;

import ding.co.backendportfolio.chapter2.repository.MemberRepository;
import ding.co.backendportfolio.chapter4._3_event_with_lock.entity.EventWithLockParticipant;
import ding.co.backendportfolio.chapter4._3_event_with_lock.repository.EventWithLockRepository;
import ding.co.backendportfolio.chapter4._4_event_with_external.repository.EventWithLockParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestEventPublishingService {
    private final ApplicationEventPublisher eventPublisher;
    private final EventWithLockParticipantRepository participantRepository;
    private final EventWithLockRepository eventRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void publishEventInsideTransaction(Long eventId, Long memberId) {
        log.info("트랜잭션 안에서 이벤트 발행");
        eventPublisher.publishEvent(new TestTransactionEvent(eventId, memberId));
    }

    // 트랜잭션 없음
    public void publishEventOutsideTransaction(Long eventId, Long memberId) {
        log.info("트랜잭션 밖에서 이벤트 발행");
        eventPublisher.publishEvent(new TestTransactionEvent(eventId, memberId));
    }

    @Transactional
    public void publishEventInsideRequiresNew(Long eventId, Long memberId) {
        log.info("외부 트랜잭션에서 내부 REQUIRES_NEW 호출");
        publishInRequiresNew(eventId, memberId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishInRequiresNew(Long eventId, Long memberId) {
        log.info("REQUIRES_NEW 트랜잭션 안에서 이벤트 발행");
        eventPublisher.publishEvent(new TestTransactionEvent(eventId, memberId));
    }

    // 현재 ImprovedEventJoinWithExternalApiUpdateFacade 패턴 시뮬레이션
    public void simulateCurrentPattern(Long eventId, Long memberId) {
        // 1. 내부 트랜잭션으로 DB 작업 (이 안에서는 이벤트 발행 안함)
        doDbWork(eventId, memberId);
        
        // 2. 외부 API 호출 시뮬레이션 (트랜잭션 밖)
        simulateExternalApiCall();
        
        // 3. 다시 트랜잭션으로 업데이트
        doExternalIdUpdate(eventId);
        
        // 4. 여기서 이벤트 발행 - 트랜잭션 컨텍스트 없음!
        log.info("현재 패턴: 트랜잭션 밖에서 이벤트 발행");
        eventPublisher.publishEvent(new TestTransactionEvent(eventId, memberId));
    }

    @Transactional
    public void doDbWork(Long eventId, Long memberId) {
        var event = eventRepository.findById(eventId).orElseThrow();
        var member = memberRepository.findById(memberId).orElseThrow();
        var participant = EventWithLockParticipant.builder()
                .event(event)
                .member(member)
                .build();
        participantRepository.save(participant);
        log.info("DB 작업 완료 (트랜잭션 커밋됨)");
    }

    private void simulateExternalApiCall() {
        try {
            Thread.sleep(100); // 외부 API 호출 시뮬레이션
            log.info("외부 API 호출 완료");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void doExternalIdUpdate(Long eventId) {
        log.info("externalId 업데이트 완료 (별도 트랜잭션)");
    }
}
