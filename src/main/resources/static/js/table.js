const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

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
    return　(bytes / Math.pow(k, i)) //bytesをkのi乗でわる
            .toFixed(decimals) //小数点以下を設定どおりに切り捨て
        + ' ' + sizes[i]; //サイズの文字を追加
}

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
        {data: "size", title: "サイズ", render: function (data, type) {
                if (type === "display") {
                    return formatBytes(data);
                }
                return data;
            }
        },
        {data: "uploadDate", title: "アップロード日時"},
        {
            data: "uniqueFileName", title: "ダウンロード", render: function (data) {
                return `<a href="api/download?uniqueFileName=${data}"><img src="img/dl.ico" width="16" height="16" alt="ダウンロード"></a>`
            }
        },
        {
            data: null, title: "削除", render: function (data, type, row) {
                if (row.owned === true) {
                    return `
                        <form action="api/delete" method="post">
                        <input type="hidden" name="uniqueFileName" value="${row.uniqueFileName}">
                        <input type="hidden" name="_csrf" value="${csrfToken}" />
                        <input type="image" src="img/gomibako.ico" width="16" height="16" alt="削除" onclick="return confirm('本当に削除しますか?')">
                        </form>`;
                }
                return null;
            }
        }
    ]
});

/**
 * テーブル更新用関数
 */
function tableRefresh() {
    table.ajax.reload();
}