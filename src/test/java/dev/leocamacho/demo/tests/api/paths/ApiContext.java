package dev.leocamacho.demo.tests.api.paths;

public class ApiContext {
    private String token;

    public String token() {
        return token;
    }

    public void token(String token) {
        this.token = token;
    }
}
