/*
HTMLを読み込み後に呼ばれる
 */
$(function () {
    'use strict';

    //GET以外のすべてのajaxのヘッダーにcsrfトークンを入れる
    $(document).ajaxSend(function (event, xhr, settings) {
        if (settings.type !== "GET") {
            xhr.setRequestHeader(csrfHeader, `${getCsrfToken()}`);
        }
    })

    /*
    アップローダーの設定
     */
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
                $("#uploadModal").modal("hide");
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
        uploadModalCloseButtonHeader.disabled = false;
        uploadModalCloseButtonFooter.disabled = false;
    });
    uppy.on("error", () => {
        uploadModalCloseButtonHeader.disabled = false;
        uploadModalCloseButtonFooter.disabled = false;
    })

    /*
    アップロードモーダルのUI関係
     */
    const setDeletePassword = document.getElementById("setDeletePassword");
    const setDeletePassword_div = document.getElementById("setDeletePassword_div");
    const confirmSetDeletePassword = document.getElementById("confirmSetDeletePassword");

    const downloadPasswordEnabled = document.getElementById("downloadPasswordEnabled");
    const setDownloadPasswordGroup = document.getElementById("setDownloadPasswordGroup");
    const setDownloadPassword = document.getElementById("setDownloadPassword");
    const setDownloadPassword_div = document.getElementById("setDownloadPassword_div");
    const confirmSetDownloadPassword = document.getElementById("confirmSetDownloadPassword");

    const uploadButton = document.getElementById("uploadButton");

    const uploadModalCloseButtonHeader = document.getElementById("uploadModalCloseButtonHeader");
    const uploadModalCloseButtonFooter = document.getElementById("uploadModalCloseButtonFooter");

    //以下削除用パスワードのバリデーションチェック
    setDeletePassword.addEventListener("focus", () => {
        setDeletePassword_div.classList.remove("was-validated");
        turnOffConfirmSetDeletePasswordIsValid();
    });

    setDeletePassword.addEventListener("blur", () => {
        setDeletePassword_div.classList.add("was-validated");
        confirmSetDeletePasswordIsValid();
        switchUploadButton()
    });

    confirmSetDeletePassword.addEventListener("focus", () => {
        turnOffConfirmSetDeletePasswordIsValid();
    });

    confirmSetDeletePassword.addEventListener("blur", () => {
        confirmSetDeletePasswordIsValid();
        switchUploadButton();
    });

    //確認用パスワードの整合性チェックを外す
    function turnOffConfirmSetDeletePasswordIsValid() {
        confirmSetDeletePassword.classList.remove("is-valid");
        confirmSetDeletePassword.classList.remove("is-invalid");
    }

    //確認用パスワードの整合性チェック
    function confirmSetDeletePasswordIsValid() {
        if (confirmSetDeletePassword.value === "") return;
        if (setDeletePassword.value === confirmSetDeletePassword.value) {
            confirmSetDeletePassword.classList.remove("is-invalid");
            confirmSetDeletePassword.classList.add("is-valid");
        } else {
            confirmSetDeletePassword.classList.remove("is-valid");
            confirmSetDeletePassword.classList.add("is-invalid");
        }
    }

    //ダウンロード用パスワードを入力するかのチェックボックス
    downloadPasswordEnabled.addEventListener("change", () => {
        if (downloadPasswordEnabled.checked) {
            setDownloadPasswordGroup.classList.remove("d-none");
        } else {
            setDownloadPasswordGroup.classList.add("d-none");
        }
        switchUploadButton();
    })

    //ダウンロード用パスワードのバリデーション
    setDownloadPassword.addEventListener("focus", () => {
        setDownloadPassword_div.classList.remove("was-validated");
        turnOffConfirmSetDownloadPasswordIsValid();
    });

    setDownloadPassword.addEventListener("blur", () => {
        setDownloadPassword_div.classList.add("was-validated");
        confirmSetDownloadPasswordIsValid();
        switchUploadButton()
    });

    confirmSetDownloadPassword.addEventListener("focus", () => {
        turnOffConfirmSetDownloadPasswordIsValid();
    });

    confirmSetDownloadPassword.addEventListener("blur", () => {
        confirmSetDownloadPasswordIsValid();
        switchUploadButton();
    });

    //確認用パスワードの整合性チェックを外す
    function turnOffConfirmSetDownloadPasswordIsValid() {
        confirmSetDownloadPassword.classList.remove("is-valid");
        confirmSetDownloadPassword.classList.remove("is-invalid");
    }

    //確認用パスワードの整合性チェック
    function confirmSetDownloadPasswordIsValid() {
        if (confirmSetDownloadPassword.value === "") return;
        if (setDownloadPassword.value === confirmSetDownloadPassword.value) {
            confirmSetDownloadPassword.classList.remove("is-invalid");
            confirmSetDownloadPassword.classList.add("is-valid");
        } else {
            confirmSetDownloadPassword.classList.remove("is-valid");
            confirmSetDownloadPassword.classList.add("is-invalid");
        }
    }

    //アップロードボタンのを有効か無効か切り替え
    function switchUploadButton() {
        const {totalProgress} = uppy.getState();
        if (totalProgress > 0) return; //アップロード中かアップロード完了時は処理しない

        if (downloadPasswordEnabled.checked) {
            if (uppy.getFiles().length > 0 &&  setDeletePassword.checkValidity() && confirmSetDeletePassword.classList.contains("is-valid") && setDownloadPassword.checkValidity() && confirmSetDownloadPassword.classList.contains("is-valid")) {
                uploadButton.disabled = false;
            } else {
                uploadButton.disabled = true;
            }
        } else {
            if (uppy.getFiles().length > 0 &&  setDeletePassword.checkValidity() && confirmSetDeletePassword.classList.contains("is-valid")) {
                uploadButton.disabled = false;
            } else {
                uploadButton.disabled = true;
            }
        }
    }

    //アップロードモーダルが閉じられた時,モーダル内の情報をリセットしテーブルを更新する
    $("#uploadModal").on("hidden.bs.modal", function () {
        uppy.clear();

        $("#uploadForm")[0].reset();

        turnOffConfirmSetDeletePasswordIsValid();
        setDeletePassword_div.classList.remove("was-validated");
        setDeletePassword.classList.remove("is-valid");

        setDownloadPasswordGroup.classList.add("d-none");

        turnOffConfirmSetDownloadPasswordIsValid();
        setDownloadPassword_div.classList.remove("was-validated");
        setDownloadPassword.classList.remove("is-valid");

        tableRefresh();
    })

    //アップロードボタンを押したとき条件を満たしていればアップロードし、パスワードを編集不可にする
    uploadButton.addEventListener("click", () => {
        if (downloadPasswordEnabled.checked) {
            if (uppy.getFiles().length > 0 &&  setDeletePassword.checkValidity() && confirmSetDeletePassword.classList.contains("is-valid") && setDownloadPassword.checkValidity() && confirmSetDownloadPassword.classList.contains("is-valid")) {
                uploadFile();
                setDeletePassword.readOnly = true;
                setDeletePassword.classList.add("is-valid");
                confirmSetDeletePassword.readOnly = true;

                downloadPasswordEnabled.disabled = true;
                setDownloadPassword.readOnly = true;
                setDownloadPassword.classList.add("is-valid");
                confirmSetDownloadPassword.readOnly = true;

                uploadButton.disabled = true;
                uploadModalCloseButtonHeader.disabled = true;
                uploadModalCloseButtonFooter.disabled = true;
            }
        } else {
            if (uppy.getFiles().length > 0 &&  setDeletePassword.checkValidity() && confirmSetDeletePassword.classList.contains("is-valid")) {
                uploadFile();
                setDeletePassword.readOnly = true;
                setDeletePassword.classList.add("is-valid");
                confirmSetDeletePassword.readOnly = true;

                uploadButton.disabled = true;
                uploadModalCloseButtonHeader.disabled = true;
                uploadModalCloseButtonFooter.disabled = true;
            }
        }
    })

    /*
    ユーザー登録関係
     */

    //ログイン状態による表示ボタンの切替
    function navbarChange(isLoggedIn) {
        if (isLoggedIn) {
            $("#notLoggedIn").addClass("d-none");
            $("#loggedIn").removeClass("d-none");

        } else {
            $("#notLoggedIn").removeClass("d-none");
            $("#loggedIn").addClass("d-none");
        }
    }

    //ユーザー登録ボタンのリスナー
    $("#registrationButton").on("click", () => {
        $("#registerModal").modal("show");
    })

    //ユーザー登録実行ボタンのリスナー
    $("#registerExecuteButton").on("click", function () {
        $.ajax({
            url: "/api/register",
            method: "POST",
            dataType: "json",
            data: {
                username: $("#userName").val(),
                password: $("#userPassword").val()
            }
        }).done(function (data) {
            if (data.code === 200) {
                showToast(data.message, "success");$("#registerModal").modal("hide");
            }  else {
                showToast(data.message, "danger");$("#registerModal").modal("hide");
            }
        }).fail(function () {
            showToast("通信エラー", "danger");
        })
    })

    //ユーザー登録モーダルを閉じたときフォームをリセットする
    $("#registerModal").on("hidden.bs.modal", function () {
        $("#registerForm")[0].reset();
    })

    //ログインボタンのリスナー
    $("#loginButton").on("click", () => {
        $("#loginModal").modal("show");
    })

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
                showToast("ログインに成功しました", "success");$("#loginModal").modal("hide");
            } else {
                showToast("ユーザー名またはパスワードが一致しません", "danger");$("#loginModal").modal("hide");
            }
        }).fail(function () {
            showToast("通信エラー", "danger");
        })
    })

    //ログインモーダルを閉じたときフォームをリセット
    $("#loginModal").on("hidden.bs.modal", function () {
        $("#loginForm")[0].reset();
    })

    //ログアウトボタンのリスナー
    $("#logoutButton").on("click", () => {
        $("#logoutModal").modal("show");
    })

    //ログアウト実行ボタンのイベントリスナー
    $("#logoutExecuteButton").on("click", () => {
        $.ajax({
            url: "/api/logout",
            method: "POST",
            dataType: "json",
        }).done(function () {
            $("#logoutModal").modal("hide");
            window.location.reload();
        })
    })

})
