package com.dipcoin.recon.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

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

	private static final int INITIAL_PAGE_INDEX = 0;
	private static final int NO_OF_RECORDS_PER_PAGE = 100;
	
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
			int startIndex = INITIAL_PAGE_INDEX, pageSize = NO_OF_RECORDS_PER_PAGE;
			while (true) {
				List<Future<Dipcoin>> resultList = new ArrayList<>();
				Future<List<Dipcoin>> asyncResults = coinDBService.getExpiredDipcoins(pageSize, startIndex);
				List<Dipcoin> expiredDipcoins = asyncResults.get();
				if (expiredDipcoins == null || expiredDipcoins.isEmpty())
					break;
				ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
						.newFixedThreadPool(expiredDipcoins.size()); // need to check on the number of threads
				for (Dipcoin dipcoin : expiredDipcoins) { 
					LienRemovalHelper lienRemovalHelper = new LienRemovalHelper(dipcoin);
					Future<Dipcoin> result = executor.submit(lienRemovalHelper);
					resultList.add(result);
				}
				startIndex += pageSize;
				//shut down the executor service when the FutureTasks are completed
	            executor.shutdown();
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
	 * @param dipcoin
	 *            theDipcoin which is expired
	 */
	
	class LienRemovalHelper implements Callable<Dipcoin>{

		private Dipcoin dipcoin;
		 
	    public LienRemovalHelper(Dipcoin dipcoin) {
	        this.dipcoin = dipcoin;
	    }
	    
		@Override
		public Dipcoin call() throws Exception {
			Dipcoin updateDipcoin = createAndFireTransactionsForLienRemoval(dipcoin);
			return updateDipcoin;
		}
		
		private Dipcoin createAndFireTransactionsForLienRemoval(Dipcoin dipcoin) {
			BankTransaction bankTransaction = new BankTransaction();
			Dipcoin updatedDipcoin  = null;
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
				// Update the Dipcoin Status with Dipcoin with the status received
				// from the bank
				 updatedDipcoin = coinDBService.updateCoin(resultedTransaction.getDipcoin(),
						resultedTransaction.getStatus()).get();

			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return updatedDipcoin;
		}
		
	}
}
