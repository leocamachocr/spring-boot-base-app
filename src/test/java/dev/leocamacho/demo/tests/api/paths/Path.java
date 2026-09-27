package dev.leocamacho.demo.tests.api.paths;

public class Path {

    enum Type {
        PUBLIC,
        PRIVATE
    }

    private String path;

    private Path(String path, Type type) {
        this.path = "/api/" + (type.equals(Type.PUBLIC) ? "public" : "private") + path;
    }

    public String path() {
        return path;
    }

    static class Public {
        static class User {
            static final Path REGISTER = new Path("/register", Type.PUBLIC);
            static final Path LOGIN = new Path("/login", Type.PUBLIC);
        }
    }

    static class Private {
        static class User {
            static final Path LOGGED_USER = new Path("/users/current", Type.PRIVATE);
        }
    }

}
