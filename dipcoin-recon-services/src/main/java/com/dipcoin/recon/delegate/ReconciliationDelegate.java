package com.dipcoin.recon.delegate;

public interface ReconciliationDelegate {

	public boolean performMerchantReconciliation(Integer merchantId) throws Exception; // TODO to throw BusinessDelegate Exception
	
	public void performBankReconciliation(Integer bankId) throws Exception; // TODO to throw BusinessDelegate Exception
}
