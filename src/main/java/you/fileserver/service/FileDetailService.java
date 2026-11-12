package you.fileserver.service;

import jakarta.annotation.Nullable;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Service;
import you.fileserver.dto.ForumFileDetail;
import you.fileserver.dto.TargetFileDetail;
import you.fileserver.dto.projection.TargetFileDetailProjection;
import you.fileserver.dto.projection.UserForumFileInfoProjection;
import you.fileserver.security.annotation.IsAdmin;
import you.fileserver.security.annotation.IsUser;
import you.fileserver.security.authentication.CustomUserDetails;
import you.fileserver.dto.AllFileSizeInfo;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.projection.FileSizeInfoProjection;
import you.fileserver.repository.UploadFileInfoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static you.fileserver.config.Constants.*;

/**
 * ユーザーにかえすファイルリスト関係のサービスクラス
 */
@Service
@EnableMethodSecurity
public class FileDetailService {
    private final UploadFileInfoRepository uploadFileInfoRepository;

    public FileDetailService(UploadFileInfoRepository uploadFileInfoRepository) {
        this.uploadFileInfoRepository = uploadFileInfoRepository;
    }

    /**
     * ユーザーに表示するファイル掲示板のリストを取得する
     * @param userDetail ログインユーザー情報(NULL可)
     * @return 掲示板用ファイルリスト
     */
    public List<ForumFileDetail> getUserForumFileDetails(@Nullable CustomUserDetails userDetail) {
        Optional<CustomUserDetails> userDetailOpt = Optional.ofNullable(userDetail); //userDetailをオプショナルでラップ
        List<ForumFileDetail> forumFileList = new ArrayList<>(); //掲示板用のファイル詳細リスト

        //オプショナルのメソッドを使って必要な情報をリポジトリから取り出す
        List<UserForumFileInfoProjection> userForumFileList = userDetailOpt
                .map(user -> uploadFileInfoRepository.findByOwnerOrVisible(user.getUsername(), true, UserForumFileInfoProjection.class)) //ログイン中なら自分のファイル+可視設定に応じて
                .orElseGet(() -> uploadFileInfoRepository.findByVisible(true, UserForumFileInfoProjection.class)); //非ログインなら可視設定のみ

        for (UserForumFileInfoProjection forumFileInfo : userForumFileList) {
            //ファイルが自分のアップロードしたものかの判定
            boolean owned = userDetailOpt
                    .map(user -> user.getUsername().equals(forumFileInfo.getOwner()))
                    .orElse(false);

            //ユーザーに送る情報をリストに格納
            forumFileList.add(ForumFileDetail.builder()
                    .fileID(forumFileInfo.getFileID())
                    .filename(forumFileInfo.getFilename())
                    .size(forumFileInfo.getSize())
                    .uploadDate(forumFileInfo.getUploadDate())
                    .lastDownloadDate(forumFileInfo.getLastDownloadDate())
                    .downloadLocked(forumFileInfo.isDownloadLocked())
                    .owned(owned)
                    .build());
        }
        return forumFileList;
    }

    /**
     * 対象のファイルの詳細情報を返す
     * @param fileID ファイルid
     * @param userDetail ログイン中ユーザー情報(NULL可)
     * @return 対象ファイルの情報
     */
    public TargetFileDetail getTargetFileDetail(String fileID, @Nullable CustomUserDetails userDetail) {
        Optional<CustomUserDetails> userDetailOpt = Optional.ofNullable(userDetail); //userDetailをオプショナルでラップ

        TargetFileDetailProjection fileDetail = uploadFileInfoRepository.findByFileID(fileID, TargetFileDetailProjection.class).orElse(null); //DBから対象ファイルの必要な情報を取得

        if (fileDetail == null) return null; //対象ファイルが存在しない場合はNULLを返す
        if (fileDetail.isVisible()) { //対象ファイルが可視設定の場合
            return TargetFileDetail.builder() //情報をビルドしてかえす
                    .comment(fileDetail.getComment())
                    .downloadCount(fileDetail.getDownloadCount())
                    .build();
        }

        //以下不可視設定の場合、ログイン中かどうかの分岐
        return userDetailOpt.map(user -> { //ログイン中の場合
            if (fileDetail.getOwner().equals(user.getUsername())) { //ファイルの所有者の場合
                return TargetFileDetail.builder() //情報をビルドしてかえす
                        .comment(fileDetail.getComment())
                        .downloadCount(fileDetail.getDownloadCount())
                        .build();
            }
            return null; //ファイル所有者でない場合NULLをかえす
        }).orElse(null); //ログイン中でない場合NULLをかえす
    }

