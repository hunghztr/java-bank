package com.bank.transaction.application.port.in;

import com.bank.transaction.domain.model.Transfer;
import java.util.UUID;

public interface GetTransferQuery {

    Transfer get(UUID transferId);
}
