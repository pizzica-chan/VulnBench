package com.example.secapp.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文字列を UTF-8 で表したときのバイト数の上限を検査する制約。
 * <p>
 * BCrypt は入力の先頭 72 バイトしか使わず、Spring Security の {@code BCryptPasswordEncoder} は
 * 72 バイトを超える入力のハッシュ化で例外を投げる。{@code @Size} は文字数しか見ないため、
 * 日本語（1 文字 3 バイト）のパスワードは 25 文字で上限を超える。そこでバイト数で検査する。
 * {@code null} は検査対象外（{@code @NotBlank} と組み合わせる）。
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    /**
     * 許容する最大バイト数。
     *
     * @return UTF-8 での最大バイト数
     */
    int value();

    String message() default "UTF-8 で {value} バイト以内にしてください（日本語は 1 文字 3 バイト）";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
