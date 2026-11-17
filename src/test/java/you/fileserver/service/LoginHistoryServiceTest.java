package you.fileserver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import you.fileserver.dto.UserLoginHistory;
import you.fileserver.dto.entity.LoginHistory;
import you.fileserver.repository.LoginHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LoginHistoryService の単体テスト
 */
@ExtendWith(MockitoExtension.class)
class LoginHistoryServiceTest {

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @InjectMocks
    private LoginHistoryService loginHistoryService;

    @Test
    void recordLogin_savesHistory() {
        LocalDateTime before = LocalDateTime.now();

        loginHistoryService.recordLogin("testuser", true, "127.0.0.1");

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());
        LoginHistory saved = captor.getValue();
        assertEquals("testuser", saved.getUsername());
        assertTrue(saved.isSuccess());
        assertEquals("127.0.0.1", saved.getIp());
        assertNotNull(saved.getLoginAt());
        assertFalse(saved.getLoginAt().isBefore(before));
    }

    @Test
    void recordLogin_failure_savesSuccessFalse() {
        loginHistoryService.recordLogin("testuser", false, "10.0.0.1");

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());
        assertFalse(captor.getValue().isSuccess());
    }

    @Test
    void getLoginHistory_delegatesToRepository() {
        List<UserLoginHistory> expected = List.of(
                new UserLoginHistory(LocalDateTime.of(2026, 11, 17, 0, 0), "127.0.0.1", true));
        when(loginHistoryRepository.findTop10ByUsernameOrderByLoginAtDesc("testuser", UserLoginHistory.class))
                .thenReturn(expected);

        assertEquals(expected, loginHistoryService.getLoginHistory("testuser"));
    }

    @Test
    void getAdminLonginHistory_delegatesToGetLoginHistory() {
        List<UserLoginHistory> expected = List.of(
                new UserLoginHistory(LocalDateTime.of(2026, 11, 17, 0, 0), "127.0.0.1", true));
        when(loginHistoryRepository.findTop10ByUsernameOrderByLoginAtDesc("testuser", UserLoginHistory.class))
                .thenReturn(expected);

        assertEquals(expected, loginHistoryService.getAdminLonginHistory("testuser"));
    }
}