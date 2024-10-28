package dev.leocamacho.demo.session;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

public class Session implements Authentication {
    private UUID id;
    private String email;
    private List<String> roles;
    private boolean authenticated;
    private UUID correlationId;

    private Session(UUID id, String email, List<String> roles, boolean authenticated, UUID correlationId) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.authenticated = authenticated;
        this.correlationId = correlationId;
    }

    public UUID correlationId() {
        return correlationId;
    }
    public UUID id() {
        return id;
    }
    public String email() {
        return email;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getId() {
        return id != null ? id.toString() : null;
    }

    public String getRoles() {
        return roles.stream().reduce("", (a, b) -> a + "," + b);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return this;
    }

    @Override
    public Object getPrincipal() {
        return email;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
        return email;
    }

    public static class Builder {
        private UUID id;
        private String email;
        private List<String> roles;
        private boolean authenticated;
        private UUID correlationId;

        public Builder fromMap(Map<String, Object> claims) {
            id = UUID.fromString(claims.get("id").toString());
            email = claims.get("username").toString();
            roles = Arrays.stream(claims.get("roles").toString().split(","))
                    .toList();
            correlationId = UUID.randomUUID();
            return this;
        }

        // generate methods for each field
        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withId(String id) {
            try {
                this.id = UUID.fromString(id);
            } catch (IllegalArgumentException | NullPointerException ex) {
                // ignore
            }
            return this;
        }

        public Builder withCorrelationId(String id) {
            try {
                this.correlationId = UUID.fromString(id);
            } catch (IllegalArgumentException | NullPointerException ex) {
                // ignore
            }
            return this;
        }

        public Builder withCorrelationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Session buildForApiRequest() {
            validateBasics();
            return new Session(id, email, roles, true, UUID.randomUUID());
        }

        public Session buildAnonymous() {
            return new Session(null, "Anonymous", List.of(), false, UUID.randomUUID());
        }

        public Builder withRoles(String roles) {
            this.roles = roles != null ? List.of(roles.split(",")) : List.of();
            return this;
        }

        public Session build() {
            if (id == null) return buildAnonymous();
            validateBasics();
            if (correlationId == null) {
                correlationId = UUID.randomUUID();
            }
            return new Session(id, email, roles, true, correlationId);
        }

        private void validateBasics() {
            if (id == null) {
                throw new IllegalArgumentException("Id is required");
            }
            if (email == null) {
                throw new IllegalArgumentException("Email is required");
            }

        }

    }

}