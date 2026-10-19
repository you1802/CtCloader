package you.fileserver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllFileSizeInfo {
    private long forumFileSize; //現在の掲示板のファイル合計サイズ
    private long forumFileSizeLimit; //掲示板のサイズ上限
    private long userFileSize; //ログイン中のユーザーが不可視設定でアップロードしたファイルサイズ合計
    private long userFileSizeLimit; //ユーザーごとの不可視設定ファイルサイズ上限
}
