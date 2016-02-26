package com.dipcoin.recon.common;

import com.dipcoin.db.services.model.BankTransaction;

public interface DebitToAccountDelegate {

	public BankTransaction debitToAccount(BankTransaction bankTransaction);

	public BankTransaction debitToAccount(Integer bankTransactionId);
}
