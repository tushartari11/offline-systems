package com.dipcoin.recon.common;

import com.dipcoin.db.services.model.BankTransaction;

public interface LienRemovalDelegate {

	public BankTransaction removeLien(BankTransaction bankTransaction);
}
