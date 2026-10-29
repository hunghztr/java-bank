package com.bank.common.events;

public final class Topics {

    /** transaction-service gửi lệnh trừ / cộng / hoàn tiền cho account-service */
    public static final String ACCOUNT_COMMANDS = "bank.account.commands";
    /** account-service trả kết quả xử lý lệnh */
    public static final String ACCOUNT_REPLIES = "bank.account.replies";
    /** Sự kiện kết thúc giao dịch (hoàn tất / thất bại) cho notification, audit, reporting */
    public static final String TRANSFER_EVENTS = "bank.transfer.events";

    /** Header Kafka chứa tên loại message để consumer giải mã */
    public static final String HEADER_MESSAGE_TYPE = "messageType";

    private Topics() {
    }
}
