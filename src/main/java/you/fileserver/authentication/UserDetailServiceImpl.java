package you.fileserver.authentication;

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
    private final PasswordEncoder passwordEncoder;

    public UserDetailServiceImpl(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return new CustomUserDetails(account);
    }

    /**
     * ユーザーアカウントとハッシュ化したパスワードをDBに登録する
     * @param username ユーザーネーム
     * @param password パスワード
     * @return 既に使われてる名前ならfalse,登録できたならtrue
     */
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