    /**
     * 自身のアップロードしたファイルの詳細を取得する(ログイン中ユーザー専用)
     * @param username ログイン中ユーザー名
     * @return ファイルリスト
     */
    @IsUser
    public List<ForumFileDetail> getUserOwnedFileDetails(String username) {
        //ユーザーが自分でアップロードしたファイルリストを取得
        List<UserForumFileInfoProjection> ownedUploadFiles = uploadFileInfoRepository.findByOwner(username, UserForumFileInfoProjection.class);

        List<ForumFileDetail> fileDetailList = new ArrayList<>(); //ユーザーに送るデータリスト

        //必要な情報をビルドしてリストに格納
        for (UserForumFileInfoProjection userFileInfo : ownedUploadFiles) {
            fileDetailList.add(ForumFileDetail.builder()
                    .fileID(userFileInfo.getFileID())
                    .size(userFileInfo.getSize())
                    .filename(userFileInfo.getFilename())
                    .uploadDate(userFileInfo.getUploadDate())
                    .downloadLocked(userFileInfo.isDownloadLocked())
                    .owned(true)
                    .lastDownloadDate(userFileInfo.getLastDownloadDate())
                    .build());
        }
        return fileDetailList;
    }

    /**
     * 掲示板のファイルサイズ合計とログインユーザーの不可視設定のファイルサイズ合計をかえす
     * @param userDetail ログインユーザー
     * @return DTO
     */
    public AllFileSizeInfo getAllFileSize(@Nullable CustomUserDetails userDetail) {
        Optional<CustomUserDetails> userDetailOpt = Optional.ofNullable(userDetail); //userDetailをオプショナルでラップ
        List<FileSizeInfoProjection> fileSizeInfoList = uploadFileInfoRepository.findAllBy(FileSizeInfoProjection.class); //ファイルサイズ情報をリポジトリから取得

        //ユーザーのアップロードした不可視設定のファイルの合計サイズを計算
        long userFileSize = userDetailOpt
                .map(user -> fileSizeInfoList.stream().filter(file -> !file.isVisible() && file.getOwner().equals(user.getUsername())).mapToLong(FileSizeInfoProjection::getSize).sum()) //ログイン中なら不可視のファイルかつ自分のものの合計サイズ)
                .orElse(-1L); //非ログインなら-1

        return AllFileSizeInfo.builder()
                .forumFileSizeLimit(FORUM_FILESIZE_LIMIT) //掲示板サイズ限界
                .userFileSizeLimit(USER_FILESIZE_LIMIT) //ユーザーごとのサイズ限界
                .forumFileSize(fileSizeInfoList.stream().filter(FileSizeInfoProjection::isVisible).mapToLong(FileSizeInfoProjection::getSize).sum()) //現在の掲示板のファイルサイズ合計、ファイル全体から可視化オンのものを抜き出してサイズを合計
                .userFileSize(userFileSize)
                .build();
    }

    /**
     * ADMIN用ファイル詳細情報
     * @return DTO
     */
    @IsAdmin
    public List<UploadFileInfo> adminFileDetails() {
        return uploadFileInfoRepository.findAll();
    }

//    //テスト用
//    public List<FileDetail> test(){
//        List<FileDetail> fileDetails = new ArrayList<>();
//
//        for (inti = 0; i < 100; i++) {
//            fileDetails.add(FileDetail.builder()
//                            .fileID("uniqueFile" + i)
//                            .size(i * 1000)
//                            .owned(false)
//                            .name("name" + i)
//                            .downloadLock(false)
//                            .uploadDate(LocalDateTime.now().minusSeconds(i))
//                    .build());
//        }
//        return fileDetails;
//    }
}
