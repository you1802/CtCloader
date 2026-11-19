package you.fileserver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.dto.DownloadBridgePageInfo;
import you.fileserver.dto.entity.DownloadTokenInfo;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.dto.entity.UserAccount;
import you.fileserver.dto.projection.DownloadBridgePageInfoProjection;
import you.fileserver.dto.projection.DownloadFileInfoProjection;
import you.fileserver.exception.TargetFileNotFoundException;
import you.fileserver.repository.DownloadTokenInfoRepository;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;
import you.fileserver.security.authentication.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * DownloadService の単体テスト
 */
@ExtendWith(MockitoExtension.class)
class DownloadServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UploadFileInfoRepository uploadFileInfoRepository;

    @Mock
    private UploadFilePasswordRepository uploadFilePasswordRepository;

    @Mock
    private DownloadTokenInfoRepository downloadTokenInfoRepository;

    @InjectMocks
    private DownloadService downloadService;

    private DownloadFileInfoProjection fileInfo(boolean locked, String owner, boolean visible) {
        // テストごとに使う getter が違うため lenient にする
        DownloadFileInfoProjection projection = mock(DownloadFileInfoProjection.class);
        lenient().when(projection.isDownloadLocked()).thenReturn(locked);
        lenient().when(projection.getOwner()).thenReturn(owner);
        lenient().when(projection.getFilename()).thenReturn("report.pdf");
        lenient().when(projection.isVisible()).thenReturn(visible);
        return projection;
    }

    private void stubPasswordAuthFile(String fileID, boolean locked, String owner, boolean visible) {
        // モックの生成とスタブは when() の引数の中で行わないこと。
        // thenReturn の引数評価中に別の when() を呼ぶと UnfinishedStubbingException になる
        DownloadFileInfoProjection projection = fileInfo(locked, owner, visible);
        when(uploadFileInfoRepository.findByFileID(eq(fileID), eq(DownloadFileInfoProjection.class)))
                .thenReturn(Optional.of(projection));
        when(uploadFilePasswordRepository.findById(fileID))
                .thenReturn(Optional.of(UploadFilePassword.builder()
                        .fileID(fileID)
                        .downloadPassword("hashedDl")
                        .deletePassword("")
                        .build()));
    }

    private static CustomUserDetails userDetails(String username) {
        return new CustomUserDetails(UserAccount.builder()
                .username(username).password("hashed").role("USER").build());
    }

    private UploadFileInfo uploadFileInfo(String fileID, String owner) {
        return UploadFileInfo.builder()
                .fileID(fileID).owner(owner).filename("report.pdf").build();
    }

    private DownloadTokenInfo tokenInfo(String token, String fileID, LocalDateTime expiration) {
        return DownloadTokenInfo.builder()
                .token(token).fileID(fileID).expirationDate(expiration).build();
    }

    private DownloadBridgePageInfoProjection bridgeProjection() {
        // getTransferPageInfo は4 getter 全部、downloadFile は getFilename だけ使うため lenient にする
        DownloadBridgePageInfoProjection projection = mock(DownloadBridgePageInfoProjection.class);
        lenient().when(projection.getFilename()).thenReturn("report.pdf");
        lenient().when(projection.getSize()).thenReturn(123L);
        lenient().when(projection.getOwner()).thenReturn("owner1");
        lenient().when(projection.getComment()).thenReturn("hello");
        return projection;
    }

    private void stubBridgeFile(String fileID) {
        // bridgeProjection() は中で lenient().when() を呼ぶため、thenReturn の引数の中では
        // 呼ばないこと（UnfinishedStubbingException になる）。先に作ってからスタブする
        DownloadBridgePageInfoProjection projection = bridgeProjection();
        when(uploadFileInfoRepository.findByFileID(eq(fileID), eq(DownloadBridgePageInfoProjection.class)))
                .thenReturn(Optional.of(projection));
    }

    // ---------- passwordAuth ----------

    @Test
    void passwordAuth_locked_correctPassword_returnsToken() {
        stubPasswordAuthFile("file1", true, "owner1", true);
        when(passwordEncoder.matches("pw1234", "hashedDl")).thenReturn(true);

        ResponseEntity<?> response = downloadService.passwordAuth("file1", "pw1234", null, "127.0.0.1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(200, body.get("code"));
        assertNotNull(body.get("token"));
        verify(downloadTokenInfoRepository).save(any(DownloadTokenInfo.class));
    }

    @Test
    void passwordAuth_locked_wrongPassword_returns401() {
        stubPasswordAuthFile("file1", true, "owner1", true);
        when(passwordEncoder.matches("wrong", "hashedDl")).thenReturn(false);

        ResponseEntity<?> response = downloadService.passwordAuth("file1", "wrong", null, "127.0.0.1");

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("code"));
        assertEquals("パスワードが一致しません", body.get("message"));
        verify(downloadTokenInfoRepository, never()).save(any());
    }

    @Test
    void passwordAuth_locked_ownerBypassesPasswordAndVisibility() {
        // 所有者は不可視のロック付きファイルでもパスワード・可視性チェックを迂回する
        stubPasswordAuthFile("file1", true, "owner1", false);

        ResponseEntity<?> response = downloadService.passwordAuth(
                "file1", "wrong", userDetails("owner1"), "127.0.0.1");

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(200, body.get("code"));
        assertNotNull(body.get("token"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void passwordAuth_unlocked_noPasswordCheck() {
        stubPasswordAuthFile("file1", false, "owner1", true);

        ResponseEntity<?> response = downloadService.passwordAuth("file1", "", null, "127.0.0.1");

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(200, body.get("code"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void passwordAuth_invisible_locked_nonOwner_returns401() {
        stubPasswordAuthFile("file1", true, "owner1", false);

        ResponseEntity<?> response = downloadService.passwordAuth("file1", "pw1234", null, "127.0.0.1");

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("code"));
        assertEquals("このファイルへのアクセス権がありません", body.get("message"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(downloadTokenInfoRepository, never()).save(any());
    }

    @Test
    void passwordAuth_invisible_unlocked_nonOwner_returns401() {
        // 不可視はロックなしでも所有者以外はアクセス不可
        stubPasswordAuthFile("file1", false, "owner1", false);

        ResponseEntity<?> response = downloadService.passwordAuth("file1", "", null, "127.0.0.1");

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("code"));
        assertEquals("このファイルへのアクセス権がありません", body.get("message"));
        verify(downloadTokenInfoRepository, never()).save(any());
    }

    @Test
    void passwordAuth_fileNotFound_throws() {
        when(uploadFileInfoRepository.findByFileID(eq("missing"), eq(DownloadFileInfoProjection.class)))
                .thenReturn(Optional.empty());

        assertThrows(TargetFileNotFoundException.class,
                () -> downloadService.passwordAuth("missing", "pw", null, "127.0.0.1"));
    }

    @Test
    void passwordAuth_passwordInfoNotFound_throws() {
        DownloadFileInfoProjection projection = fileInfo(true, "owner1", true);
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(DownloadFileInfoProjection.class)))
                .thenReturn(Optional.of(projection));
        when(uploadFilePasswordRepository.findById("file1")).thenReturn(Optional.empty());

        assertThrows(TargetFileNotFoundException.class,
                () -> downloadService.passwordAuth("file1", "pw", null, "127.0.0.1"));
    }

    // ---------- createFileTransferUrl ----------

    @Test
    void createFileTransferUrl_owner_returnsUrl() {
        when(uploadFileInfoRepository.findById("file1"))
                .thenReturn(Optional.of(uploadFileInfo("file1", "owner1")));

        ResponseEntity<?> response = downloadService.createFileTransferUrl("file1", userDetails("owner1"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(200, body.get("code"));
        String url = (String) body.get("URL");
        assertTrue(url.contains("fileID=file1"));
        assertTrue(url.contains("token="));
    }

    @Test
    void createFileTransferUrl_notOwner_returns401() {
        when(uploadFileInfoRepository.findById("file1"))
                .thenReturn(Optional.of(uploadFileInfo("file1", "owner1")));

        ResponseEntity<?> response = downloadService.createFileTransferUrl("file1", userDetails("other"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("code"));
    }

    @Test
    void createFileTransferUrl_fileNotFound_returns401() {
        when(uploadFileInfoRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseEntity<?> response = downloadService.createFileTransferUrl("missing", userDetails("owner1"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("code"));
    }

    @Test
    void createFileTransferUrl_guest_returns401() {
        when(uploadFileInfoRepository.findById("file1"))
                .thenReturn(Optional.of(uploadFileInfo("file1", "owner1")));

        ResponseEntity<?> response = downloadService.createFileTransferUrl("file1", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(401, body.get("code"));
    }

    // ---------- getTransferPageInfo ----------

    @Test
    void getTransferPageInfo_valid_returnsDto() {
        LocalDateTime expiration = LocalDateTime.now().plusDays(1);
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.of(
                tokenInfo("token1", "file1", expiration)));

        DownloadBridgePageInfo pageInfo = downloadService.getTransferPageInfo("file1", "token1");

        assertEquals("report.pdf", pageInfo.getFilename());
        assertEquals(123L, pageInfo.getSize());
        assertEquals("owner1", pageInfo.getOwner());
        assertEquals("hello", pageInfo.getComment());
        assertEquals(expiration, pageInfo.getExpirationDate());
        assertTrue(pageInfo.getDownloadUrl().contains("/api/download"));
        assertTrue(pageInfo.getDownloadUrl().contains("fileID=file1"));
        assertTrue(pageInfo.getDownloadUrl().contains("token=token1"));
        verify(downloadTokenInfoRepository, never()).deleteById(anyString()); // クッション表示では消費しない
    }

    @Test
    void getTransferPageInfo_fileNotFound_throwsGone() {
        when(uploadFileInfoRepository.findByFileID(eq("missing"), eq(DownloadBridgePageInfoProjection.class)))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.getTransferPageInfo("missing", "token1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void getTransferPageInfo_tokenNotFound_throwsGone() {
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.getTransferPageInfo("file1", "token1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void getTransferPageInfo_tokenForOtherFile_throwsGone() {
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.of(
                tokenInfo("token1", "otherFile", LocalDateTime.now().plusDays(1))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.getTransferPageInfo("file1", "token1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void getTransferPageInfo_expiredToken_throwsGone() {
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.of(
                tokenInfo("token1", "file1", LocalDateTime.now().minusMinutes(1))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.getTransferPageInfo("file1", "token1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    // ---------- downloadFile ----------

    @Test
    void downloadFile_fileNotFound_throwsGone() {
        when(uploadFileInfoRepository.findByFileID(eq("missing"), eq(DownloadBridgePageInfoProjection.class)))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.downloadFile("missing", "token1", "127.0.0.1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void downloadFile_tokenNotFound_throwsGone() {
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.downloadFile("file1", "token1", "127.0.0.1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
        verify(uploadFileInfoRepository, never()).updateDownloadCountAndDate(anyString(), any());
    }

    @Test
    void downloadFile_tokenForOtherFile_throwsGone() {
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.of(
                tokenInfo("token1", "otherFile", LocalDateTime.now().plusMinutes(1))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.downloadFile("file1", "token1", "127.0.0.1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void downloadFile_expiredToken_throwsGone() {
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.of(
                tokenInfo("token1", "file1", LocalDateTime.now().minusMinutes(1))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.downloadFile("file1", "token1", "127.0.0.1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void downloadFile_locked_validToken_returns200AndUpdatesCount() {
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.of(
                tokenInfo("token1", "file1", LocalDateTime.now().plusMinutes(1))));

        ResponseEntity<StreamingResponseBody> response =
                downloadService.downloadFile("file1", "token1", "127.0.0.1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("report.pdf"));
        verify(downloadTokenInfoRepository).deleteById("token1");
        verify(uploadFileInfoRepository).updateDownloadCountAndDate(eq("file1"), any(LocalDateTime.class));
    }

    @Test
    void downloadFile_unlocked_validToken_returns200() {
        // 厳密な A：ロックなしでも有効なトークンがあれば落とせる
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("token1")).thenReturn(Optional.of(
                tokenInfo("token1", "file1", LocalDateTime.now().plusMinutes(1))));

        ResponseEntity<StreamingResponseBody> response =
                downloadService.downloadFile("file1", "token1", "127.0.0.1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(downloadTokenInfoRepository).deleteById("token1");
    }

    @Test
    void downloadFile_unlocked_emptyToken_throwsGone() {
        // ロックなしでも空トークンの直リンは通らない
        stubBridgeFile("file1");
        when(downloadTokenInfoRepository.findById("")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.downloadFile("file1", "", "127.0.0.1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    // ---------- adminFileDownload ----------

    @Test
    void adminFileDownload_success() {
        DownloadFileInfoProjection projection = fileInfo(true, "owner1", true);
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(DownloadFileInfoProjection.class)))
                .thenReturn(Optional.of(projection));

        ResponseEntity<StreamingResponseBody> response = downloadService.adminFileDownload("file1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void adminFileDownload_notFound_throwsNotFound() {
        when(uploadFileInfoRepository.findByFileID(eq("missing"), eq(DownloadFileInfoProjection.class)))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> downloadService.adminFileDownload("missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}