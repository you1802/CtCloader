package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.authentication.CustomUserDetails;
import you.fileserver.dto.AllFileDetails;
import you.fileserver.dto.FileDetail;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.repository.UploadFileInfoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static you.fileserver.config.Constants.AUTH_ADMIN;

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
    public List<FileDetail> fileDetails(CustomUserDetails userDetail) {
        List<UploadFileInfo> uploadFileInfoList;
        if (!(userDetail == null)) {
            if (userDetail.userAccount().getRole().equals(AUTH_ADMIN)) {
                uploadFileInfoList = uploadFileInfoRepository.findAll(); //ADMINならすべてのファイル
            } else uploadFileInfoList = uploadFileInfoRepository.findByOwnerOrVisible(userDetail.getUsername(), true); //ログイン中なら自分のファイル+可視設定に応じて
        } else uploadFileInfoList = uploadFileInfoRepository.findByVisible(true); //非ログインなら可視設定のみ

        List<FileDetail> fileDetails = new ArrayList<>();

        for (UploadFileInfo fileInfo : uploadFileInfoList) {
            boolean owned;
            if (userDetail == null) {
                owned = false;
            } else owned = userDetail.getUsername().equals(fileInfo.getOwner());

            fileDetails.add(FileDetail.builder()
                    .uploadDate(fileInfo.getUploadDate())
                    .size(fileInfo.getSize())
                    .owned(owned)
                    .name(fileInfo.getOriginalFileName())
                    .uniqueFileName(fileInfo.getUniqueFileName())
                    .downloadLock(fileInfo.isDownloadLocked())
                    .comment(fileInfo.getComment())
                    .build());
        }
        return fileDetails;
    }

    /**
     * 掲示板のファイルサイズ合計とログインユーザーの不可視設定のファイルサイズ合計をかえす
     * @param userDetail ログインユーザー
     * @return DTO
     */
    public AllFileDetails allFileDetails(CustomUserDetails userDetail) {
        List<UploadFileInfo> fileInfoList = uploadFileInfoRepository.findAll();

        return new AllFileDetails(fileInfoList.stream()
                .filter(UploadFileInfo::isVisible)
                .mapToLong(UploadFileInfo::getSize).sum(),
                userDetail == null ? -1 :
                        fileInfoList.stream().filter(info -> !info.isVisible())
                                .filter(info -> Objects.equals(info.getOwner(), userDetail.getUsername()))
                                .mapToLong(UploadFileInfo::getSize).sum());
    }
}
