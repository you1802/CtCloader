/*
テーブルから呼び出す関数
 */
//ファイル削除用モーダルを呼び出す
function fileDeleteModal(targetFileName) {
    $("#targetFileName").val(targetFileName);
    $("#fileControlModalTitle").text("ファイルの削除");
    $("#fileControlExecuteButton").text("削除").removeClass("btn-success").addClass("btn-danger").off().on("click", function () {
        deleteFile($('#targetFileName').val(), $('#fileControlPassword').val());
    })
    $("#fileControlModal").modal("show");
}

//パスワードロックされたファイルダウンロード用モーダルを呼び出す
function lockedFileDownloadModal(targetFileName) {
    $("#targetFileName").val(targetFileName);
    $("#fileControlModalTitle").text("ファイルのダウンロード");
    $("#fileControlExecuteButton").text("ダウンロード").removeClass("btn-danger").addClass("btn-success").off().on("click", function () {
        downloadFile($('#targetFileName').val(), $('#fileControlPassword').val());
    })
    $("#fileControlModal").modal("show");
}

/**
 * パスワードが一致する場合、サーバー側からワンタイムトークンを受け取りファイルをダウンロードする
 * @param targetFileName ダウンロードするファイル名
 * @param fileControlPassword ダウンロードするファイルのパスワード
 */
function downloadFile(targetFileName, fileControlPassword) {
    $.ajax({
        url: "/api/download/auth",
        type: "POST",
        dataType: "json",
        data: {
            targetFileName: targetFileName,
            fileControlPassword: fileControlPassword
        }
    }).done(function (data) {
        if (data.code === 200) {
            window.location.href = `/api/download?targetFileName=${targetFileName}&token=${data.token}`;
        } else {
            showToast(data.message, "danger");
        }
    }).fail(function () {
        showToast("通信エラー", "danger");
    })
}

/**
 * 削除用関数
 * @param targetFileName 削除するファイル名
 * @param fileControlPassword 削除用パスワード
 */
function deleteFile(targetFileName, fileControlPassword) {
    $.ajax({
        url: "/api/delete",
        type: "POST",
        dataType: 'json',
        data: {
            targetFileName: targetFileName,
            fileControlPassword: fileControlPassword
        }
    }).done(function (data) {
        if (data.code === 200) {
            showToast(data.message, "success");
        } else {
            showToast(data.message, "danger");
        }
        tableRefresh()
    })
        .fail(function () {
            showToast("通信エラー", "danger");
        })
}

//csrfトークンを取得する関数
function getCsrfToken() {
    return $("meta[name='_csrf']").attr("content");
}

/*
以下はHTMLを読み込み後に呼ばれる
 */
