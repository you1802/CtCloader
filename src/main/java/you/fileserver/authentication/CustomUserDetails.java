package you.fileserver.authentication;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import you.fileserver.dto.entity.UserAccount;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(UserAccount userAccount) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userAccount.getRole()));
    }

    @Override
    public @Nullable String getPassword() {
        return userAccount.getPassword();
    }

    @Override
    public String getUsername() {
        return userAccount.getUsername();
    }
}
