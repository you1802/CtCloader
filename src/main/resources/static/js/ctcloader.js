/*
以下はHTMLを読み込み後に呼ばれる
 */
$(function () {
     'use strict';

    //ヘッダーにあるcsrfトークン用ヘッダーを取得
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    //csrfトークンを取得する関数
    function getCsrfToken() {
        return $("meta[name='_csrf']").attr("content");
    }

    /*
    テーブル関係
     */
    //テーブル自体の設定
    const table = new DataTable("#files-table", {
        language: {
            url: "https://cdn.datatables.net/plug-ins/3.0.1/i18n/ja.json",
        },
        order: [], // 初期ソートを無効化
        columnDefs: [
            {className: "dt-center", "targets": "_all"},
            //{"orderable": false, "targets": [3, 4]},
            //{"width": 120, "targets": 3},
            {"width": 60, "targets": [0, 2]},
            {searchable: false, "targets": [0, 2, 3, 4, 5]}
        ],
        ajax: {
            url: "/api/file-details.json",
            dataSrc: ""
        },
        columns: [
            {
                data: "owned", render: function () {

                }},
            {
                data: "name", title: "ファイル名", render: function (data, type, row) {
                    if (type === "display") {
                        return `
                        <button type="button" class="btn btn-link text-start text-decoration-none p-0" onclick="openFileDetailModal('${row.fileID}')">
                        ${data}
                        </button>
                        `;
                    }
                }},
            {
                data: "downloadLock", render: function () {

                }},
            {
                data: "size", title: "サイズ", render: function (data, type) {
                    if (type === "display") {
                        return formatBytes(data);
                    }
                    return data;
                }},
            {data: "uploadDate", title: "アップロード日時"},
            {data: "lastDownloadDate", title: "最終ダウンロード日時"},

            // {
            //     data: "uniqueFileName", title: "ダウンロード", render: function (data, type, row) {
            //         if (row.downloadLock === true) {
            //             return `
            //         <button type="button" class="btn btn-link" onclick="lockedFileDownloadModal('${row.uniqueFileName}')">
            //         <i class="bi bi-file-earmark-lock"></i>
            //         </button>
            //         `
            //         } else {
            //             return `<a href="api/download?targetFileName=${data}&token=" class="icon-link"><i class="bi-download"></a>`
            //         }
            //     }
            // },

            // {
            //     data: null, title: "削除", render: function (data, type, row) {
            //         //if (!row.owned === true) {return null;}
            //         return `
            //         <button type="button" class="btn btn-link" onclick="fileDeleteModal('${row.uniqueFileName}')">
            //         <i class="bi bi-trash"></i>
            //         </button>
            //         `
            //     }
            // }
        ]
    });

    //テーブル更新用関数
    function tableRefresh() {
        table.ajax.reload();
    }

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
        return parseFloat((bytes / Math.pow(k, i)) //bytesをkのi乗でわる
                .toFixed(decimals)) //小数点以下を設定どおりに切り捨て
            + ' ' + sizes[i]; //サイズの文字を追加
    }

    /*
    ファイルのダウンロードとアップロード関係
     */
    const $fileControlModal = $("#fileControlModal");
    const $fileID = $("#fileID");
    const $fileControlModalTitle = $("#fileControlModalTitle");
    const $fileControlExecuteButton = $("#fileControlExecuteButton");
    const $fileControlPassword = $("#fileControlPassword");
    const $fileDetailModal = $("#fileDetailModal");

    //ファイル個別メニュー表示用
    window.openFileDetailModal = function(fileID) {
        bootstrap.Modal.getOrCreateInstance($fileDetailModal[0]).show();
    };


    //ファイルのダウンロードまたはアップロードのパスワード入力用モーダルのイベントリスナー
    $fileControlModal.on("hidden.bs.modal", function () {
        $fileControlPassword.val("");
    })

    //ファイル削除用モーダルを呼び出す
    function fileDeleteModal(fileID) {
        $fileID.val(fileID);
        $fileControlModalTitle.text("ファイルの削除");
        $fileControlExecuteButton.text("削除").removeClass("btn-success").addClass("btn-danger").off().on("click", function () {
            deleteFile($fileID.val(), $fileControlPassword.val());
        })
        $fileControlModal.modal("show");
    }

    //パスワードロックされたファイルダウンロード用モーダルを呼び出す
    function lockedFileDownloadModal(fileID) {
        $fileID.val(fileID);
        $fileControlModalTitle.text("ファイルのダウンロード");
        $fileControlExecuteButton.text("ダウンロード").removeClass("btn-danger").addClass("btn-success").off().on("click", function () {
            downloadFile($fileID.val(), $fileControlPassword.val());
        })
        $fileControlModal.modal("show");
    }

    /**
     * パスワードが一致する場合、サーバー側からワンタイムトークンを受け取りファイルをダウンロードする
     * @param fileID ダウンロードするファイル名
     * @param fileControlPassword ダウンロードするファイルのパスワード
     */
    function downloadFile(fileID, fileControlPassword) {
        $.ajax({
            url: "/api/download/auth",
            type: "POST",
            dataType: "json",
            data: {
                fileID: fileID,
                fileControlPassword: fileControlPassword
            }
        }).done(function (data) {
            if (data.code === 200) {
                window.location.href = `/api/download?fileID=${fileID}&token=${data.token}`;
            } else {
                showToast(data.message, "danger");
            }
        }).fail(function () {
            showToast("通信エラー", "danger");
        })
    }

    /**
     * 削除用関数
     * @param fileID 削除するファイルid
     * @param fileControlPassword 削除用パスワード
     */
    function deleteFile(fileID, fileControlPassword) {
        $.ajax({
            url: "/api/delete",
            type: "POST",
            dataType: 'json',
            data: {
                targetFileName: fileID,
                fileControlPassword: fileControlPassword
            }
        }).done(function (data) {
            if (data.code === 200) {
                showToast(data.message, "success");
            } else {
                showToast(data.message, "danger");
            }
            tableRefresh();
            capacityBarUpdate();
        })
            .fail(function () {
                showToast("通信エラー", "danger");
            })
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

    const $uploadComment = $("#uploadComment");

    const $setDeletePassword = $("#setDeletePassword");
    const $setDeletePassword_div = $("#setDeletePassword_div");
    const $confirmSetDeletePassword = $("#confirmSetDeletePassword");

    const $downloadPasswordEnabled = $("#downloadPasswordEnabled");
    const $downloadPasswordEnabledLabel = $("#downloadPasswordEnabledLabel");
    const $visibleSetting = $("#visibleSetting");

    const $setDownloadPasswordGroup = $("#setDownloadPasswordGroup");
    const $setDownloadPassword = $("#setDownloadPassword");
    const $setDownloadPassword_div = $("#setDownloadPassword_div");
    const $confirmSetDownloadPassword = $("#confirmSetDownloadPassword");

    const $uploadButton = $("#uploadButton");
    const $uploadModalCloseButtonHeader = $("#uploadModalCloseButtonHeader");
    const $uploadModalCloseButtonFooter = $("#uploadModalCloseButtonFooter");

    const {Uppy, Dashboard, Tus} = window.Uppy;
    const ja_JP = window.Uppy.locales.ja_JP;
    const uppy = new Uppy({
        locale: ja_JP, //日本語化
        onBeforeUpload: (files) => { //ファイルにパスワードなどのメタ情報を付与する
            Object.keys(files).forEach(fileId => {
                files[fileId].meta.downloadPassword = $setDownloadPassword.val();
                files[fileId].meta.deletePassword = $setDeletePassword.val();
                files[fileId].meta.downloadPasswordEnabled = $downloadPasswordEnabled.prop("checked");
                files[fileId].meta.visibleSetting = $visibleSetting.prop("checked");
                files[fileId].meta.comment = $uploadComment.val();
            })
        }
    })
        .use(Dashboard, {
            target: "#uploader",
            inline: true,
            hideUploadButton: true,
            doneButtonHandler: () => { //doneボタンを押したときモーダルを閉じる
                $uploadModal.modal("hide");
            }
        })
        .use(Tus, { //Tusプロトコルを指定
            endpoint: "/api/upload", //エンドポイントの指定
            headers: () => {
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
        capacityBarUpdate();
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

    //アップロードボタンが有効か無効か切り替え
    function switchUploadButton() {
        const {totalProgress} = uppy.getState();
        if (totalProgress > 0) return; //アップロード中かアップロード完了時は処理しない

        if ($downloadPasswordEnabled.prop("checked")) {
            if (uppy.getFiles().length > 0 &&  $setDeletePassword[0].checkValidity() && $confirmSetDeletePassword.hasClass("is-valid") && $setDownloadPassword[0].checkValidity() && $confirmSetDownloadPassword.hasClass("is-valid")) {
                $uploadButton.prop("disabled", false);
            } else {
                $uploadButton.prop("disabled", true);
            }
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
        //共通のバリデーションチェック(ファイルが存在しダウンロードパスワードが有効かどうか)
        if (!(uppy.getFiles().length > 0 && $setDeletePassword[0].checkValidity() && $confirmSetDeletePassword.hasClass("is-valid"))) return;

        //ダウンロードパスワードを設定してる場合のバリデーション
        if ($downloadPasswordEnabled.prop("checked")) {
            if (!($setDownloadPassword[0].checkValidity() && $confirmSetDownloadPassword.hasClass("is-valid"))) return;
        }

        //フォームを編集不可に
        $uploadComment.prop("readOnly", true);

        $setDeletePassword.prop("readOnly", true).addClass("is-valid");
        $confirmSetDeletePassword.prop("readOnly", true);

        $downloadPasswordEnabled.addClass("is-readonly");
        $downloadPasswordEnabledLabel.addClass("is-readonly");

        $uploadButton.prop("disabled", true);
        $uploadModalCloseButtonHeader.prop("disabled", true);
        $uploadModalCloseButtonFooter.prop("disabled", true);

        $setDownloadPassword.prop("readOnly", true).addClass("is-valid");
        $confirmSetDownloadPassword.prop("readOnly", true);

        //アップロード実行
        uploadFile();
    });

    //アップロードモーダルが閉じられた時,アップロード中でないのならモーダル内の情報をリセットしテーブルを更新する
    $uploadModal.on("hidden.bs.modal", function () {
        if (Object.keys(uppy.getState().currentUploads).length > 0) return

        uppy.clear();

        $("#uploadForm")[0].reset();

        $uploadComment.prop("readOnly", false);

        $confirmSetDeletePassword.removeClass("is-valid is-invalid").prop("readOnly", false);
        $setDeletePassword.removeClass("is-valid").prop("readOnly", false);
        $setDeletePassword_div.removeClass("was-validated");

        $downloadPasswordEnabled.removeClass("is-readonly");
        $downloadPasswordEnabledLabel.removeClass("is-readonly");
        $setDownloadPasswordGroup.addClass("d-none");

        $setDownloadPassword.removeClass("is-valid").prop("readOnly", false);
        $confirmSetDownloadPassword.removeClass("is-valid is-invalid").prop("readOnly", false);
        $setDownloadPassword_div.removeClass("was-validated");

        tableRefresh();
    });

    /*
    ユーザー登録関係
     */

    const $loggedIn = $("#loggedIn");
    const $notLoggedIn = $("#notLoggedIn");
    const $loginUserNameButton = $("#loginUserNameButton");
    const $userName = $("#userName");
    const $userPassword = $("#userPassword");
    const $confirmUserPassword = $("#confirmUserPassword");
    const $registerExecuteButton = $("#registerExecuteButton");
    const $userNameInfo = $("#userNameInfo");
    const $registerModal = $("#registerModal");
    const $registerForm = $("#registerForm");
    const $loginUserName = $("#loginUserName");
    const $registrationButton = $("#registrationButton");
    const $loginExecuteButton = $("#loginExecuteButton");
    const $loginUserPassword = $("#loginUserPassword")
    const $loginModal = $("#loginModal");
    const $loginForm = $("#loginForm");
    const $loginButton  =$("#loginButton");
    const $logoutButton = $("#logoutButton");
    const $logoutModal = $("#logoutModal");
    const $logoutExecuteButton = $("#logoutExecuteButton");
    const $userMenuButton = $("#userMenuButton");
    const $loadingSpinner = $("#loadingSpinner");
    const $serverRemainingCapacity = $("#serverRemainingCapacity");
    const $serverUsage = $("#serverUsage");
    const $userRemainingCapacity = $("#userRemainingCapacity");
    const $userUsage = $("#userUsage");
    const $userProgressBarArea = $("#userProgressBarArea");
    const $serverProgressBar = $("#serverProgressBar");
    const $userProgressBar = $("#userProgressBar");

    //スピナー表示用関数
    const spinner = {
        show: function () {$loadingSpinner.removeClass("d-none");},
        hide: function () {$loadingSpinner.addClass("d-none");}
    };

    //ログイン状態による表示ボタンの切替
    /**
     * ナビバーの表示を変更
     * @param isLoggedIn ログイン状態(true:ログイン済み false:未ログイン)
     * @param name ユーザー名の文字列
     */
    function navbarChange(isLoggedIn, name) {
        if (isLoggedIn) {
            $notLoggedIn.addClass("d-none");
            $loggedIn.removeClass("d-none");
            $userMenuButton.text("👤" + name);
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
                    navbarChange(true, json.user);
                }
                $(document).off('ajaxStop');
            });
    });

    //ユーザー登録モーダルのバリデーション系
    $registerForm.on("submit", function (e) {
        //イベント伝播停止
        e.preventDefault();
        e.stopPropagation();
        //バリに通らなかったらクラス付けて終了
        if (!this.checkValidity()) {
            $registerForm.addClass("was-validated");
            return;
        }
        //バリに通った場合
        $registerForm.addClass("was-validated");
        $registerExecuteButton.prop("disabled", true);  //通信中、ボタン無効
        spinner.show();

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
                showToast(data.message, "info");
                loginProcess($userName.val(), $userPassword.val()); //そのままログイン
                $registerModal.modal("hide");
            }  else {
                showToast(data.message, "danger");
            }
        }).fail(function () {
            showToast("通信エラー", "danger");
        }).always(function () {
            $registerExecuteButton.prop("disabled", false);
            spinner.hide();
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
        spinner.show();

        $.getJSON("/api/user_name_exists?username=" + userName)
            .done(function (json) {
                if (json.exists) {
                    inputEl.setCustomValidity("ユーザー名重複");
                    $userNameInfo.text("このユーザー名は既に存在します");
                    $userName.addClass("is-invalid");
                } else {
                    inputEl.setCustomValidity("");
                    $userName.removeClass("is-invalid");
                }
            })
            .always(function () {
                $registerExecuteButton.prop("disabled", false);
                spinner.hide();
            });
    });
    //入力時に判定をクリア
    $userName.on("input", function () {
        this.setCustomValidity("");
        $userName.removeClass("is-invalid");
        $userNameInfo.text("");
    });
    //パスワードダブルチェック
    function checkConfirmPassword() {
        const password = $userPassword.val();
        const confirmPassword = $confirmUserPassword.val();
        const conPassInputEl = $confirmUserPassword[0]; //生DOM取得
        //どちらかが空ならチェックしない
        if (password === "" || confirmPassword === "") {
            conPassInputEl.setCustomValidity("");
            $confirmUserPassword.removeClass("is-invalid");
            return;
        }
        if (password !== confirmPassword) {
            conPassInputEl.setCustomValidity("エラー");
            $confirmUserPassword.addClass("is-invalid");
        } else {
            conPassInputEl.setCustomValidity("");
            $confirmUserPassword.removeClass("is-invalid");
        }
    }
    $userPassword.on("input blur", checkConfirmPassword);
    $confirmUserPassword.on("input blur", checkConfirmPassword);

    //ユーザー登録モーダルを閉じたときフォームをリセットする
    $registerModal.on("hidden.bs.modal", function () {
        $registerForm[0].reset();
        $registerForm.removeClass("was-validated");
        $registerForm.find(".form-control").removeClass("is-invalid");
        $userNameInfo.text("エラー");
    });


    //ログイン実行ボタンのリスナー
    $loginForm.on("submit", function (e) {
        //イベント伝播停止
        e.preventDefault();
        e.stopPropagation();

        $loginForm.addClass("was-validated");
        if (this.checkValidity()) loginProcess(); //バリに通った場合
    });

    /**
     * ログイン処理　ログインフォーム以外からの呼び出し時、引数必須
     * @param userName 文字列
     * @param password 文字列
     */
    function loginProcess(userName, password) {
        const name = userName || $loginUserName.val();
        const pass = password || $loginUserPassword.val();
        $loginExecuteButton.prop("disabled", true);  //通信中、ボタン無効
        spinner.show();

        $.ajax({
            url: "/api/login",
            method: "POST",
            data: {
                username: name,
                password: pass
            }
        }).done(function (data) {
            if (data.code === 200) {
                $("meta[name='_csrf']").attr("content", data.csrfToken);
                navbarChange(true, name);
                showToast("ログインに成功しました", "info");$loginModal.modal("hide");tableRefresh();
                capacityBarUpdate();
            } else {
                const errorMsg = userName ? "自動ログインに失敗しました。ログイン画面からお試しください。" : "ユーザー名またはパスワードが一致しません";  //呼び出し場所によって文言変更
                showToast(errorMsg, "danger");$loginModal.modal("hide");
            }
        }).fail(function () {
            showToast("通信エラー", "danger");
        }).always(function () {
            $loginExecuteButton.prop("disabled", false);
            spinner.hide();
        });
    }

    //ログインモーダルを閉じたときフォームをリセット
    $loginModal.on("hidden.bs.modal", function () {
        $loginForm[0].reset();
        $loginForm.removeClass("was-validated");
    });


    //ログアウト実行ボタンのイベントリスナー
    $logoutExecuteButton.on("click", function () {
        $logoutExecuteButton.prop("disabled", true);    //通信の間、ボタン無効
        spinner.show();
        $.ajax({
            url: "/api/logout",
            method: "POST",
            dataType: "json",
        }).done(function (data) {
            navbarChange(false);
            $("meta[name='_csrf']").attr("content", data.csrfToken);
            tableRefresh();
            $logoutModal.modal("hide");
            showToast("ログアウトしました。", "info");
            capacityBarUpdate();
        }).fail(function () {
            showToast("通信エラー", "danger")
        }).always(function () {
            $logoutExecuteButton.prop("disabled", false);
            spinner.hide();
        });
    });

    /**
     * バーの見た目を変更
     * @param $barElement バーの要素
     * @param ratio 容量の割合
     */
    function progressBarUpdate($barElement, ratio) {
        $barElement.css("width", ratio + "%");  //バーの幅を変更
        //バーの色を変更
        $barElement.removeClass("text-bg-primary text-bg-warning text-bg-danger");
        if (ratio <= 70) {
            $barElement.addClass("text-bg-primary");
        } else if (ratio <= 90) {
            $barElement.addClass("text-bg-warning");
        } else {
            $barElement.addClass("text-bg-danger");
        }
    }

    /**
     * サーバー容量バーを更新
     */
    function capacityBarUpdate() {
        $.getJSON("/api/all_filesize_info")
            .done(function (json) {
                //サーバー側
                const serverRatio = json.forumFileSize / json.forumFileSizeLimit * 100;
                progressBarUpdate($serverProgressBar, serverRatio);
                $serverRemainingCapacity.text(`空き: ${formatBytes(json.forumFileSizeLimit - json.forumFileSize)}`);   //バーのラベル部分
                $serverUsage.text(`${formatBytes(json.forumFileSize)} / ${formatBytes(json.forumFileSizeLimit)}`);    //バーの中の部分
                //ユーザー側
                if (json.userFileSize === -1) {
                    $userProgressBarArea.addClass("d-none");
                } else {
                    const userRatio = json.userFileSize / json.userFileSizeLimit * 100;
                    progressBarUpdate($userProgressBar, userRatio);
                    $userRemainingCapacity.text(`残り: ${formatBytes(json.userFileSizeLimit - json.userFileSize)}`);
                    $userUsage.text(`${formatBytes(json.userFileSize)} / ${formatBytes(json.userFileSizeLimit)}`);
                    $userProgressBarArea.removeClass("d-none");
                }
            })
    }
    capacityBarUpdate();
});
