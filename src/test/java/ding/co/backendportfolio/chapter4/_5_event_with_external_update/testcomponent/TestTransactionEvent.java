package ding.co.backendportfolio.chapter4._5_event_with_external_update.testcomponent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TestTransactionEvent {
    private final Long eventId;
    private final Long memberId;
}
