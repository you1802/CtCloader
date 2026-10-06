package you.fileserver.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import you.fileserver.authentication.CustomUserDetails;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static you.fileserver.config.Constants.AUTH_ADMIN;
import static you.fileserver.config.Constants.FILE_PATH;

@Service
public class DeleteService {
    private final PasswordEncoder passwordEncoder;
    private final UploadFilePasswordRepository uploadFilePasswordRepository;
    private final UploadFileInfoRepository uploadFileInfoRepository;

    public DeleteService(PasswordEncoder passwordEncoder, UploadFilePasswordRepository uploadFilePasswordRepository, UploadFileInfoRepository uploadFileInfoRepository) {
        this.passwordEncoder = passwordEncoder;
        this.uploadFilePasswordRepository = uploadFilePasswordRepository;
        this.uploadFileInfoRepository = uploadFileInfoRepository;
    }
    /**
     * ファイル削除メソッド
     * @param targetFileName ファイルの実際の保存名(ID)
     * @param deletePassword ユーザーが設定したパスワード
     * @return 削除成功したかどうかのレスポンス
     */
    public Map<String, Object> deleteFile(String targetFileName, String deletePassword, CustomUserDetails userDetail) {
        Map<String, Object> failRes = Map.of("code", 400, "message", "削除に失敗しました"); //失敗時のレスポンス
        Map<String, Object> SuccessRes = Map.of("code", 200, "message", "削除に成功しました"); //成功時のレスポンス

        //リポジトリにフェイル情報があるかの確認
        Optional<UploadFileInfo> uploadFileInfo = uploadFileInfoRepository.findById(targetFileName);
        if (uploadFileInfo.isEmpty()) return failRes;

        if (!(!(userDetail == null) && (userDetail.userAccount().getRole().equals(AUTH_ADMIN) || userDetail.getUsername().equals(uploadFileInfo.get().getOwner())))) { //ログイン中のユーザーがadminまたはアップロードしたidの場合はパスワード認証を飛ばす
            //ユーザーが設定したファイルとパスワードが一致するかの確認
            Optional<UploadFilePassword> uploadFilePassword = uploadFilePasswordRepository.findById(targetFileName);
            if (uploadFilePassword.isEmpty()) return failRes;
            if (!passwordEncoder.matches(deletePassword, uploadFilePassword.get().getDeletePassword())) return failRes;

        }

        //リポジトリからファイル情報を削除
        uploadFileInfoRepository.delete(uploadFileInfo.get());

        //ファイル実体を削除
        File file = Path.of(FILE_PATH, targetFileName).toFile();

        file.delete();
        return SuccessRes;
    }
}
