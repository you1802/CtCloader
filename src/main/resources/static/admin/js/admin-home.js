$(function () {
    'use strict';

    // ---------- CSRF：ヘッダーの meta から取り、全 AJAX に付ける ----------
    var csrfToken = $('meta[name="_csrf"]').attr('content');
    var csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    $.ajaxSetup({
        beforeSend: function (xhr) {
            if (csrfToken && csrfHeader) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        }
    });

    // ---------- 共通ヘルパー ----------
    function showAlert(type, message) {
        // type: 'success' / 'danger' / 'warning' / 'info'
        var $alert = $('<div>')
            .addClass('alert alert-' + type + ' alert-dismissible fade show')
            .attr('role', 'alert')
            .text(message);
        var $close = $('<button>')
            .attr('type', 'button')
            .addClass('btn-close')
            .attr('data-bs-dismiss', 'alert')
            .attr('aria-label', '閉じる');
        $alert.append($close);
        $('#alertArea').empty().append($alert);
    }

    function formatSize(bytes) {
        if (bytes == null) {
            return '-';
        }
        var units = ['B', 'KB', 'MB', 'GB', 'TB'];
        var size = Number(bytes);
        var i = 0;
        while (size >= 1024 && i < units.length - 1) {
            size /= 1024;
            i++;
        }
        return (i === 0 ? size : size.toFixed(1)) + ' ' + units[i];
    }

    function formatDateTime(value) {
        if (!value) {
            return '-';
        }
        // "yyyy-MM-dd HH:mm:ss" のまま、または ISO 形式 "2026-11-17T01:00:00" の両方に対応
        return String(value).replace('T', ' ').replace(/\.\d+$/, '');
    }

    function textOrDash(value) {
        return (value == null || value === '') ? '-' : String(value);
    }

    // ---------- DataTables 日本語化 ----------
    var dataTablesLanguage = {
        emptyTable: 'テーブルにデータがありません',
        info: '_TOTAL_ 件中 _START_ から _END_ まで表示',
        infoEmpty: '0 件中 0 から 0 まで表示',
        infoFiltered: '（全 _MAX_ 件より抽出）',
        infoThousands: ',',
        lengthMenu: '_MENU_ 件表示',
        loadingRecords: '読み込み中...',
        processing: '処理中...',
        search: '検索:',
        zeroRecords: '一致するレコードがありません',
        paginate: {
            first: '先頭',
            last: '最終',
            next: '次',
            previous: '前'
        },
        aria: {
            sortAscending: ': 列を昇順に並べ替えるにはアクティブにする',
            sortDescending: ': 列を降順に並べ替えるにはアクティブにする'
        }
    };

    // ファイル一覧の列定義（AdminForumFileDetail）。ユーザーファイル一覧でも使い回す
    var fileColumns = [
        { data: 'filename' },
        {
            data: 'size',
            render: function (data) { return formatSize(data); }
        },
        {
            data: 'uploadDate',
            render: function (data) { return formatDateTime(data); }
        },
        {
            data: 'lastDownloadDate',
            render: function (data) { return formatDateTime(data); }
        },
        { data: 'owner' },
        {
            data: 'downloadLocked',
            render: function (data) {
                return data
                    ? '<span class="badge text-bg-warning">ロック</span>'
                    : '<span class="badge text-bg-secondary">なし</span>';
            }
        },
        {
            data: 'visible',
            render: function (data) {
                return data
                    ? '<span class="badge text-bg-success">公開</span>'
                    : '<span class="badge text-bg-secondary">非公開</span>';
            }
        },
        {
            data: 'reportCount',
            render: function (data) {
                var count = Number(data) || 0;
                return count > 0
                    ? '<span class="badge text-bg-danger">' + count + ' 件</span>'
                    : '<span class="badge text-bg-secondary">0 件</span>';
            }
        },
        {
            data: null,
            orderable: false,
            searchable: false,
            render: function (data, type, row) {
                return '<div class="btn-group btn-group-sm" role="group">' +
                    '<button type="button" class="btn btn-outline-primary btn-detail" data-file-id="' + row.fileID + '">詳細</button>' +
                    '<button type="button" class="btn btn-outline-secondary btn-download" data-file-id="' + row.fileID + '">ダウンロード</button>' +
                    '<button type="button" class="btn btn-outline-danger btn-delete-file" data-file-id="' + row.fileID + '" data-filename="' + $('<div>').text(row.filename).html() + '">削除</button>' +
                    '</div>';
            }
        }
    ];

    // ---------- ファイル一覧 ----------
    var fileTable = $('#fileTable').DataTable({
        ajax: {
            url: '/admin/api/forum-file-details.json',
            dataSrc: ''
        },
        columns: fileColumns,
        order: [[2, 'desc']],
        language: dataTablesLanguage
    });

    $('#reloadFilesBtn').on('click', function () {
        fileTable.ajax.reload(null, false);
    });

    // ---------- ユーザー一覧 ----------
    var userTable = $('#userTable').DataTable({
        ajax: {
            url: '/admin/user_list',
            dataSrc: ''
        },
        columns: [
            { data: 'username' },
            {
                data: 'lastLoginAt',
                render: function (data) { return formatDateTime(data); }
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                render: function (data, type, row) {
                    return '<div class="btn-group btn-group-sm" role="group">' +
                        '<button type="button" class="btn btn-outline-primary btn-user-detail" data-username="' + row.username + '">詳細</button>' +
                        '<button type="button" class="btn btn-outline-danger btn-delete-user" data-username="' + row.username + '">削除</button>' +
                        '</div>';
                }
            }
        ],
        order: [[0, 'asc']],
        language: dataTablesLanguage
    });

    $('#reloadUsersBtn').on('click', function () {
        userTable.ajax.reload(null, false);
    });

    // ---------- ログイン履歴・ユーザーファイル一覧（選択後に AJAX で読み込む） ----------
    var loginHistoryTable = $('#loginHistoryTable').DataTable({
        data: [],
        columns: [
            {
                data: 'loginAt',
                render: function (data) { return formatDateTime(data); }
            },
            { data: 'ip' },
            {
                data: 'success',
                render: function (data) {
                    return data
                        ? '<span class="badge text-bg-success">成功</span>'
                        : '<span class="badge text-bg-danger">失敗</span>';
                }
            }
        ],
        order: [[0, 'desc']],
        language: dataTablesLanguage
    });

    // ユーザーファイル一覧は所有者列を除いた fileColumns を使う
    var userFileColumns = $.map(fileColumns, function (col) {
        return col.data === 'owner' ? null : col;
    });
    var userFileTable = $('#userFileTable').DataTable({
        data: [],
        columns: userFileColumns,
        order: [[2, 'desc']],
        language: dataTablesLanguage
    });

    // タブを切り替えたときに DataTables の幅を直す
    $('button[data-bs-toggle="tab"]').on('shown.bs.tab', function () {
        $.fn.dataTable.tables({ visible: true, api: true }).columns.adjust();
    });

    // ---------- ファイル詳細モーダル ----------
    var currentFileId = null;
    var currentFileRow = null;

    function openFileDetail(fileID, row) {
        currentFileId = fileID;
        currentFileRow = row || null;

        // 一覧の情報はすぐ表示し、詳細 API の分は取得後に埋める
        if (row) {
            $('#detailFilename').text(textOrDash(row.filename));
            $('#detailFileId').text(textOrDash(row.fileID));
            $('#detailOwner').text(textOrDash(row.owner));
            $('#detailSize').text(formatSize(row.size));
            $('#detailUploadDate').text(formatDateTime(row.uploadDate));
            $('#detailLastDownloadDate').text(formatDateTime(row.lastDownloadDate));
            $('#detailDownloadLocked').text(row.downloadLocked ? 'ロック' : 'なし');
            $('#detailVisible').text(row.visible ? '公開' : '非公開');
            $('#detailReportCount').text(textOrDash(row.reportCount));
        } else {
            $('#detailFilename').text('-');
            $('#detailFileId').text(textOrDash(fileID));
            $('#detailOwner').text('-');
            $('#detailSize').text('-');
            $('#detailUploadDate').text('-');
            $('#detailLastDownloadDate').text('-');
            $('#detailDownloadLocked').text('-');
            $('#detailVisible').text('-');
            $('#detailReportCount').text('-');
        }
        $('#detailComment').text('読み込み中...');
        $('#detailIpAddress').text('読み込み中...');
        $('#detailDownloadCount').text('読み込み中...');
        $('#detailFileReports').empty();

        bootstrap.Modal.getOrCreateInstance($('#fileDetailModal')[0]).show();

        $.ajax({
            url: '/admin/api/target-file-detail',
            type: 'GET',
            data: { fileID: fileID },
            dataType: 'json'
        }).done(function (detail) {
            $('#detailComment').text(textOrDash(detail.comment));
            $('#detailIpAddress').text(textOrDash(detail.ipAddress));
            $('#detailDownloadCount').text(textOrDash(detail.downloadCount));

            var reports = detail.fileReports || [];
            var $list = $('#detailFileReports');
            $list.empty();
            if (reports.length === 0) {
                $list.append($('<li>').addClass('list-group-item').text('問題報告はありません。'));
            } else {
                $.each(reports, function (i, report) {
                    // FileReport: comment（ファイルに対する問題）と
                    // ipAddress（報告者のIP）を表示する
                    var $item = $('<li>').addClass('list-group-item');
                    var $comment = $('<div>').text(textOrDash(report.comment));
                    var $ip = $('<small>').addClass('text-muted')
                        .text('報告者IP：' + textOrDash(report.ipAddress));
                    $item.append($comment).append($ip);
                    $list.append($item);
                });
            }
        }).fail(function (jqXHR) {
            $('#detailComment').text('-');
            $('#detailIpAddress').text('-');
            $('#detailDownloadCount').text('-');
            $('#detailFileReports').empty()
                .append($('<li>').addClass('list-group-item').text('詳細の取得に失敗しました。'));
            showAlert('danger', 'ファイル詳細の取得に失敗しました。（' + jqXHR.status + '）');
        });
    }

    // ファイル一覧・ユーザーファイル一覧の詳細 / ダウンロード / 削除ボタン（委譲）
    $('#fileTable tbody, #userFileTable tbody')
        .on('click', '.btn-detail', function () {
            var $btn = $(this);
            var table = $btn.closest('table').DataTable();
            var row = table.row($btn.closest('tr')).data();
            openFileDetail($btn.data('file-id'), row);
    })
        .on('click', '.btn-download', function () {
            downloadFile($(this).data('file-id'));
    })
        .on('click', '.btn-delete-file', function () {
            var $btn = $(this);
            openConfirm(
                'ファイル「' + $btn.data('filename') + '」を削除しますか？この操作は取り消せません。',
                function () {
                    deleteFile($btn.data('file-id'));
                });
    });

    // ---------- ダウンロード ----------
    // GET /admin/api/download はバイナリを返すため、AJAX ではなく
    // ブラウザのダウンロードとして開く（ページ遷移はしない）
    function downloadFile(fileID) {
        window.location.href = '/admin/api/download?fileID=' + encodeURIComponent(fileID);
    }

    $('#detailDownloadBtn').on('click', function () {
        if (currentFileId) {
            downloadFile(currentFileId);
        }
    });

    // ---------- ログアウト ----------
    $('#logoutBtn').on('click', function () {
        $.ajax({
            url: '/api/logout',
            type: 'POST',
            dataType: 'json'
        }).done(function (res) {
            if (res && res.code === 200) {
                window.location.href = '/CtCloader';
            } else {
                showAlert('danger', (res && res.message) || 'ログアウトに失敗しました。');
            }
        }).fail(function (jqXHR) {
            showAlert('danger', 'ログアウトに失敗しました。（' + jqXHR.status + '）');
        });
    });

    // ---------- 削除確認モーダル ----------
    var pendingConfirmAction = null;

    function openConfirm(message, onConfirm) {
        $('#confirmMessage').text(message);
        pendingConfirmAction = onConfirm;
        bootstrap.Modal.getOrCreateInstance($('#confirmModal')[0]).show();
    }

    $('#confirmButton').on('click', function () {
        bootstrap.Modal.getInstance($('#confirmModal')[0]).hide();
        if (pendingConfirmAction) {
            pendingConfirmAction();
            pendingConfirmAction = null;
        }
    });

    // ---------- ファイル削除 ----------
    function deleteFile(fileID) {
        $.ajax({
            url: '/admin/api/delete_file',
            type: 'POST',
            data: { fileID: fileID },
            dataType: 'json'
        }).done(function (res) {
            if (res && res.code === 200) {
                showAlert('success', res.message || 'ファイルを削除しました。');
                fileTable.ajax.reload(null, false);
                userFileTable.ajax.reload(null, false);
                var detailModal = bootstrap.Modal.getInstance($('#fileDetailModal')[0]);
                if (detailModal) {
                    detailModal.hide();
                }
            } else {
                showAlert('danger', (res && res.message) || 'ファイルの削除に失敗しました。');
            }
        }).fail(function (jqXHR) {
            showAlert('danger', 'ファイルの削除に失敗しました。（' + jqXHR.status + '）');
        });
    }

    $('#detailDeleteBtn').on('click', function () {
        if (!currentFileId) {
            return;
        }
        var filename = currentFileRow ? currentFileRow.filename : currentFileId;
        bootstrap.Modal.getInstance($('#fileDetailModal')[0]).hide();
        openConfirm(
            'ファイル「' + filename + '」を削除しますか？この操作は取り消せません。',
            function () { deleteFile(currentFileId); }
        );
    });

    // ---------- ユーザー詳細 ----------
    var currentUsername = null;

    function loadUserDetail(username) {
        currentUsername = username;
        $('#userDetailName').text(username);
        $('#userDetailSection').removeClass('d-none');

        // ログイン履歴
        $.ajax({
            url: '/admin/api/target_user_login_history',
            type: 'GET',
            data: { username: username },
            dataType: 'json'
        }).done(function (list) {
            loginHistoryTable.clear().rows.add(list || []).draw();
        }).fail(function (jqXHR) {
            loginHistoryTable.clear().draw();
            showAlert('danger', 'ログイン履歴の取得に失敗しました。（' + jqXHR.status + '）');
        });

        // そのユーザーのファイル
        $.ajax({
            url: '/admin/api/target-user-file-details.json',
            type: 'GET',
            data: { username: username },
            dataType: 'json'
        }).done(function (list) {
            userFileTable.clear().rows.add(list || []).draw();
        }).fail(function (jqXHR) {
            userFileTable.clear().draw();
            showAlert('danger', 'ユーザーファイルの取得に失敗しました。（' + jqXHR.status + '）');
        });
    }

    $('#userTable tbody')
        .on('click', '.btn-user-detail', function () {
            loadUserDetail($(this).data('username'));
        })
        .on('click', '.btn-delete-user', function () {
            var username = $(this).data('username');
            openConfirm(
                'ユーザー「' + username + '」を削除しますか？この操作は取り消せません。',
                function () {
                    deleteUser(username);
                }
            );
    });

    $('#closeUserDetailBtn').on('click', function () {
        $('#userDetailSection').addClass('d-none');
        currentUsername = null;
        loginHistoryTable.clear().draw();
        userFileTable.clear().draw();
    });

    $('#deleteUserBtn').on('click', function () {
        if (!currentUsername) {
            return;
        }
        var username = currentUsername;
        openConfirm(
            'ユーザー「' + username + '」を削除しますか？この操作は取り消せません。',
            function () { deleteUser(username); }
        );
    });

    // ---------- ユーザー削除 ----------
    function deleteUser(username) {
        $.ajax({
            url: '/admin/api/delete_user',
            type: 'POST',
            data: { username: username },
            dataType: 'json'
        }).done(function (res) {
            if (res && res.code === 200) {
                showAlert('success', res.message || 'ユーザーを削除しました。');
                userTable.ajax.reload(null, false);
                if (currentUsername === username) {
                    $('#userDetailSection').addClass('d-none');
                    currentUsername = null;
                    loginHistoryTable.clear().draw();
                    userFileTable.clear().draw();
                }
            } else {
                showAlert('danger', (res && res.message) || 'ユーザーの削除に失敗しました。');
            }
        }).fail(function (jqXHR) {
            showAlert('danger', 'ユーザーの削除に失敗しました。（' + jqXHR.status + '）');
        });
    }
});
