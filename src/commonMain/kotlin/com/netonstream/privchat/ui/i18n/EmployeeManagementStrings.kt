package com.netonstream.privchat.ui.i18n

import androidx.compose.runtime.Composable

data class EmployeeManagementStrings(
    val detail: String, val manage: String, val edit: String, val rebind: String, val binding: String,
    val currentBinding: String, val targetBinding: String, val chooseSession: String,
    val newSession: String, val existingSession: String, val preview: String, val confirm: String,
    val impact: String, val historyConsent: String, val privateOnly: String,
    val pendingOther: String, val conflict: String, val ownerOnly: String,
    val pendingNew: String, val noSessionTitle: String, val keepEditing: String,
    val discardTitle: String, val discardMessage: String, val nativeSession: String, val inFlight: String,
    val reloadBinding: String, val reloadBindingHint: String,
)

val employeeManagementStrings: EmployeeManagementStrings
    @Composable get() = employeeManagementStringsFor(PrivChatI18n.languageTag)

fun employeeManagementStringsFor(tag: String): EmployeeManagementStrings = when {
    tag.startsWith("zh-TW", true) || tag.startsWith("zh-HK", true) || tag.startsWith("zh-Hant", true) -> EmployeeManagementStrings(
        "員工詳情", "管理員工", "編輯資料", "更換工作綁定", "工作綁定", "目前綁定", "目標綁定", "目標會話",
        "下次交辦時新建會話", "選擇已有安全私有會話", "預覽變更", "確認更換綁定",
        "保留員工身分與舊歷史，不遷移舊會話上下文、執行器帳號或檔案。已有目標會話保留自身上下文；新會話尚未建立。只影響後續新交辦，不取消在途工作，不修改空間授權或專案目錄；原關聯可能因此不可交辦。",
        "我理解並允許此員工存取所選私有會話的已有歷史。", "僅提供已授權裝置上終態、未綁定的託管私有會話；不匯入外部會話。",
        "另有未確認的員工操作，請先在原入口恢復，不能另發請求。", "工作綁定已變更。草稿保留，請返回重新核對；不會自動替換版本。", "只有員工主人可管理全域資料及工作綁定。",
        "尚未建立；下次接受交辦時才建立新會話", "未命名的私有會話", "繼續編輯", "放棄未儲存的修改？", "返回將放棄此頁草稿；不會清除尚未確認的已提交請求。",
        "原生會話", "員工仍有未完成或待核對的工作。請先回到原任務／會話處理執行、審批或自動推進；換綁不會自動取消。",
        "放棄草稿並載入最新綁定", "將放棄目前未提交的換綁草稿，重新讀取目前工作綁定與版本；不會提交換綁，也不會清除未確認的請求。",
    )
    tag.startsWith("zh", true) -> EmployeeManagementStrings(
        "员工详情", "管理员工", "编辑资料", "更换工作绑定", "工作绑定", "当前绑定", "目标绑定", "目标会话",
        "下次交办时新建会话", "选择已有安全私有会话", "预览变更", "确认更换绑定",
        "保留员工身份与旧历史，不迁移旧会话上下文、执行器账号或文件。已有目标会话保留自身上下文；新会话尚未创建。只影响后续新交办，不取消在途工作，不修改空间授权或项目目录；原关联可能因此不可交办。",
        "我理解并允许此员工访问所选私有会话的已有历史。", "仅提供已授权设备上终态、未绑定的托管私有会话；不导入外部会话。",
        "另有未确认的员工操作，请先在原入口恢复，不能另发请求。", "工作绑定已变化。草稿保留，请返回重新核对；不会自动替换版本。", "只有员工主人可管理全局资料及工作绑定。",
        "尚未创建；下次接受交办时才创建新会话", "未命名的私有会话", "继续编辑", "放弃未保存的修改？", "返回将放弃此页草稿；不会清除尚未确认的已提交请求。",
        "原生会话", "员工仍有未完成或待核对的工作。请先回到原任务／会话处理执行、审批或自动推进；换绑不会自动取消。",
        "放弃草稿并加载最新绑定", "将放弃当前未提交的换绑草稿，重新读取当前工作绑定与版本；不会提交换绑，也不会清除未确认的请求。",
    )
    tag.startsWith("ja", true) -> EmployeeManagementStrings(
        "従業員の詳細", "管理", "プロフィールを編集", "作業環境を変更", "作業環境", "現在の環境", "変更先の環境", "変更先のセッション",
        "次の依頼時にセッションを新規作成", "安全な既存のプライベートセッションを選択", "変更を確認", "環境の変更を確定",
        "従業員のIDと過去の履歴は保持します。旧セッションの文脈、認証やファイルは移行しません。既存の変更先は自身の文脈を保持し、新規セッションはまだ作成されません。今後の依頼のみが対象で、進行中の処理、スペースの権限やプロジェクトのディレクトリは変更しません。既存の関連先に依頼できなくなる場合があります。",
        "選択したプライベートセッションの既存履歴へのアクセスを許可します。", "許可済みデバイス上で終了済みかつ未割り当ての管理セッションのみ選べます。外部セッションは取り込みません。",
        "未確認の操作があります。元の画面で復旧してから続けてください。", "作業環境が変更されています。下書きは保持されています。戻って確認してください。版は自動更新しません。", "所有者のみがプロフィールと作業環境を管理できます。",
        "未作成です。次の依頼を受け付けた時に作成します", "無題のプライベートセッション", "編集を続ける", "未保存の変更を破棄しますか？", "この画面の下書きを破棄します。結果が未確認の送信済みリクエストは削除しません。",
        "ネイティブセッション", "未完了または未確認の処理があります。元のタスクやセッションで実行、承認、自動進行を確認してください。環境変更では自動キャンセルしません。",
        "下書きを破棄して最新の環境を取得", "未送信の下書きを破棄し、最新の作業環境と版を取得します。環境変更を送信せず、未確認のリクエストも削除しません。",
    )
    tag.startsWith("vi", true) -> EmployeeManagementStrings(
        "Chi tiết nhân viên", "Quản lý", "Sửa hồ sơ", "Đổi liên kết làm việc", "Liên kết làm việc", "Liên kết hiện tại", "Liên kết mới", "Phiên đích",
        "Tạo phiên khi nhận việc tiếp theo", "Chọn phiên riêng tư an toàn có sẵn", "Xem trước thay đổi", "Xác nhận đổi liên kết",
        "Giữ danh tính nhân viên và lịch sử cũ; không chuyển ngữ cảnh, tài khoản hay tệp. Phiên đích giữ ngữ cảnh riêng; phiên mới chưa được tạo. Chỉ áp dụng cho việc mới, không hủy việc đang chạy hay đổi quyền không gian hoặc thư mục dự án. Liên kết cũ có thể không còn giao việc được.",
        "Tôi cho phép nhân viên truy cập lịch sử của phiên riêng tư đã chọn.", "Chỉ chọn phiên được quản lý, đã kết thúc và chưa được gán trên thiết bị được cấp quyền; không nhập phiên bên ngoài.",
        "Có thao tác nhân viên chưa được xác nhận. Hãy khôi phục tại màn hình ban đầu trước.", "Liên kết đã thay đổi. Giữ bản nháp; quay lại kiểm tra. Không tự đổi phiên bản.", "Chỉ chủ nhân viên được quản lý hồ sơ và liên kết toàn cục.",
        "Chưa tạo; sẽ tạo khi nhận việc tiếp theo", "Phiên riêng tư chưa đặt tên", "Tiếp tục sửa", "Bỏ thay đổi chưa lưu?", "Bỏ bản nháp trang này nhưng không xóa yêu cầu đã gửi chưa xác nhận.",
        "Phiên gốc", "Nhân viên còn việc chưa hoàn tất hoặc chưa xác nhận. Xử lý thực thi, phê duyệt hoặc tiến trình tự động tại nhiệm vụ/phiên ban đầu; đổi liên kết không tự hủy việc.",
        "Bỏ bản nháp và tải liên kết mới nhất", "Bỏ bản nháp chưa gửi và đọc lại liên kết cùng phiên bản hiện tại; không gửi đổi liên kết hay xóa yêu cầu chưa xác nhận.",
    )
    else -> EmployeeManagementStrings(
        "Employee details", "Manage", "Edit profile", "Change work binding", "Work binding", "Current binding", "Target binding", "Target session",
        "Create a session on the next assignment", "Choose a safe existing private session", "Preview changes", "Confirm binding change",
        "Keep the employee identity and old history. Do not migrate the old context, executor account or files. An existing target keeps its own context; a new session has not been created yet. This affects future assignments only, does not cancel in-flight work, and does not change space permissions or project directories. Existing links may become unavailable for assignment.",
        "I understand and allow this employee to access the selected private session's existing history.", "Only terminal, unbound managed private sessions on authorized devices are offered. External sessions are not imported.",
        "Another employee operation is unconfirmed. Recover it at its original entry before sending a new request.", "The binding changed. Your draft is retained. Go back and verify it; the expected version is not replaced automatically.", "Only the employee owner can manage global profile and work bindings.",
        "Not created yet; created when the next assignment is accepted", "Untitled private session", "Keep editing", "Discard unsaved changes?", "Discard this page's draft without deleting an unconfirmed submitted request.",
        "Native session", "The employee still has unfinished or unconfirmed work. Resolve execution, approvals or automatic progress at the original task/session first. Rebinding does not cancel them.",
        "Discard draft and load current binding", "Discard this unsent draft and read the current binding and version. This does not submit a rebind or delete an unconfirmed request.",
    )
}
