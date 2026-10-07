package you.fileserver.config;

/**
 * 共通定数のクラス
 */
public class Constants {
    //エンドポイント
    public static final String UPLOADER_ROOT_PATH = "/CtCloader";
    //API
    public static final String FILE_DETAILS_JSON_PATH = "/api/file-details.json";
    public static final String UPLOAD_PATH = "/api/upload/**";
    public static final String TRANSFER_PATH = "/api/transfer";
    public static final String DOWNLOAD_AUTH_PATH = "/api/download/auth";
    public static final String DOWNLOAD_PATH = "/api/download";
    public static final String DELETE_PATH = "/api/delete";
    public static final String REGISTER_PATH = "/api/register";
    public static final String LOGIN_PATH = "/api/login";
    public static final String LOGOUT_PATH = "/api/logout";
    public static final String USER_ID_PATH = "/api/user";
    public static final String USER_NAME_EXISTS_PATH = "/api/user_name_exists";

    //htmlコンポーネント
    public static final String CSS_PATH = "/css/**";
    public static final String JS_PATH = "/js/**";
    public static final String IMG_PATH = "/img/**";

    //権限
    public static final String AUTH_USER = "USER";
    public static final String AUTH_ADMIN = "ADMIN";

    //TusFileUploadServiceのURL指定
    public static final String TUS_UPLOAD_PATH = "/api/upload/*";
    //アップロードファイルの一時保存用フォルダ
    public static final String TEMP_PATH = "temp";
    //アップロードされたファイル用フォルダ
    public static final String FILE_PATH = "files";

    //パスワード用正規表現
    public static final String FILE_PASSWORD_REGEX = "^[a-zA-Z0-9]{4,12}$";

    //ページのルート
    public static final String ROOT_URL = "http://localhost:8080";
}
