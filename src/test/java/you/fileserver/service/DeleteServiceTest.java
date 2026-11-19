package you.fileserver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import you.fileserver.dto.DefaultApiResponse;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.dto.entity.UserAccount;
import you.fileserver.dto.projection.DeleteFileInfoProjection;
import you.fileserver.exception.DeleteFailureException;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;
import you.fileserver.security.authentication.CustomUserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * DeleteService の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class DeleteServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UploadFilePasswordRepository uploadFilePasswordRepository;

    @Mock
    private UploadFileInfoRepository uploadFileInfoRepository;

    @InjectMocks
    private DeleteService deleteService;

    private DeleteFileInfoProjection fileInfo(String fileID, String owner) {
        // テストごとに使う getter が違うため lenient にする
        DeleteFileInfoProjection projection = mock(DeleteFileInfoProjection.class);
        lenient().when(projection.getFileID()).thenReturn(fileID);
        lenient().when(projection.getOwner()).thenReturn(owner);
        return projection;
    }

    private void stubFileExists(String fileID, String owner) {
        // モックの生成とスタブは when() の引数の中で行わないこと。
        // thenReturn の引数評価中に別の when() を呼ぶと UnfinishedStubbingException になる
        DeleteFileInfoProjection projection = fileInfo(fileID, owner);
        when(uploadFileInfoRepository.findByFileID(eq(fileID), eq(DeleteFileInfoProjection.class)))
                .thenReturn(Optional.of(projection));
        when(uploadFilePasswordRepository.findById(fileID))
                .thenReturn(Optional.of(UploadFilePassword.builder()
                        .fileID(fileID)
                        .deletePassword("hashedDelete")
                        .downloadPassword("")
                        .build()));
    }

    private static CustomUserDetails userDetails(String username) {
        return new CustomUserDetails(UserAccount.builder()
                .username(username).password("hashed").role("USER").build());
    }

    // ---------- ゲストアップロードのファイル (owner が空文字) ----------

    @Test
    void deleteFile_guestFile_correctPassword_success() {
        stubFileExists("file1", "");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        DefaultApiResponse response = deleteService.deleteFile("file1", "abcd1234", null, "127.0.0.1");

        assertEquals(200, response.getCode());
        assertEquals("削除に成功しました。", response.getMessage());
        verify(uploadFileInfoRepository).deleteById("file1");
        verify(uploadFilePasswordRepository).deleteById("file1");
    }

    @Test
    void deleteFile_guestFile_wrongPassword_throws() {
        stubFileExists("file1", "");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(DeleteFailureException.class,
                () -> deleteService.deleteFile("file1", "wrongpass", null, "127.0.0.1"));
        verify(uploadFileInfoRepository, never()).deleteById(anyString());
    }

    // ------------ 実物の BCryptPasswordEncoder を使ったテスト ------------
    @Test
    void deleteFile_guestFile_realEncoder_passwordMatches() {
        PasswordEncoder realEncoder = new BCryptPasswordEncoder();
        DeleteService serviceWithRealEncoder = new DeleteService(
                realEncoder, uploadFilePasswordRepository, uploadFileInfoRepository);

        DeleteFileInfoProjection projection = fileInfo("file1", "");
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(DeleteFileInfoProjection.class)))
                .thenReturn(Optional.of(projection));
        when(uploadFilePasswordRepository.findById("file1"))
                .thenReturn(Optional.of(UploadFilePassword.builder()
                        .fileID("file1")
                        .deletePassword(realEncoder.encode("abcd1234"))
                        .downloadPassword("")
                        .build()));

        DefaultApiResponse response = serviceWithRealEncoder.deleteFile("file1", "abcd1234", null, "127.0.0.1");

        assertEquals(200, response.getCode());
    }

    // ---------- ログイン中アップロードのファイル (owner あり) ----------

    @Test
    void deleteFile_ownerLoggedIn_success() {
        stubFileExists("file1", "owner1");

        DefaultApiResponse response = deleteService.deleteFile(
                "file1", "ignored", userDetails("owner1"), "127.0.0.1");

        assertEquals(200, response.getCode());
        verify(uploadFileInfoRepository).deleteById("file1");
    }

    @Test
    void deleteFile_nonOwnerLoggedIn_throws() {
        stubFileExists("file1", "owner1");

        assertThrows(DeleteFailureException.class,
                () -> deleteService.deleteFile("file1", "ignored", userDetails("other"), "127.0.0.1"));
        verify(uploadFileInfoRepository, never()).deleteById(anyString());
    }

    @Test
    void deleteFile_ownerFile_guest_throws() {
        stubFileExists("file1", "owner1");

        assertThrows(DeleteFailureException.class,
                () -> deleteService.deleteFile("file1", "ignored", null, "127.0.0.1"));
        verify(uploadFileInfoRepository, never()).deleteById(anyString());
    }

    // ---------- ファイル情報なし ----------

    @Test
    void deleteFile_fileInfoNotFound_throws() {
        when(uploadFileInfoRepository.findByFileID(eq("missing"), eq(DeleteFileInfoProjection.class)))
                .thenReturn(Optional.empty());

        assertThrows(DeleteFailureException.class,
                () -> deleteService.deleteFile("missing", "abcd1234", null, "127.0.0.1"));
    }

    @Test
    void deleteFile_passwordInfoMissing_deletesFileInfoThenThrows() {
        // パスワード情報だけ無い場合、実装はファイル情報を削除してから例外を投げる
        DeleteFileInfoProjection projection = fileInfo("file1", "");
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(DeleteFileInfoProjection.class)))
                .thenReturn(Optional.of(projection));
        when(uploadFilePasswordRepository.findById("file1")).thenReturn(Optional.empty());

        assertThrows(DeleteFailureException.class,
                () -> deleteService.deleteFile("file1", "abcd1234", null, "127.0.0.1"));
        verify(uploadFileInfoRepository).deleteById("file1");
        verify(uploadFilePasswordRepository).deleteById("file1");
    }

    // ---------- ADMIN用 ----------

    @Test
    void adminDeleteFile_success() {
        when(uploadFileInfoRepository.existsById("file1")).thenReturn(true);

        DefaultApiResponse response = deleteService.adminDeleteFile("file1");

        assertEquals(200, response.getCode());
        verify(uploadFileInfoRepository).deleteById("file1");
        verify(uploadFilePasswordRepository).deleteById("file1");
    }

    @Test
    void adminDeleteFile_notExists_throws() {
        when(uploadFileInfoRepository.existsById("missing")).thenReturn(false);

        assertThrows(DeleteFailureException.class, () -> deleteService.adminDeleteFile("missing"));
        verify(uploadFileInfoRepository, never()).deleteById(anyString());
    }
}
