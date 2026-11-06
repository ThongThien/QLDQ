package com.qldq.qldqapi.security;

import com.qldq.qldqapi.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserAccountRepository userRepository;

    public DatabaseUserDetailsService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Sai tên đăng nhập hoặc mật khẩu"));
    }
}
