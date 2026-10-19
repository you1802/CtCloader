package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.authentication.CustomUserDetails;
import you.fileserver.dto.AllFileSizeInfo;
import you.fileserver.dto.FileDetail;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.projection.UserUploadFileInfoProjection;
import you.fileserver.repository.UploadFileInfoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static you.fileserver.config.Constants.*;

@Service
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
        List<UserUploadFileInfoProjection> userUploadFileInfoList;

        boolean login = userDetail == null; //ログイン中かどうか

        if(login) {
            userUploadFileInfoList = uploadFileInfoRepository.findByVisible(true, UserUploadFileInfoProjection.class); //非ログインなら可視設定のみ
        } else userUploadFileInfoList = uploadFileInfoRepository.findByOwnerOrVisible(userDetail.getUsername(), true, UserUploadFileInfoProjection.class); //ログイン中なら自分のファイル+可視設定に応じて

        List<FileDetail> fileDetailList = new ArrayList<>();

        for (UserUploadFileInfoProjection userFileInfo : userUploadFileInfoList) {
            boolean owned;

            //ファイルが自分のアップロードしたものかどうかの判定
            if (login) {
                owned = false;
            } else owned = userFileInfo.getOwner().equals(userDetail.getUsername());

            fileDetailList.add(FileDetail.builder()
                            .uniqueFileName(userFileInfo.getUniqueFileName())
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
        List<UploadFileInfo> fileInfoList = uploadFileInfoRepository.findAll();

        return AllFileSizeInfo.builder().
                forumFileSizeLimit(FORUM_FILESIZE_LIMIT).
                userFileSizeLimit(USER_FILESIZE_LIMIT).
                forumFileSize(fileInfoList.stream().
                        filter(UploadFileInfo::isVisible).
                        mapToLong(UploadFileInfo::getSize).sum()).
                userFileSize(userDetail == null ? -1 :
                        fileInfoList.stream().
                                filter(info -> !info.isVisible() && Objects.equals(info.getOwner(), userDetail.getUsername())).
                                mapToLong(UploadFileInfo::getSize).sum())
                .build();
    }

//    //テスト用
//    public List<FileDetail> test(){
//        List<FileDetail> fileDetails = new ArrayList<>();
//
//        for (int i = 0; i < 100; i++) {
//            fileDetails.add(FileDetail.builder()
//                            .uniqueFileName("uniqueFile" + i)
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
