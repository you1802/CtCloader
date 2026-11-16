package you.fileserver.security.authentication;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import you.fileserver.dto.entity.UserAccount;
import you.fileserver.repository.UserAccountRepository;

@Component
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return new CustomUserDetails(account);
    }
}
