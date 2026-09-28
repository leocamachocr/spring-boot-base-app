package dev.leocamacho.demo.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unchecked business exception, thrown only to roll back an operation already in progress.
 * Expected business outcomes are modeled as handler {@code Result} variants instead.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<Object> params;

    public BusinessException(ErrorCode errorCode, Object... params) {
        super(format(errorCode.message(), Arrays.asList(params)));
        this.errorCode = errorCode;
        this.params = Arrays.asList(params);
    }

    private BusinessException(Builder builder) {
        this(builder.errorCode, builder.params.toArray());
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<Object> getParams() {
        return params;
    }

    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode);
    }

    private static String format(String template, List<Object> params) {
        String message = template;
        for (int i = 0; i < params.size(); i++) {
            message = message.replace("{" + i + "}", String.valueOf(params.get(i)));
        }
        return message;
    }

    public static final class Builder {
        private final ErrorCode errorCode;
        private final List<Object> params = new ArrayList<>();

        private Builder(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }

        public Builder param(Object value) {
            params.add(value);
            return this;
        }

        public BusinessException build() {
            return new BusinessException(this);
        }
    }
}
