package com.example.secapp.secure.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LoginAttemptService} のロックの条件と解除のタイミングを検証する。
 */
class LoginAttemptServiceTest {

    /** テストから時刻を進められる時計。 */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service = new LoginAttemptService(clock);

    private void fail(String username, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(username);
        }
    }

    /**
     * 成功時の担保: 上限の回数に達するまではロックせず、達したらロックする。
     */
    @Test
    void locksOnlyAfterMaxFailures() {
        fail("alice", LoginAttemptService.MAX_FAILURES - 1);
        assertThat(service.isLocked("alice")).isFalse();

        fail("alice", 1);
        assertThat(service.isLocked("alice")).isTrue();
    }

    /**
     * 成功時の担保: ロックは一定時間で解け、失敗回数も数え直しになる。
     */
    @Test
    void lockExpiresAfterDuration() {
        fail("alice", LoginAttemptService.MAX_FAILURES);
        clock.advance(LoginAttemptService.LOCK_DURATION.plusSeconds(1));

        assertThat(service.isLocked("alice")).isFalse();
        fail("alice", 1);
        assertThat(service.isLocked("alice")).isFalse();
    }

    /**
     * 成功時の担保: 大文字と小文字を変えても、同じユーザー名として数える（制限のすり抜け防止）。
     */
    @Test
    void countsUsernameCaseInsensitively() {
        fail("Alice", 2);
        fail("ALICE", 2);
        fail("alice", 1);

        assertThat(service.isLocked("aLiCe")).isTrue();
    }

    /**
     * 成功時の担保: ログインに成功すると失敗回数が消え、別のユーザー名には影響しない。
     */
    @Test
    void resetClearsOnlyThatUser() {
        fail("alice", LoginAttemptService.MAX_FAILURES);
        fail("bob", LoginAttemptService.MAX_FAILURES);

        service.reset("alice");

        assertThat(service.isLocked("alice")).isFalse();
        assertThat(service.isLocked("bob")).isTrue();
    }
}
