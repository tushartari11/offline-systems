package com.dipcoin.recon.common;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;

import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.comm.DipcoinRequest.Operation;
import com.dipcoin.bank.services.comm.RemoveLienRequest;
import com.dipcoin.bank.services.comm.RemoveLienResponse;
import com.dipcoin.bank.services.utils.BankServiceException;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.DipcoinTransaction;

public class LienRemovalDelegateHelper implements LienRemovalDelegate {

	@Autowired
	BankAPIServices bankAPIservices;
	
	@Autowired
	DipcoinDBService coinDBService;
	
	@Autowired
	CustomerDBService customerDBService;
	
	DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	NumberFormat formatter = new DecimalFormat("#0.##");
	
	public static final int DEFAULT_INSTITUTE_ID = 1; // TODO need to check the actual value
	
	@Override
	public BankTransaction removeLien(BankTransaction bankTransaction) {
		// checkService - checks whether the bankAPIServices are up and running

		RemoveLienRequest removeLienRequest = generateLienRequestObject(bankTransaction);
		BankTransaction bankTransactionResult = null;
		try {
			Future<RemoveLienResponse> asyncResultLeinRemoval = bankAPIservices.removeLien(removeLienRequest);
			RemoveLienResponse removeLienResponse = asyncResultLeinRemoval.get();
			bankTransactionResult = mapResponseToBankTransaction(removeLienResponse);
		} catch (BankServiceException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return bankTransactionResult;
	}

	private RemoveLienRequest generateLienRequestObject(BankTransaction bankTransaction) {
		RemoveLienRequest removeLienRequest = new RemoveLienRequest();
		DipcoinTransaction dipcoinTransaction = null;
		try {
			if ( bankTransaction.getDipcoinTransaction() == null) {
				//Fetches dipcoinTransaction for the bankTransactionID
				Future<DipcoinTransaction> dipCoinTransactionTask = coinDBService.getDipcoinTransactionByBankTransactionId(bankTransaction.getBankTransactionId());
				dipcoinTransaction = dipCoinTransactionTask.get();
			}else{
				dipcoinTransaction = bankTransaction.getDipcoinTransaction();
			}
			// TODO check the source of each of the account number and account id field
			removeLienRequest.setCustomerId(String.valueOf(bankTransaction.getCustomerAccountId()));

			CustomerAccount customerAccount = getCustomerAccountByBankTransaction(bankTransaction);
			removeLienRequest.setAccountNum(customerAccount.getAccountNumber());
			removeLienRequest.setTransactionType("Cancel");
			removeLienRequest.setBankId(Integer.valueOf(customerAccount.getBankUId())); // where will we use this ? 
			removeLienRequest.setDipcoinReferenceNumber(String.valueOf(dipcoinTransaction.getDipcoinTransactionId()));
			removeLienRequest.setLienCancelAttempt("1"); // need to check	
			removeLienRequest.setOperation(Operation.REMOVE_LIEN); // need to check if redundant 
			removeLienRequest.setTransactionTime(String.valueOf(bankTransaction.getResponseTime()));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return removeLienRequest;
	}

	private CustomerAccount getCustomerAccountByBankTransaction(
			BankTransaction bankTransaction)
			throws InterruptedException, ExecutionException {
		CustomerAccount customerAccount = customerDBService.getAccount(bankTransaction.getCustomerAccountId(), DEFAULT_INSTITUTE_ID).get(); // TODO check what should be the institute id 
		return customerAccount;
	}

	private BankTransaction mapResponseToBankTransaction(
			RemoveLienResponse removeLienResponse) {
		BankTransaction bankTransaction = new BankTransaction();
		// TODO map required fields for creating a BankTransaction Object
		DipcoinTransaction dipcoinTransaction = new DipcoinTransaction();
		try {
			bankTransaction.setCustomerAccountId(Integer.valueOf(removeLienResponse.getCustomerId())); // map string to int
			dipcoinTransaction.setDipcoinTransactionId(Integer.parseInt(removeLienResponse.getDipcoinReferenceNumber()));
			bankTransaction.setDipcoinTransaction(dipcoinTransaction);
//			bankTransaction.setLienCancelAttempt(removeLienResponse.getLienCancelAttempt());// TODO Check if we need this 
			bankTransaction.setResponseTime(sdf.parse(removeLienResponse.getTransactionTime())); 
			bankTransaction.setType(Integer.valueOf(removeLienResponse.getTransactionType())); // map string to int
			bankTransaction.setStatus(removeLienResponse.getStatus());
//			bankTransaction.setCBSReferenceID(CBSReferenceID); // to get from the bank
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return bankTransaction;
	}
}
