package you.fileserver.security.authentication;

import org.jspecify.annotations.NonNull;
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

    public UserDetailServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return new CustomUserDetails(account);
    }
}
