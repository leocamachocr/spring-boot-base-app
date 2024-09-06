package dev.leocamacho.demo.models;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record AuthenticatedUser(
        UUID id,
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {


    public Map<String, Object> toMap() {
        Map<String, Object> user = new HashMap<>();

        user.put("id", id.toString());
        user.put("email", getUsername());
        user.put("roles", getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")));
        return user;

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}