$(function () {
    'use strict';

    //ヘッダーにあるcsrfトークン用ヘッダーを取得
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    //GET以外のすべてのajaxのヘッダーにcsrfトークンを入れる
    $(document).ajaxSend(function (event, xhr, settings) {
        if (settings.type !== "GET") {
            xhr.setRequestHeader(csrfHeader, `${getCsrfToken()}`);
        }
    })

    /*
    以下テーブル関係
     */
    //テーブル自体の設定
    const table = new DataTable("#files-table", {
        language: {
            url: "https://cdn.datatables.net/plug-ins/3.0.1/i18n/ja.json",
        },

        "columnDefs": [
            {className: "dt-center", "targets": "_all"},
            {"orderable": false, "targets": [3, 4]},
            {"width": 120, "targets": 3},
            {"width": 60, "targets": 4},
            {searchable: false, "targets": [1, 2, 3, 4]}
        ],
        ajax: {
            url: "/api/file-details.json",
            dataSrc: ""
        },
        columns: [
            {data: "name", title: "ファイル名"},
            {
                data: "size", title: "サイズ", render: function (data, type) {
                    if (type === "display") {
                        return formatBytes(data);
                    }
                    return data;
                }
            },
            {data: "uploadDate", title: "アップロード日時"},
            {
                data: "uniqueFileName", title: "ダウンロード", render: function (data, type, row) {
                    if (row.downloadLock === true) {
                        return `
                    <button type="button" class="btn btn-link" onclick="lockedFileDownloadModal('${row.uniqueFileName}')">
                    <i class="bi bi-file-earmark-lock"></i>
                    </button>
                    `} else {
                        return `<a href="api/download?targetFileName=${data}&token=" class="icon-link"><i class="bi-download"></a>`
                    }
                }
            },
            {
                data: null, title: "削除", render: function (data, type, row) {
                    if (row.owned === true) {
                        return `
                    <button type="button" class="btn btn-link" onclick="fileDeleteModal('${row.uniqueFileName}')">
                    <i class="bi bi-trash"></i>
                    </button>
                `;}
                    return null;
                }
            }
        ]
    });

//テーブル更新用関数
    function tableRefresh() {
        table.ajax.reload();
    }

//ファイルのダウンロードまたはアップロードのパスワード入力用モーダルのイベントリスナー
    $("#fileControlModal").on("hidden.bs.modal", function () {
        $("#fileControlPassword").val("");
    })

    /**
     * 与えられたbytesを見やすい形にする(1 KBなど)
     * decimalsは小数点以下何位まで表示するか、デフォルトは2桁
     * @param bytes 変換したい数
     * @param decimals 小数点以下何位まで表示するか
     * @returns {string} 変換後の文字列
     */
    function formatBytes(bytes, decimals = 2) {
        if (bytes === 0) return "0 Byte"; //0のときは0byte
        const k = 1024;　//1キロバイト
        const i = Math.floor( //与えられた数以下の整数を返す
            Math.log(bytes) / Math.log(k)); //分母の対数の底を何乗したら分子の対数の底になるか
        const sizes = ["Bytes", "KB", "MB", "GB", "TB"]; //iが0ならbytes,1ならKB...
        return (bytes / Math.pow(k, i)) //bytesをkのi乗でわる
                .toFixed(decimals) //小数点以下を設定どおりに切り捨て
            + ' ' + sizes[i]; //サイズの文字を追加
    }

    /**
     * トースト表示用関数
     * @param msg 表示するメッセージ
     * @param type メッセージのタイプ(success,danger,warning,info,dark)
     */
    function showToast(msg, type) {
        const container = document.getElementById('toastContainer');

        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-bg-${type} border-0`;
        toast.setAttribute("role", "alert");
        toast.setAttribute("aria-live", "assertive");
        toast.setAttribute("aria-atomic", "true");
        toast.innerHTML = `
    <div class="toast-header">
    <div class="me-auto">告知</div>
    <button type="button" class="ntn-close" data-bs-dismiss="toast" aria-label="Close"></button>
    </div>
    <div class="d-flex">
    <div class="toast-body">${msg}</div>
    </div>
    `;

        container.appendChild(toast);

        const bootstrapToast = new bootstrap.Toast(toast, {
            delay: 3000
        });
        bootstrapToast.show();

        toast.addEventListener("hidden.bs.modal", function () {
            toast.remove();
        })
    }

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
    const $forLogin = $("#loggedIn");
    const $forNotLogin = $("#notLoggedIn");
    const $userNameButton = $("#loginUserNameButton");
    /**
     * ナビバーの表示を変更
     * @param isLoggedIn ログイン状態(true:ログイン済み false:未ログイン)
     * @param json ユーザー名の情報
     */
    function navbarChange(isLoggedIn, json) {
        if (isLoggedIn) {
            $forNotLogin.addClass("d-none");
            $forLogin.removeClass("d-none");
            (json === undefined) ? $userNameButton.text("👤" + $("#loginUserName").val()) : $userNameButton.text("👤" + json.user);
        } else {
            $forNotLogin.removeClass("d-none");
            $forLogin.addClass("d-none");
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
            })
    })

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
                navbarChange(true);
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
            navbarChange(false);
            $("#logoutModal").modal("hide");
            window.location.reload();
        })
    })

})
