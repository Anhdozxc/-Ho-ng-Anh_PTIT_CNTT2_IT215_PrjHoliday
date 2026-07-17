package vn.edu.ptit.holidayplanner.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import vn.edu.ptit.holidayplanner.repository.UserAccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAccountRepository repository;

    public CustomUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = repository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Email hoặc mật khẩu không đúng"));
        return User.withUsername(account.getEmail())
                .password(account.getPassword())
                .roles(account.getRole().name())
                .disabled(account.getStatus() == UserStatus.LOCKED)
                .build();
    }
}
