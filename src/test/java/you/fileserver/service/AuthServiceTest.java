package you.fileserver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import you.fileserver.dto.AdminUserDetail;
import you.fileserver.dto.DefaultApiResponse;
import you.fileserver.dto.entity.UserAccount;
import you.fileserver.dto.projection.DeleteFileInfoProjection;
import you.fileserver.exception.ServerErrorException;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;
import you.fileserver.repository.UserAccountRepository;
import you.fileserver.security.authentication.CustomUserDetails;
import you.fileserver.utility.FileSizeLimitCalculator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

/**
 * AuthService の単体テスト。
 * リポジトリと PasswordEncoder はモック化し、@IsUser / @IsAdmin の
 * AOP チェックはプレーンな単体テストでは働かないため対象外。
 * 認可そのものは既存の AnnotationTest でカバーされている。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private FileSizeLimitCalculator fileSizeLimitCalculator;

    @Mock
    private UploadFileInfoRepository uploadFileInfoRepository;

    @Mock
    private UploadFilePasswordRepository uploadFilePasswordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private static CustomUserDetails userDetails(String username, String password) {
        return new CustomUserDetails(UserAccount.builder()
                .username(username)
                .password(password)
                .role("USER")
                .build());
    }

    // ---------- register ----------

    @Test
    void register_success() {
        when(fileSizeLimitCalculator.userAccountLimit()).thenReturn(false);
        when(userAccountRepository.findById("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        DefaultApiResponse response = authService.register("testuser", "password123");

        assertEquals(200, response.getCode());
        assertEquals("登録完了しました", response.getMessage());

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals("testuser", captor.getValue().getUsername());
        assertEquals("hashed", captor.getValue().getPassword());
        assertEquals("USER", captor.getValue().getRole());
    }

    @Test
    void register_invalidUsername_returns400() {
        // ユーザー名は4〜32文字のASCII可視文字。3文字はNG
        DefaultApiResponse response = authService.register("abc", "password123");

        assertEquals(400, response.getCode());
        assertEquals("正規表現エラー", response.getMessage());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void register_invalidPassword_returns400() {
        // パスワードは8〜32文字。5文字はNG
        DefaultApiResponse response = authService.register("testuser", "short");

        assertEquals(400, response.getCode());
        assertEquals("正規表現エラー", response.getMessage());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void register_userLimitReached_returns400() {
        when(fileSizeLimitCalculator.userAccountLimit()).thenReturn(true);

        DefaultApiResponse response = authService.register("testuser", "password123");

        assertEquals(400, response.getCode());
        assertEquals("ユーザー数上限に達しました", response.getMessage());
        verify(userAccountRepository, never()).findById(anyString());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_returns400() {
        when(fileSizeLimitCalculator.userAccountLimit()).thenReturn(false);
        when(userAccountRepository.findById("testuser"))
                .thenReturn(Optional.of(UserAccount.builder().username("testuser").build()));

        DefaultApiResponse response = authService.register("testuser", "password123");

        assertEquals(400, response.getCode());
        assertEquals("既に登録されてるユーザー名です", response.getMessage());
        verify(userAccountRepository, never()).save(any());
    }

    // ---------- userNameExists / getUser ----------

    @Test
    void userNameExists_exists() {
        when(userAccountRepository.existsById("testuser")).thenReturn(true);

        assertEquals(Map.of("exists", true), authService.userNameExists("testuser"));
    }

    @Test
    void userNameExists_notExists() {
        when(userAccountRepository.existsById("nobody")).thenReturn(false);

        assertEquals(Map.of("exists", false), authService.userNameExists("nobody"));
    }

    @Test
    void getUser_guest_whenNull() {
        assertEquals(Map.of("user", "ゲスト"), authService.getUser(null));
    }

    @Test
    void getUser_returnsUsername_whenLoggedIn() {
        assertEquals(Map.of("user", "testuser"), authService.getUser(userDetails("testuser", "hashed")));
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_success() {
        CustomUserDetails userDetail = userDetails("testuser", "oldHashed");
        UserAccount account = UserAccount.builder()
                .username("testuser").password("oldHashed").role("USER").build();

        when(passwordEncoder.matches("oldPass123", "oldHashed")).thenReturn(true);
        when(userAccountRepository.findById("testuser")).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("newPass123")).thenReturn("newHashed");

        DefaultApiResponse response = authService.changePassword("oldPass123", "newPass123", userDetail);

        assertEquals(200, response.getCode());
        assertEquals("パスワード変更に成功しました。", response.getMessage());

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals("newHashed", captor.getValue().getPassword());
    }

    @Test
    void changePassword_wrongOldPassword_returns400() {
        CustomUserDetails userDetail = userDetails("testuser", "oldHashed");
        when(passwordEncoder.matches("wrongPass", "oldHashed")).thenReturn(false);

        DefaultApiResponse response = authService.changePassword("wrongPass", "newPass123", userDetail);

        assertEquals(400, response.getCode());
        assertEquals("旧パスワードが一致しません。", response.getMessage());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throwsServerError() {
        CustomUserDetails userDetail = userDetails("ghost", "oldHashed");
        when(passwordEncoder.matches("oldPass123", "oldHashed")).thenReturn(true);
        when(userAccountRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThrows(ServerErrorException.class,
                () -> authService.changePassword("oldPass123", "newPass123", userDetail));
    }

    // ---------- deleteUser / adminDeleteUser / getAdminUserDetails ----------

    @Test
    void deleteUser_deletesOwnedFilesAndAccount() {
        DeleteFileInfoProjection file = mock(DeleteFileInfoProjection.class);
        when(file.getFileID()).thenReturn("file1");
        when(uploadFileInfoRepository.findByOwner(eq("testuser"), eq(DeleteFileInfoProjection.class)))
                .thenReturn(List.of(file));

        authService.deleteUser("testuser");

        verify(uploadFileInfoRepository).deleteById("file1");
        verify(uploadFilePasswordRepository).deleteById("file1");
        verify(userAccountRepository).deleteById("testuser");
    }

    @Test
    void adminDeleteUser_returns200() {
        when(userAccountRepository.findById("testuser")).thenReturn(Optional.of(
                UserAccount.builder().username("testuser").password("hashed").role("USER").build()));
        when(uploadFileInfoRepository.findByOwner(eq("testuser"), eq(DeleteFileInfoProjection.class)))
                .thenReturn(List.of());
        DefaultApiResponse response = authService.adminDeleteUser("testuser");
        assertEquals(200, response.getCode());
        verify(userAccountRepository).deleteById("testuser");
    }

    @Test
    void adminDeleteUser_admin_returns400() {
        when(userAccountRepository.findById("admin")).thenReturn(Optional.of(
                UserAccount.builder().username("admin").password("hashed").role("ADMIN").build()));
        DefaultApiResponse response = authService.adminDeleteUser("admin");
        assertEquals(400, response.getCode());
        assertEquals("管理者用アカウントは削除できません。", response.getMessage());
        verify(uploadFileInfoRepository, never()).findByOwner(anyString(), any());
        verify(userAccountRepository, never()).deleteById(anyString());
    }

    @Test
    void getAdminUserDetails_delegatesToRepository() {
        List<AdminUserDetail> expected = List.of(
                new AdminUserDetail("testuser", LocalDateTime.of(2026, 11, 17, 0, 0)));
        when(userAccountRepository.findAllWithLastLoginAt()).thenReturn(expected);

        assertEquals(expected, authService.getAdminUserDetails());
    }
}
