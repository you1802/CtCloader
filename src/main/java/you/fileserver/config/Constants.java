package you.fileserver.config;

/**
 * 共通定数のクラス
 */
public class Constants {
    //エンドポイント
    public static final String UPLOADER_ROOT_PATH = "/uploader";
    //API
    public static final String FILE_DETAILS_JSON_PATH = "/api/file-details.json";
    public static final String UPLOAD_PATH = "/api/upload/**";
    public static final String DOWNLOAD_PATH = "/api/download";

    //htmlコンポーネント
    public static final String CSS_PATH = "/css/**";
    public static final String JS_PATH = "/js/**";
    public static final String IMG_PATH = "/img/**";

    //TusFileUploadServiceのURL指定
    public static final String TUS_UPLOAD_PATH = "/api/upload/*";
    //アップロードファイルの一時保存用フォルダ
    public static final String TEMP_PATH = "temp";
    //アップロードされたファイル用フォルダ
    public static final String FILE_PATH = "files";
}
