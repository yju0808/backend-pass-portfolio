package ding.co.backendportfolio.chapter4._5_event_with_external_update.testcomponent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class TestTransactionalEventListener {
    private final AtomicInteger afterCommitCount = new AtomicInteger(0);
    private volatile CountDownLatch currentLatch;

    public void reset() {
        afterCommitCount.set(0);
        currentLatch = new CountDownLatch(1);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(TestTransactionEvent event) {
        log.info("🔔 AFTER_COMMIT 리스너 호출됨! eventId={}", event.getEventId());
        afterCommitCount.incrementAndGet();
        if (currentLatch != null) {
            currentLatch.countDown();
        }
    }

    public int getAfterCommitCount() {
        return afterCommitCount.get();
    }

    public boolean waitForEvent(long timeout, TimeUnit unit) throws InterruptedException {
        return currentLatch != null && currentLatch.await(timeout, unit);
    }
}
