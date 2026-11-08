package you.fileserver.service;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Service;
import you.fileserver.security.annotation.IsAdmin;
import you.fileserver.security.authentication.CustomUserDetails;
import you.fileserver.dto.AllFileSizeInfo;
import you.fileserver.dto.FileDetail;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.projection.FileSizeInfoProjection;
import you.fileserver.dto.projection.UserUploadFileInfoProjection;
import you.fileserver.repository.UploadFileInfoRepository;

import java.time.LocalDateTime;
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
     * ユーザーに提示するファイルリストの情報を返す
     * @return ファイルリスト
     */
    public List<FileDetail> userFileDetails(CustomUserDetails userDetail) {
        Optional<CustomUserDetails> userDetailOpt = Optional.ofNullable(userDetail); //userDetailをオプショナルでラップ

        //オプショナルのメソッドを使って必要な情報をリポジトリから取り出す
        List<UserUploadFileInfoProjection> userUploadFileList = userDetailOpt
                .map(user -> uploadFileInfoRepository.findByOwnerOrVisible(user.getUsername(), true, UserUploadFileInfoProjection.class)) //ログイン中なら自分のファイル+可視設定に応じて
                .orElseGet(() -> uploadFileInfoRepository.findByVisible(true, UserUploadFileInfoProjection.class)); //非ログインなら可視設定のみ

        List<FileDetail> fileDetailList = new ArrayList<>(); //ユーザーに送るファイルリスト

        for (UserUploadFileInfoProjection userFileInfo : userUploadFileList) {
            //ファイルが自分のアップロードしたものかどうかの判定
            boolean owned = userDetailOpt
                    .map(user -> user.getUsername().equals(userFileInfo.getOwner()))
                    .orElse(false);

            //ユーザーに送る情報をリストに格納
            fileDetailList.add(FileDetail.builder()
                    .fileID(userFileInfo.getFileID())
                    .size(userFileInfo.getSize())
                    .name(userFileInfo.getOriginalFileName())
                    .uploadDate(userFileInfo.getUploadDate())
                    .downloadLock(userFileInfo.isDownloadLocked())
                    .owned(owned)
                    .downloadCount(userFileInfo.getDownloadCount())
                    .lastDownloadDate(userFileInfo.getLastDownloadDate())
                    .comment(userFileInfo.getComment())
                    .build());
        }
        return fileDetailList;
    }

    /**
     * 掲示板のファイルサイズ合計とログインユーザーの不可視設定のファイルサイズ合計をかえす
     * @param userDetail ログインユーザー
     * @return DTO
     */
    public AllFileSizeInfo allFileDetails(CustomUserDetails userDetail) {
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

    //テスト用
    public List<FileDetail> test(){
        List<FileDetail> fileDetails = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            fileDetails.add(FileDetail.builder()
                            .fileID("uniqueFile" + i)
                            .size(i * 1000)
                            .owned(false)
                            .name("name" + i)
                            .downloadLock(false)
                            .uploadDate(LocalDateTime.now().minusSeconds(i))
                    .build());
        }
        return fileDetails;
    }
}
