package you.fileserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import you.fileserver.dto.DefaultApiResponse;
import you.fileserver.dto.entity.FileReport;
import you.fileserver.repository.FileReportRepository;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * ファイルの問題報告関係のサービスクラス
 */
@Service
@RequiredArgsConstructor
public class ReportService {
    private final FileReportRepository fileReportRepository;

    /**
     * ユーザーから受け取った報告をDBに登録する
     * @param fileID 対象ファイル名
     * @param comment コメント
     * @param ipAddress IP
     */
    public DefaultApiResponse saveReport(String fileID, String comment, String ipAddress) {
        fileReportRepository.save(FileReport.builder()
                .fileID(fileID)
                .comment(truncateComment(comment))
                .ipAddress(ipAddress)
                .build());
        return new DefaultApiResponse(200, "問題の報告に成功しました。");
    }

    /**
     * コメントをサロゲートペアや書記素クラスタを考慮しつつ１００文字までに制限する(超えた場合は100文字で切って…を付けて返す)
     * @param comment コメント
     * @return 編集済みのコメント
     */
    private String truncateComment(String comment) {
        if (comment == null) return null; //nullならnullをかえす。

        BreakIterator breakIterator = BreakIterator.getCharacterInstance(Locale.JAPANESE); //文字単位の境界を判定するイテレータを取得
        breakIterator.setText(comment); //コメントをセット

        int endIndex = breakIterator.next(100); //先頭から数えて100個目の境界の位置を取得

        if (endIndex >= 255) return comment.substring(0, 254) + "…"; //通常のコメントならまず超えないが、DBが255charまでなのでエラー対策

        if (endIndex == BreakIterator.DONE || endIndex == comment.length()) return comment; //見た目上の文字数の長さが100以下ならコメントをそのままかえす

        return comment.substring(0, endIndex) + "…"; //100以上の時は100で切って…を付けて返す
    }
}
