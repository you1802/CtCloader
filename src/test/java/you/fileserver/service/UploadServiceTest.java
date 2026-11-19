package you.fileserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.upload.UploadInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.dto.entity.UserAccount;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;
import you.fileserver.security.authentication.CustomUserDetails;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UploadService の単体テスト
 * 正常系は実際に files/ 配下へファイルを書き込むため、@AfterEach で作成したファイルを削除している。
 */
@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private TusFileUploadService tusFileUploadService;

    @Mock
    private UploadFileInfoRepository uploadFileInfoRepository;

    @Mock
    private UploadFilePasswordRepository uploadFilePasswordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private UploadInfo uploadInfo;

    private UploadService uploadService;

    @BeforeEach
    void setUp() throws Exception {
        // UploadService のフィールドは private final + コンストラクタ注入のため、
        // モックをコンストラクタで渡す（引数の順序はフィールド宣言順と同じ）
        uploadService = new UploadService(tusFileUploadService, uploadFileInfoRepository, uploadFilePasswordRepository, passwordEncoder);

        Files.createDirectories(Path.of("files"));
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Path.of("files", "abc123"));
        Files.deleteIfExists(Path.of("files", "def456"));
    }

    private void stubCompletedUpload(String uri, Map<String, String> metadata) throws Exception {
        when(request.getRequestURI()).thenReturn(uri);
        when(tusFileUploadService.getUploadInfo(uri)).thenReturn(uploadInfo);
        when(uploadInfo.isUploadInProgress()).thenReturn(false);
        when(uploadInfo.getMetadata()).thenReturn(metadata);
    }

    // 正常系でのみ使うスタブ。正規表現チェックで早期 return するテストでは
    // これらのメソッドは呼ばれないため、別メソッドに分けてある
    // (Mockito の strict stubs だと不要なスタブで失敗する)
    private void stubUploadBytes(String uri) throws Exception {
        when(uploadInfo.getFileName()).thenReturn("my file.txt");
        when(uploadInfo.getLength()).thenReturn(123L);
        when(tusFileUploadService.getUploadedBytes(uri))
                .thenReturn(new ByteArrayInputStream("hello".getBytes()));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    // sanitizeFilename の分岐を確認するための、ファイル名だけ変えられる版
    private void stubUploadBytesWithFilename(String uri, String filename) throws Exception {
        when(uploadInfo.getFileName()).thenReturn(filename);
        when(uploadInfo.getLength()).thenReturn(123L);
        when(tusFileUploadService.getUploadedBytes(uri))
                .thenReturn(new ByteArrayInputStream("hello".getBytes()));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    private UploadFileInfo uploadAsGuestWithFilename(String uri, String filename) throws Exception {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("downloadPasswordEnabled", "false");
        metadata.put("deletePassword", "abcd1234");
        stubCompletedUpload(uri, metadata);
        stubUploadBytesWithFilename(uri, filename);
        when(passwordEncoder.encode("abcd1234")).thenReturn("hashedDelete");

        uploadService.upload(request, response, null);

        ArgumentCaptor<UploadFileInfo> infoCaptor = ArgumentCaptor.forClass(UploadFileInfo.class);
        verify(uploadFileInfoRepository).save(infoCaptor.capture());
        return infoCaptor.getValue();
    }

    private static CustomUserDetails userDetails(String username) {
        return new CustomUserDetails(UserAccount.builder()
                .username(username).password("hashed").role("USER").build());
    }

    @Test
    void upload_inProgress_doesNothingButCleanup() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/upload/abc123");
        when(tusFileUploadService.getUploadInfo("/api/upload/abc123")).thenReturn(uploadInfo);
        when(uploadInfo.isUploadInProgress()).thenReturn(true);

        uploadService.upload(request, response, null);

        verify(uploadFileInfoRepository, never()).save(any());
        verify(tusFileUploadService).deleteUpload("/api/upload/abc123");
    }

    @Test
    void upload_noUploadInfo_doesNothingButCleanup() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/upload/abc123");
        when(tusFileUploadService.getUploadInfo("/api/upload/abc123")).thenReturn(null);

        uploadService.upload(request, response, null);

        verify(uploadFileInfoRepository, never()).save(any());
        verify(tusFileUploadService).deleteUpload("/api/upload/abc123");
    }

    @Test
    void upload_guest_invalidDeletePassword_returnsEarly() throws Exception {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("downloadPasswordEnabled", "false");
        metadata.put("deletePassword", "ab"); // 4文字未満で正規表現NG
        stubCompletedUpload("/api/upload/abc123", metadata);

        uploadService.upload(request, response, null);

        verify(uploadFileInfoRepository, never()).save(any());
        verify(uploadFilePasswordRepository, never()).save(any());
    }

    @Test
    void upload_downloadPasswordEnabled_invalidPassword_returnsEarly() throws Exception {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("downloadPasswordEnabled", "true");
        metadata.put("deletePassword", "abcd1234");
        metadata.put("downloadPassword", "x"); // 4文字未満で正規表現NG
        stubCompletedUpload("/api/upload/abc123", metadata);

        uploadService.upload(request, response, null);

        verify(uploadFileInfoRepository, never()).save(any());
        verify(uploadFilePasswordRepository, never()).save(any());
    }

    @Test
    void upload_guest_success() throws Exception {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("downloadPasswordEnabled", "false");
        metadata.put("deletePassword", "abcd1234");
        metadata.put("comment", "hello");
        stubCompletedUpload("/api/upload/abc123", metadata);
        stubUploadBytes("/api/upload/abc123");
        when(passwordEncoder.encode("abcd1234")).thenReturn("hashedDelete");

        uploadService.upload(request, response, null);

        assertTrue(Files.exists(Path.of("files", "abc123")));

        ArgumentCaptor<UploadFileInfo> infoCaptor = ArgumentCaptor.forClass(UploadFileInfo.class);
        verify(uploadFileInfoRepository).save(infoCaptor.capture());
        UploadFileInfo saved = infoCaptor.getValue();
        assertEquals("abc123", saved.getFileID());
        assertEquals("my-file.txt", saved.getFilename()); // スペースは - に変換される
        assertEquals("", saved.getOwner()); // ゲストは owner が空文字
        assertEquals(123L, saved.getSize());
        assertTrue(saved.isVisible()); // ゲストは常に可視
        assertFalse(saved.isDownloadLocked());
        assertEquals("hello", saved.getComment());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals(0, saved.getDownloadCount());

        ArgumentCaptor<UploadFilePassword> passCaptor = ArgumentCaptor.forClass(UploadFilePassword.class);
        verify(uploadFilePasswordRepository).save(passCaptor.capture());
        assertEquals("hashedDelete", passCaptor.getValue().getDeletePassword());
        assertEquals("", passCaptor.getValue().getDownloadPassword());
        verify(tusFileUploadService).deleteUpload("/api/upload/abc123");
    }

    @Test
    void upload_loggedIn_withDownloadLock_success() throws Exception {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("downloadPasswordEnabled", "true");
        metadata.put("deletePassword", "abcd1234");
        metadata.put("downloadPassword", "dlpass12");
        metadata.put("comment", "secret");
        stubCompletedUpload("/api/upload/def456", metadata);
        stubUploadBytes("/api/upload/def456");
        when(passwordEncoder.encode("dlpass12")).thenReturn("hashedDl");

        uploadService.upload(request, response, userDetails("user1"));

        ArgumentCaptor<UploadFileInfo> infoCaptor = ArgumentCaptor.forClass(UploadFileInfo.class);
        verify(uploadFileInfoRepository).save(infoCaptor.capture());
        UploadFileInfo saved = infoCaptor.getValue();
        assertEquals("def456", saved.getFileID());
        assertEquals("user1", saved.getOwner());
        assertTrue(saved.isVisible()); // visibleSetting なしなら公開
        assertTrue(saved.isDownloadLocked());

        ArgumentCaptor<UploadFilePassword> passCaptor = ArgumentCaptor.forClass(UploadFilePassword.class);
        verify(uploadFilePasswordRepository).save(passCaptor.capture());
        assertEquals("", passCaptor.getValue().getDeletePassword()); // ログイン中は削除パス不要
        assertEquals("hashedDl", passCaptor.getValue().getDownloadPassword());
    }

    @Test
    void upload_loggedIn_invisible_downloadLockIgnored() throws Exception {
        // 非公開ファイルでは downloadPasswordEnabled が読まれず、ロックは無効になる
        Map<String, String> metadata = new HashMap<>();
        metadata.put("downloadPasswordEnabled", "true");
        metadata.put("deletePassword", "abcd1234");
        metadata.put("downloadPassword", "dlpass12");
        metadata.put("visibleSetting", "true"); // 不可視設定
        metadata.put("comment", "secret");
        stubCompletedUpload("/api/upload/def456", metadata);
        stubUploadBytes("/api/upload/def456");

        uploadService.upload(request, response, userDetails("user1"));

        ArgumentCaptor<UploadFileInfo> infoCaptor = ArgumentCaptor.forClass(UploadFileInfo.class);
        verify(uploadFileInfoRepository).save(infoCaptor.capture());
        UploadFileInfo saved = infoCaptor.getValue();
        assertEquals("def456", saved.getFileID());
        assertEquals("user1", saved.getOwner());
        assertFalse(saved.isVisible()); // visibleSetting=true なら不可視
        assertFalse(saved.isDownloadLocked()); // 非公開ではロックが無効になる

        ArgumentCaptor<UploadFilePassword> passCaptor = ArgumentCaptor.forClass(UploadFilePassword.class);
        verify(uploadFilePasswordRepository).save(passCaptor.capture());
        assertEquals("", passCaptor.getValue().getDeletePassword()); // ログイン中は削除パス不要
        assertEquals("", passCaptor.getValue().getDownloadPassword()); // 非公開ではパス不要
        verify(passwordEncoder, never()).encode(anyString());
    }

    // ---------- sanitizeFilename ----------

    @Test
    void upload_filename_null_savesUnnamed() throws Exception {
        UploadFileInfo saved = uploadAsGuestWithFilename("/api/upload/abc123", null);
        assertEquals("unnamed", saved.getFilename());
    }

    @Test
    void upload_filename_blank_savesUnnamed() throws Exception {
        UploadFileInfo saved = uploadAsGuestWithFilename("/api/upload/abc123", "   ");
        assertEquals("unnamed", saved.getFilename());
    }

    @Test
    void upload_filename_controlChars_removed() throws Exception {
        UploadFileInfo saved = uploadAsGuestWithFilename("/api/upload/abc123", "a\u0001b\nc.txt");
        assertEquals("abc.txt", saved.getFilename());
    }

    @Test
    void upload_filename_onlyControlChars_savesUnnamed() throws Exception {
        UploadFileInfo saved = uploadAsGuestWithFilename("/api/upload/abc123", "\u0001\u0002");
        assertEquals("unnamed", saved.getFilename());
    }

    @Test
    void upload_filename_tooLong_truncatedTo255() throws Exception {
        String longName = "a".repeat(300) + ".txt";
        UploadFileInfo saved = uploadAsGuestWithFilename("/api/upload/abc123", longName);
        assertEquals(255, saved.getFilename().length());
        assertTrue(saved.getFilename().startsWith("aaa"));
    }
}