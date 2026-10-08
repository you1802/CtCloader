package you.fileserver.authentication;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import you.fileserver.dto.entity.UserAccount;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(UserAccount userAccount) implements UserDetails {
    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userAccount.getRole()));
    }

    @Override
    @NonNull
    public String getPassword() {
        return userAccount.getPassword();
    }

    @Override
    @NonNull
    public String getUsername() {
        return userAccount.getUsername();
    }
}
