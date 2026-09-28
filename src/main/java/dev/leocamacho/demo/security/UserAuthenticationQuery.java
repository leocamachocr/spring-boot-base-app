package dev.leocamacho.demo.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import dev.leocamacho.demo.persistence.model.UserEntity;
import dev.leocamacho.demo.persistence.repositories.UserRepository;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserAuthenticationQuery implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Optional<UserEntity> user = repository.findByEmail(username);

        var userEntity = user.orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));
            return new AuthenticatedUser(
                    userEntity.getId(),
                    userEntity.getName(),
                    userEntity.getEmail(),
                    userEntity.getPassword(),
                    Collections.emptyList()
                    //  user.get().getRoles().stream().map(SimpleGrantedAuthority::new).toList()
            );

    }
}
