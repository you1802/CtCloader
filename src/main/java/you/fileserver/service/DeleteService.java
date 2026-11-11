package you.fileserver.service;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import you.fileserver.dto.DefaultApiResponse;
import you.fileserver.dto.projection.DeleteFileInfoProjection;
import you.fileserver.exception.DeleteFailureException;
import you.fileserver.security.annotation.IsAdmin;
import you.fileserver.security.authentication.CustomUserDetails;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static you.fileserver.config.Constants.FILE_PATH;

/**
 * ファイルの削除関係のサービスクラス
 */
@Service
@Slf4j
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
     * @param fileID ファイルの実際の保存名(ID)
     * @param deletePassword ユーザーが設定したパスワード
     * @return 成功時のレスポンス
     */
    public DefaultApiResponse deleteFile(String fileID, String deletePassword, @Nullable CustomUserDetails userDetail, String ipAddress) {
        Optional<CustomUserDetails> userDetailOpt = Optional.ofNullable(userDetail); //ユーザー情報をオプショナルでラップ
        //リポジトリにフェイル情報があるかの確認して、あるなら変数に格納
        DeleteFileInfoProjection fileInfo = uploadFileInfoRepository.findByFileID(fileID, DeleteFileInfoProjection.class)
                .orElseThrow(() -> new DeleteFailureException("既にデータが存在しません。")); //アップロードの情報がDBにない場合

        UploadFilePassword filePassword = uploadFilePasswordRepository.findById(fileID)
                        .orElseThrow(() -> { //通常あり得ないがアップロード情報は残っていてパスワード情報のみない場合
                            executeDeleteFile(fileID); //ファイル情報を削除
                            return new DeleteFailureException("既にデータが存在しません。");
                        });

        if (fileInfo.getOwner().isEmpty()) { //非ログイン中にアップロードされたファイルの場合
            if (!passwordEncoder.matches(filePassword.getDeletePassword(), deletePassword)) throw new DeleteFailureException("削除用パスワードが一致しません。"); //削除用パスが一致しない場合
        } else { //ログイン中にアップロードされたファイルの場合
            userDetailOpt.ifPresentOrElse(user -> { //ユーザーがログイン中の場合
                if (!user.getUsername().equals(fileInfo.getOwner())) throw new DeleteFailureException("ログイン中にアップロードされたファイルです。あなたが所有者の場合はアップロードしたユーザーでログインしてから削除を実行してください。"); //ファイル所有者でない場合は削除失敗
            }, () -> {
                throw new DeleteFailureException("ログイン中にアップロードされたファイルです。あなたが所有者の場合はアップロードしたユーザーでログインしてから削除を実行してください。"); //非ログインなら削除失敗
            });
        }

        executeDeleteFile(fileID); //ファイルの実態とDB情報を削除

        //ログを出力
        log.info("file delete complete. fileID:{} ip:{}", fileID, ipAddress);

        return new DefaultApiResponse(200, "削除に成功しました。");
    }

    /**
     * ADMIN用ファイル削除メソッド。パスワード不要
     * @param fileID ファイルid
     * @return 成功時のレスポンス
     */
    @IsAdmin
    public DefaultApiResponse adminDeleteFile(String fileID) {
        if (uploadFileInfoRepository.existsById(fileID)) throw new DeleteFailureException("既にデータが存在しません。"); //既にデータがない場合

        executeDeleteFile(fileID);
        log.info("file delete complete. fileID:{}", fileID);

        return new DefaultApiResponse(200, "削除に成功しました。");
    }

    /**
     * 対象ファイルの実態とDB上の情報を削除する
     * @param fileID ファイルid
     */
    private void executeDeleteFile(String fileID) {
        //リポジトリからファイル情報とパスワードを削除
        uploadFileInfoRepository.deleteById(fileID);
        uploadFilePasswordRepository.deleteById(fileID);

        //ファイル実体を削除
        File file = Path.of(FILE_PATH, fileID).toFile();

        file.delete();
    }
}
