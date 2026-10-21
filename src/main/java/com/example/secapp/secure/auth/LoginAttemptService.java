package com.example.secapp.secure.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 対策版のログイン失敗回数をユーザー名ごとに数え、続けて失敗したユーザー名を一定時間ロックする。
 * <p>
 * パスワードの総当たり（ブルートフォース）を遅らせるための仕組み。存在しないユーザー名も同じように数えるので、
 * 「ロックされた／されない」の違いからユーザー名の存在を推測されない。
 * <p>
 * 教材用の簡易実装のため、カウンタはアプリのメモリに持つ（再起動で消え、複数台構成では共有されない）。
 * 実務では DB や Redis に持たせ、IP アドレス単位の制限や CAPTCHA と組み合わせる。
 */
@Component
public class LoginAttemptService {

    /** この回数だけ続けて失敗するとロックする。 */
    static final int MAX_FAILURES = 5;

    /** ロックの継続時間。失敗回数もこの時間が経つと数え直す。 */
    static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    /** 記録の上限。大量のユーザー名で失敗させてメモリを使い切られないよう、超えたら期限切れの記録を捨てる。 */
    private static final int MAX_ENTRIES = 10_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    /**
     * システム時計を使うインスタンスを作る。
     */
    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    /**
     * テストで時刻を進められるよう、時計を差し替えて作る。
     *
     * @param clock 現在時刻の取得元
     */
    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /**
     * 指定ユーザー名が現在ロック中かを返す。
     *
     * @param username ログインフォームに入力されたユーザー名
     * @return ロック中なら {@code true}
     */
    public boolean isLocked(String username) {
        Attempt a = attempts.get(key(username));
        return a != null && a.failures >= MAX_FAILURES && !isExpired(a);
    }

    /**
     * ログイン失敗を 1 回記録する。
     *
     * @param username ログインフォームに入力されたユーザー名
     */
    public void recordFailure(String username) {
        if (attempts.size() >= MAX_ENTRIES) {
            attempts.values().removeIf(this::isExpired);
        }
        attempts.compute(key(username), (k, a) ->
                (a == null || isExpired(a)) ? new Attempt(1, now()) : new Attempt(a.failures + 1, now()));
    }

    /**
     * ログイン成功時に失敗回数を消す。
     *
     * @param username ログインしたユーザー名
     */
    public void reset(String username) {
        attempts.remove(key(username));
    }

    private boolean isExpired(Attempt a) {
        return now().isAfter(a.lastFailure.plus(LOCK_DURATION));
    }

    private Instant now() {
        return clock.instant();
    }

    /** {@code Alice} と {@code alice} を別々に数えて回数制限をすり抜けられないよう、小文字にそろえる。 */
    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempt(int failures, Instant lastFailure) {
    }
}
