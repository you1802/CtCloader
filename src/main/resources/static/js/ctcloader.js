/*
以下はHTMLを読み込み後に呼ばれる
 */
$(function () {
     'use strict';

    //GET以外のすべてのajaxのヘッダーにcsrfトークンを入れる
    $(document).ajaxSend(function (event, xhr, settings) {
        if (settings.type !== "GET") {
            xhr.setRequestHeader(csrfHeader, `${getCsrfToken()}`);
        }
    });

    /*
    アップローダーの設定
     */
    const $uploadModal = $("#uploadModal");

    const $setDeletePassword = $("#setDeletePassword");
    const $setDeletePassword_div = $("#setDeletePassword_div");
    const $confirmSetDeletePassword = $("#confirmSetDeletePassword");

    const $downloadPasswordEnabled = $("#downloadPasswordEnabled");

    const $setDownloadPasswordGroup = $("#setDownloadPasswordGroup");
    const $setDownloadPassword = $("#setDownloadPassword");
    const $setDownloadPassword_div = $("#setDownloadPassword_div");
    const $confirmSetDownloadPassword = $("#confirmSetDownloadPassword");

    const $uploadButton = $("#uploadButton");
    const $uploadModalCloseButtonHeader = $("#uploadModalCloseButtonHeader");
    const $uploadModalCloseButtonFooter = $("#uploadModalCloseButtonFooter");

    const {Uppy, Dashboard, Form, Tus} = window.Uppy;
    const ja_JP = window.Uppy.locales.ja_JP;
    const uppy = new Uppy({
        locale: ja_JP //日本語化
    })
        .use(Dashboard, {
            target: "#uploader",
            inline: true,
            hideUploadButton: true,
            doneButtonHandler: () => { //doneボタンを押したときモーダルを閉じる
                $uploadModal.modal("hide");
            }
        })
        .use(Form, {
            target: "#uploadForm",
            getMetaFromForm: true //アップロード用のフォームの情報
        })
        .use(Tus, { //Tusプロトコルを指定
            endpoint: "/api/upload", //エンドポイントの指定
            headers: (file) => {
                return  {[csrfHeader]: getCsrfToken()}
            }
        });

    //アップロード用関数
    function uploadFile() {
        uppy.upload()
    }

    //ファイルが追加か削除された時,ボタンのオンオフの判定を行う
    uppy.on("file-added", switchUploadButton)
    uppy.on("file-removed", switchUploadButton)

    //アップロード完了またはエラー時にモーダルを閉じるボタンをアクティブに
    uppy.on("complete", () => {
        $uploadModalCloseButtonHeader.prop("disabled", false);
        $uploadModalCloseButtonFooter.prop("disabled", false);
    });
    uppy.on("error", () => {
        $uploadModalCloseButtonHeader.prop("disabled", false);
        $uploadModalCloseButtonFooter.prop("disabled", false);
    });

    /*
    アップロードモーダルのUI関係
     */

    //以下削除用パスワードのバリデーションチェック
    $setDeletePassword
        .on("focus", function () {
        $setDeletePassword_div.removeClass("was-validated");
        $confirmSetDeletePassword.removeClass("is-valid is-invalid");
        })
        .on("blur", function () {
        $setDeletePassword_div.addClass("was-validated");
        confirmSetDeletePasswordIsValid();
        switchUploadButton();
    });

    $confirmSetDeletePassword
    .on("focus", function () {
        $confirmSetDeletePassword.removeClass("is-valid is-invalid");
    }).on("blur", function () {
        confirmSetDeletePasswordIsValid();
        switchUploadButton();
    });

    //確認用パスワードの整合性チェック
    function confirmSetDeletePasswordIsValid() {
        if (!$confirmSetDeletePassword.val()) return;
        if ($setDeletePassword.val() === $confirmSetDeletePassword.val()) {
            $confirmSetDeletePassword.removeClass("is-invalid").addClass("is-valid");
        } else {
            $confirmSetDeletePassword.removeClass("is-valid").addClass("is-invalid");
        }
    }

    //ダウンロード用パスワードを入力するかのチェックボックス
    $downloadPasswordEnabled.on("change", function () {
        if ($downloadPasswordEnabled.prop("checked")) {
            $setDownloadPasswordGroup.removeClass("d-none");
        } else {
            $setDownloadPasswordGroup.addClass("d-none");
        }
        switchUploadButton();
    });

    //ダウンロード用パスワードのバリデーション
    $setDownloadPassword.on("focus", function () {
        $setDownloadPassword_div.removeClass("was-validated");
        $confirmSetDownloadPassword.removeClass("is-invalid is-valid");
    }).on("blur", function () {
        $setDownloadPassword_div.addClass("was-validated");
        confirmSetDownloadPasswordIsValid();
        switchUploadButton();
    });

    //確認用パスワードの整合性チェック
    $confirmSetDownloadPassword.on("focus", function () {
        $confirmSetDownloadPassword.removeClass("is-invalid is-valid");
    }).on("blur", function () {
        confirmSetDownloadPasswordIsValid();
        switchUploadButton();
    });

    //確認用パスワードの整合性チェック
    function confirmSetDownloadPasswordIsValid() {
        if (!$confirmSetDownloadPassword.val()) return;
        if ($setDownloadPassword.val() === $confirmSetDownloadPassword.val()) {
            $confirmSetDownloadPassword.removeClass("is-invalid").addClass("is-valid");
        } else {
            $confirmSetDownloadPassword.removeClass("is-valid").addClass("is-invalid");
        }
    }

    //アップロードボタンのを有効か無効か切り替え
    function switchUploadButton() {
        const {totalProgress} = uppy.getState();
        if (totalProgress > 0) return; //アップロード中かアップロード完了時は処理しない

        if ($downloadPasswordEnabled.prop("checked")) {
            if (uppy.getFiles().length > 0 &&  $setDeletePassword[0].checkValidity() && $confirmSetDeletePassword.hasClass("is-valid") && $setDownloadPassword[0].checkValidity() && $confirmSetDownloadPassword.hasClass("is-valid")) {
                $uploadButton.prop("disabled", false);
            } else {
                $uploadButton.prop("disabled", true);
            };
        } else {
            if (uppy.getFiles().length > 0 &&  $setDeletePassword[0].checkValidity() && $confirmSetDeletePassword.hasClass("is-valid")) {
                $uploadButton.prop("disabled", false);
            } else {
                $uploadButton.prop("disabled", true);
            }
        }
    }

    //アップロードボタンを押したとき条件を満たしていればアップロードし、パスワードを編集不可にする
    $uploadButton.on("click", function () {
        if ($downloadPasswordEnabled.prop("checked")) {
            if (uppy.getFiles().length > 0 && $setDeletePassword[0].checkValidity() && $confirmSetDeletePassword.hasClass("is-valid") && $setDownloadPassword[0].checkValidity() && $confirmSetDownloadPassword.hasClass("is-valid")) {
                $setDeletePassword.prop("readOnly", true).addClass("is-valid");
                $confirmSetDeletePassword.prop("readOnly", true);

                $downloadPasswordEnabled.prop("disabled", true);
                $setDownloadPassword.prop("readOnly", true).addClass("is-valid");
                $confirmSetDownloadPassword.prop("readOnly", true);

                $uploadButton.prop("disabled", true);
                $uploadModalCloseButtonHeader.prop("disabled", true);
                $uploadModalCloseButtonFooter.prop("disabled", true);

                uploadFile();
            }
        } else {
            if (uppy.getFiles().length > 0 && $setDeletePassword[0].checkValidity() && $confirmSetDeletePassword.hasClass("is-valid")) {
                $setDeletePassword.prop("readOnly", true).addClass("is-valid");
                $confirmSetDeletePassword.prop("readOnly", true);

                $downloadPasswordEnabled.prop("disabled", true);

                $uploadButton.prop("disabled", true);
                $uploadModalCloseButtonHeader.prop("disabled", true);
                $uploadModalCloseButtonFooter.prop("disabled", true);

                uploadFile();
            }
        }
    });

    //アップロードモーダルが閉じられた時,モーダル内の情報をリセットしテーブルを更新する
    $uploadModal.on("hidden.bs.modal", function () {
        uppy.clear();

        $("#uploadForm")[0].reset();

        $confirmSetDeletePassword.removeClass("is-valid is-invalid");
        $setDeletePassword.removeClass("is-valid");
        $setDeletePassword_div.removeClass("was-validated");

        $downloadPasswordEnabled.prop("disabled", false);
        $setDownloadPasswordGroup.addClass("d-none");

        $confirmSetDownloadPassword.removeClass("is-valid is-invalid");
        $setDownloadPassword_div.removeClass("was-validated");
        $setDownloadPassword.removeClass("is-valid");

        tableRefresh();
    });

    /*
    ユーザー登録関係
     */

    const $loggedIn = $("#loggedIn");
    const $notLoggedIn = $("#notLoggedIn");
    const $loginUserNameButton = $("#loginUserNameButton");
    const $forms = $(".needs-validation");
    const $userName = $("#userName");
    const $userPassword = $("#userPassword");
    const $confirmUserPassword = $("#confirmUserPassword");
    const $registerExecuteButton = $("#registerExecuteButton");
    const $userNameInfo = $("#userNameInfo");
    const $registerModal = $("#registerModal");
    const $registerForm = $("#registerForm");
    const $loginUserName = $("#loginUserName");
    const $registrationButton = $("#registrationButton");

    //バリデーション系
    Array.from($forms).forEach(form => {
        form.addEventListener("submit", e => {
            if (!form.checkValidity()) {
                e.preventDefault();
                e.stopPropagation();
            }
            form.classList.add("was-validated");
        });
    });
    //ユーザー名入力検証
    $userName.on("blur", function () {
        const userName = $userName.val().trim();
        const inputEl = $userName[0];   //生のDOM要素を取得（setCustomValidity用）
        //再判定用にエラークリア
        inputEl.setCustomValidity("");
        $userNameInfo.text("");

        if ($userName.is(":invalid")) {
            $userNameInfo.text("入力内容に誤りがあります。半角で正しく入力されているかご確認ください。");
            return;
        }
        if (userName === "") return;
        $registerExecuteButton.prop("disabled", true);  //通信の間、ボタン無効

        $.getJSON("/api/user_name_exists?username=" + userName)
            .done(function (json) {
                if (json.exists) {
                    inputEl.setCustomValidity("このユーザー名は既に存在します。");
                    $userNameInfo.text("このユーザー名は既に存在します");
                    $userName.addClass("is-invalid");
                } else {
                    inputEl.setCustomValidity("");
                    $userName.removeClass("is-invalid");
                }
            })
            .always(function () {
                $registerExecuteButton.prop("disabled", false);
            });
    });
    //入力時に判定をクリア
    $userName.on("input", function () {
        this.setCustomValidity("");
        $userName.removeClass("is-invalid");
        $userNameInfo.text("");
    });

    function checkConfirmPassword() {
        const password = $userPassword.val();
        const confirmPassword = $confirmUserPassword.val();
        const conPassInputEl = $confirmUserPassword[0];

    }



    //ログイン状態による表示ボタンの切替
    /**
     * ナビバーの表示を変更
     * @param isLoggedIn ログイン状態(true:ログイン済み false:未ログイン)
     * @param json ユーザー名の情報
     */
    function navbarChange(isLoggedIn, json) {
        if (isLoggedIn) {
            $notLoggedIn.addClass("d-none");
            $loggedIn.removeClass("d-none");
            (json === undefined) ? $loginUserNameButton.text("👤" + $loginUserName.val()) : $loginUserNameButton.text("👤" + json.user);
        } else {
            $notLoggedIn.removeClass("d-none");
            $loggedIn.addClass("d-none");
        }
    }
    //ページ読み込み時初回判定
    let isInitialDetermination = false;
    $(document).ajaxStop(() => {
        if (isInitialDetermination) return;
        isInitialDetermination = true;
        $.getJSON("/api/user")
            .done(function (json) {
                if (json.user === "ゲスト") {
                    navbarChange(false);
                } else {
                    navbarChange(true, json);
                }
                $(document).off('ajaxStop');
            });
    });

    //ユーザー登録ボタンのリスナー
    $registrationButton.on("click", () => {
        $registerModal.modal("show");
    });

    //ユーザー登録実行ボタンのリスナー
    $registerExecuteButton.on("click", function () {
        $.ajax({
            url: "/api/register",
            method: "POST",
            dataType: "json",
            data: {
                username: $userName.val(),
                password: $userPassword.val()
            }
        }).done(function (data) {
            if (data.code === 200) {
                showToast(data.message, "success");//$registerModal.modal("hide");
            }  else {
                showToast(data.message, "danger");//$registerModal.modal("hide");
            }
        }).fail(function () {
            showToast("通信エラー", "danger");
        });
    });

    //ユーザー登録モーダルを閉じたときフォームをリセットする
    $registerModal.on("hidden.bs.modal", function () {
        $registerForm[0].reset();
        $registerForm.removeClass("was-validated");
        $registerForm.find(".form-control").removeClass("is-invalid");
        $userNameInfo.text("エラー");
    });

    //ログインボタンのリスナー
    $("#loginButton").on("click", () => {
        $("#loginModal").modal("show");
    });

    //ログイン実行ボタンのリスナー
    $("#loginExecuteButton").text("ログイン").off("click").on("click", function () {
        $.ajax({
            url: "/api/login",
            method: "POST",
            data: {
                username: $("#loginUserName").val(),
                password: $("#loginUserPassword").val(),
            }
        }).done(function (data) {
            if (data.code === 200) {
                $("meta[name='_csrf']").attr("content", data.csrfToken);
                navbarChange(true);
                showToast("ログインに成功しました", "success");$("#loginModal").modal("hide");
            } else {
                showToast("ユーザー名またはパスワードが一致しません", "danger");$("#loginModal").modal("hide");
            }
        }).fail(function () {
            showToast("通信エラー", "danger");
        });
    });

    //ログインモーダルを閉じたときフォームをリセット
    $("#loginModal").on("hidden.bs.modal", function () {
        $("#loginForm")[0].reset();
    });

    //ログアウトボタンのリスナー
    $("#logoutButton").on("click", () => {
        $("#logoutModal").modal("show");
    });

    //ログアウト実行ボタンのイベントリスナー
    $("#logoutExecuteButton").on("click", () => {
        $.ajax({
            url: "/api/logout",
            method: "POST",
            dataType: "json",
        }).done(function () {
            navbarChange(false);
            $("#logoutModal").modal("hide");
            window.location.reload();
        });
    });

});
