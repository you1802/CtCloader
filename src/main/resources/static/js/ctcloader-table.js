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

/*
ファイルのダウンロードとアップロード関係
 */
const $fileControlModal = $("#fileControlModal");
const $targetFileName = $("#targetFileName");
const $fileControlModalTitle = $("#fileControlModalTitle");
const $fileControlExecuteButton = $("#fileControlExecuteButton");
const $fileControlPassword = $("#fileControlPassword");

//ファイルのダウンロードまたはアップロードのパスワード入力用モーダルのイベントリスナー
$fileControlModal.on("hidden.bs.modal", function () {
    $fileControlPassword.val("");
})

//ファイル削除用モーダルを呼び出す
function fileDeleteModal(targetFileName) {
    $targetFileName.val(targetFileName);
    $fileControlModalTitle.text("ファイルの削除");
    $fileControlExecuteButton.text("削除").removeClass("btn-success").addClass("btn-danger").off().on("click", function () {
        deleteFile($targetFileName.val(), $fileControlPassword.val());
    })
    $fileControlModal.modal("show");
}

//パスワードロックされたファイルダウンロード用モーダルを呼び出す
function lockedFileDownloadModal(targetFileName) {
    $targetFileName.val(targetFileName);
    $fileControlModalTitle.text("ファイルのダウンロード");
    $fileControlExecuteButton.text("ダウンロード").removeClass("btn-danger").addClass("btn-success").off().on("click", function () {
        downloadFile($targetFileName.val(), $fileControlPassword.val());
    })
    $fileControlModal.modal("show");
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