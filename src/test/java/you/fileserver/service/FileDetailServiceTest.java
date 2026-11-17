package you.fileserver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import you.fileserver.dto.*;
import you.fileserver.dto.entity.FileReport;
import you.fileserver.dto.entity.UserAccount;
import you.fileserver.dto.projection.AdminTargetFileDetailProjection;
import you.fileserver.dto.projection.FileSizeInfoProjection;
import you.fileserver.dto.projection.TargetFileDetailProjection;
import you.fileserver.dto.projection.UserForumFileInfoProjection;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.security.authentication.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * FileDetailService の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class FileDetailServiceTest {

    @Mock
    private UploadFileInfoRepository uploadFileInfoRepository;

    @InjectMocks
    private FileDetailService fileDetailService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 17, 0, 0);

    private static CustomUserDetails userDetails(String username) {
        return new CustomUserDetails(UserAccount.builder()
                .username(username).password("hashed").role("USER").build());
    }

    private UserForumFileInfoProjection forumFile(String fileID, String owner) {
        // テストごとに使う getter が違うため lenient にする
        UserForumFileInfoProjection p = mock(UserForumFileInfoProjection.class);
        lenient().when(p.getFileID()).thenReturn(fileID);
        lenient().when(p.getFilename()).thenReturn(fileID + ".txt");
        lenient().when(p.getSize()).thenReturn(100L);
        lenient().when(p.getUploadDate()).thenReturn(NOW);
        lenient().when(p.getLastDownloadDate()).thenReturn(NOW);
        lenient().when(p.getOwner()).thenReturn(owner);
        lenient().when(p.isDownloadLocked()).thenReturn(false);
        return p;
    }

    private FileSizeInfoProjection fileSize(long size, String owner, boolean visible) {
        // テストごとに使う getter が違うため lenient にする
        FileSizeInfoProjection p = mock(FileSizeInfoProjection.class);
        lenient().when(p.getSize()).thenReturn(size);
        lenient().when(p.getOwner()).thenReturn(owner);
        lenient().when(p.isVisible()).thenReturn(visible);
        return p;
    }

    // ---------- getUserForumFileDetails ----------

    @Test
    void getUserForumFileDetails_guest_onlyVisible() {
        // モックの生成とスタブは when() の引数の中で行わないこと。
        // thenReturn の引数評価中に別の when() を呼ぶと UnfinishedStubbingException になる
        List<UserForumFileInfoProjection> files = List.of(forumFile("file1", "someone"));
        when(uploadFileInfoRepository.findByVisible(eq(true), eq(UserForumFileInfoProjection.class)))
                .thenReturn(files);

        List<ForumFileDetail> result = fileDetailService.getUserForumFileDetails(null);

        assertEquals(1, result.size());
        assertEquals("file1", result.getFirst().getFileID());
        assertFalse(result.getFirst().isOwned()); // ゲストは owned にならない
        verify(uploadFileInfoRepository, never())
                .findByOwnerOrVisible(anyString(), anyBoolean(), any());
    }

    @Test
    void getUserForumFileDetails_loggedIn_ownedFlag() {
        List<UserForumFileInfoProjection> files = List.of(forumFile("mine", "me"), forumFile("theirs", "other"));
        when(uploadFileInfoRepository.findByOwnerOrVisible(
                eq("me"), eq(true), eq(UserForumFileInfoProjection.class)))
                .thenReturn(files);

        List<ForumFileDetail> result = fileDetailService.getUserForumFileDetails(userDetails("me"));

        assertEquals(2, result.size());
        assertTrue(result.get(0).isOwned());
        assertFalse(result.get(1).isOwned());
    }

    // ---------- getTargetFileDetail ----------

    private TargetFileDetailProjection targetDetail(String owner, boolean visible) {
        // テストごとに使う getter が違うため lenient にする
        TargetFileDetailProjection p = mock(TargetFileDetailProjection.class);
        lenient().when(p.getOwner()).thenReturn(owner);
        lenient().when(p.isVisible()).thenReturn(visible);
        lenient().when(p.getComment()).thenReturn("hello");
        lenient().when(p.getDownloadCount()).thenReturn(3);
        return p;
    }

    @Test
    void getTargetFileDetail_notFound_returnsNull() {
        when(uploadFileInfoRepository.findByFileID(eq("missing"), eq(TargetFileDetailProjection.class)))
                .thenReturn(Optional.empty());

        assertNull(fileDetailService.getTargetFileDetail("missing", null));
    }

    @Test
    void getTargetFileDetail_visible_returnsDetailEvenForGuest() {
        TargetFileDetailProjection detail = targetDetail("someone", true);
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(TargetFileDetailProjection.class)))
                .thenReturn(Optional.of(detail));

        TargetFileDetail result = fileDetailService.getTargetFileDetail("file1", null);

        assertNotNull(result);
        assertEquals("hello", result.getComment());
        assertEquals(3, result.getDownloadCount());
    }

    @Test
    void getTargetFileDetail_invisible_owner_returnsDetail() {
        TargetFileDetailProjection detail = targetDetail("me", false);
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(TargetFileDetailProjection.class)))
                .thenReturn(Optional.of(detail));

        assertNotNull(fileDetailService.getTargetFileDetail("file1", userDetails("me")));
    }

    @Test
    void getTargetFileDetail_invisible_nonOwner_returnsNull() {
        TargetFileDetailProjection detail = targetDetail("me", false);
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(TargetFileDetailProjection.class)))
                .thenReturn(Optional.of(detail));

        assertNull(fileDetailService.getTargetFileDetail("file1", userDetails("other")));
    }

    @Test
    void getTargetFileDetail_invisible_guest_returnsNull() {
        TargetFileDetailProjection detail = targetDetail("me", false);
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(TargetFileDetailProjection.class)))
                .thenReturn(Optional.of(detail));

        assertNull(fileDetailService.getTargetFileDetail("file1", null));
    }

    // ---------- getUserOwnedFileDetails ----------

    @Test
    void getUserOwnedFileDetails_allOwned() {
        List<UserForumFileInfoProjection> files = List.of(forumFile("file1", "me"));
        when(uploadFileInfoRepository.findByOwner(eq("me"), eq(UserForumFileInfoProjection.class)))
                .thenReturn(files);

        List<ForumFileDetail> result = fileDetailService.getUserOwnedFileDetails("me");

        assertEquals(1, result.size());
        assertTrue(result.getFirst().isOwned());
        assertEquals("file1", result.getFirst().getFileID());
    }

    // ---------- getAllFileSize ----------

    @Test
    void getAllFileSize_guest_userFileSizeIsMinusOne() {
        List<FileSizeInfoProjection> sizes = List.of(
                fileSize(100, "a", true),
                fileSize(200, "b", true),
                fileSize(50, "me", false));
        when(uploadFileInfoRepository.findAllBy(eq(FileSizeInfoProjection.class)))
                .thenReturn(sizes);

        AllFileSizeInfo result = fileDetailService.getAllFileSize(null);

        assertEquals(300, result.getForumFileSize());
        assertEquals(-1, result.getUserFileSize());
        assertEquals(107374182400L, result.getForumFileSizeLimit());
        assertEquals(5368709120L, result.getUserFileSizeLimit());
    }

    @Test
    void getAllFileSize_loggedIn_sumsOwnInvisibleFilesOnly() {
        List<FileSizeInfoProjection> sizes = List.of(
                fileSize(100, "a", true),
                fileSize(50, "me", false),
                fileSize(999, "other", false),
                fileSize(7, "me", true)); // 可視の自分のファイルは対象外
        when(uploadFileInfoRepository.findAllBy(eq(FileSizeInfoProjection.class)))
                .thenReturn(sizes);

        AllFileSizeInfo result = fileDetailService.getAllFileSize(userDetails("me"));

        assertEquals(107, result.getForumFileSize());
        assertEquals(50, result.getUserFileSize());
    }

    // ---------- ADMIN用 ----------

    @Test
    void getAdminForumFileDetails_delegatesToRepository() {
        List<AdminForumFileDetail> expected = List.of(
                new AdminForumFileDetail("file1", "a.txt", 100, NOW, NOW, "me", false, true, 0));
        when(uploadFileInfoRepository.adminFindAllForumFiles()).thenReturn(expected);

        assertEquals(expected, fileDetailService.getAdminForumFileDetails());
    }

    @Test
    void getAdminTargetFileDetail_present() {
        AdminTargetFileDetailProjection p = mock(AdminTargetFileDetailProjection.class);
        when(p.getComment()).thenReturn("c");
        when(p.getIpAddress()).thenReturn("127.0.0.1");
        when(p.getDownloadCount()).thenReturn(5);
        when(p.getFileReports()).thenReturn(List.of(
                FileReport.builder().comment("spam").ipAddress("10.0.0.1").build()));
        when(uploadFileInfoRepository.findByFileID(eq("file1"), eq(AdminTargetFileDetailProjection.class)))
                .thenReturn(Optional.of(p));

        AdminTargetFileDetail result = fileDetailService.getAdminTargetFileDetail("file1");

        assertNotNull(result);
        assertEquals("c", result.getComment());
        assertEquals("127.0.0.1", result.getIpAddress());
        assertEquals(5, result.getDownloadCount());
        assertEquals(1, result.getFileReports().size());
    }

    @Test
    void getAdminTargetFileDetail_notFound_returnsNull() {
        when(uploadFileInfoRepository.findByFileID(eq("missing"), eq(AdminTargetFileDetailProjection.class)))
                .thenReturn(Optional.empty());

        assertNull(fileDetailService.getAdminTargetFileDetail("missing"));
    }

    @Test
    void getAdminTargetUserFileDetails_delegatesToRepository() {
        List<AdminForumFileDetail> expected = List.of(
                new AdminForumFileDetail("file1", "a.txt", 100, NOW, NOW, "me", false, true, 0));
        when(uploadFileInfoRepository.adminFindByUsernameForumFiles("me")).thenReturn(expected);

        assertEquals(expected, fileDetailService.getAdminTargetUserFileDetails("me"));
    }
}
