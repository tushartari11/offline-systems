package com.dipcoin.recon.common;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.Dipcoin;

@Component("removeExpiredDipcoinDelegate")
public class RemoveExpiredDipcoinDelegateHelper implements
		RemoveExpiredDipcoinDelegate {

	@Autowired
	DipcoinDBService coinDBService;

	@Autowired
	BankDBService bankDBService;

	@Autowired
	BankAPIServices bankAPIServices;

	@Autowired
	LienRemovalDelegate lienRemovalDelegate;

	@Override
	public boolean removeExpiredDipcoins() {
		try {
			Future<List<Dipcoin>> asyncResults = coinDBService
					.getExpiredDipcoins();
			List<Dipcoin> expiredDipcoins = asyncResults.get();
			for (Dipcoin dipcoin : expiredDipcoins) {
				createAndFireTransactionsForLienRemoval(dipcoin);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * This method fetches the <code>DipcoinTransactions</code> for the passed
	 * DipCoin and creates a transaction to be sent <b>for Lien Removal</b> to
	 * the Bank Services
	 * 
	 * @param dipcoin theDipcoin which is expired
	 */

	private void createAndFireTransactionsForLienRemoval(Dipcoin dipcoin) {
		BankTransaction bankTransaction = new BankTransaction();
		bankTransaction.setAmount(dipcoin.getAmount());
		bankTransaction.setDipcoinTransaction(dipcoin.getDipcoinTransaction());
		bankTransaction.setDipcoin(dipcoin);
		try {
			Future<BankTransaction> asyncResult = bankDBService.createTransaction(bankTransaction);
			BankTransaction createdTransaction = asyncResult.get();

			BankTransaction resultedTransaction = lienRemovalDelegate.removeLien(createdTransaction);
			bankDBService.updateBankTransactionStatus(resultedTransaction);

			dipcoin.setStatusWithBank(resultedTransaction.getStatus()); // set the statuswithbank flag in dipcoin
			resultedTransaction.setDipcoin(dipcoin);
			// Update the Dipcoin Status with Dipcoin with the status received from the bank
			coinDBService.updateCoin(resultedTransaction.getDipcoin(),resultedTransaction.getStatus());

		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
