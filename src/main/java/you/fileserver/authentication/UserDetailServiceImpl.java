package you.fileserver.authentication;

import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import you.fileserver.dto.entity.UserAccount;
import you.fileserver.repository.UserAccountRepository;

import static you.fileserver.config.Constants.AUTH_USER;

@Component
public class UserDetailServiceImpl implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDetailServiceImpl(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return new CustomUserDetails(account);
    }

    public boolean saveUserAccount(String username, String password) {
        if (userAccountRepository.findById(username).isPresent()) {
            return false;
        }

        UserAccount account = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(AUTH_USER)
                .build();
        userAccountRepository.save(account);
        return true;
    }
}
