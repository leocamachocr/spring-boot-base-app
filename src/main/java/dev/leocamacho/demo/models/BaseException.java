package dev.leocamacho.demo.models;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaseException extends RuntimeException {

    private final int code;
    private final List<String> params;

    public BaseException(String message) {

        super(message);
        code = ErrorCodes.ERROR_NOT_IDENTIFIED;
        params = Collections.emptyList();
    }

    public BaseException(String message, int code, List<String> params) {
        super(message);
        this.code = code;

        this.params = params;
    }

    public static BusinessExceptionBuilder exceptionBuilder() {
        return new BusinessExceptionBuilder();
    }

    public int getCode() {
        return code;
    }

    public static class BusinessExceptionBuilder {
        private int code;
        private String message;
        private List<String> params;

        BusinessExceptionBuilder() {
        }

        public BusinessExceptionBuilder code(int code) {
            this.code = code;
            return this;
        }

        public BusinessExceptionBuilder message(String message) {
            this.message = message;
            return this;
        }

        public BusinessExceptionBuilder param(String params) {
            if (this.params == null) {
                this.params = new ArrayList<>();
            }
            this.params.add(params);
            return this;
        }

        public BusinessExceptionBuilder params(List<String> params) {
            if (this.params == null) {
                this.params = new ArrayList<>();
            }
            this.params.addAll(params);
            return this;
        }

        public BaseException build() {
            if (params == null) {
                params = Collections.emptyList();
            }
            return new BaseException(message, code, params);
        }

        public String toString() {
            return "BusinessException.BusinessExceptionBuilder(code=" + this.code + ", message=" + this.message + ")";
        }
    }
}
