package com.project.tracker.internal_expsense_tracker_backend.security;

import com.project.tracker.internal_expsense_tracker_backend.Repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepo.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found"));


        return new User(
                user.getEmail(),
                user.getPassword_hash(),
                List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name())));
        }
    }


}
