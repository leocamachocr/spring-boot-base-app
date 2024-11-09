package dev.leocamacho.demo.models;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaseException extends RuntimeException {

    private final ErrorCode code;
    private final List<String> params;

    public BaseException(String message) {

        super(message);
        code = ErrorCode.ERROR_NOT_IDENTIFIED;
        params = Collections.emptyList();
    }

    public BaseException(String message, ErrorCode code, List<String> params) {
        super(message);
        this.code = code;

        this.params = params;
    }
    public List<String> getParams() {
        return this.params;
    }

    public static BusinessExceptionBuilder exceptionBuilder() {
        return new BusinessExceptionBuilder();
    }

    public int getCode() {
        return code.code();
    }

    public static class BusinessExceptionBuilder {
        private ErrorCode code;
        private String message;
        private List<String> params;

        BusinessExceptionBuilder() {
        }

        public BusinessExceptionBuilder code(ErrorCode code) {
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
            return new BaseException(message == null ? code.message() : message, code, params);
        }

        public String toString() {
            return "BusinessException.BusinessExceptionBuilder(code=" + this.code + ", message=" + this.message + ")";
        }
    }
}
