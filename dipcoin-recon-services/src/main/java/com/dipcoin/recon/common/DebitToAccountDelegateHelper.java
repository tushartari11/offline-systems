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
import com.dipcoin.bank.services.comm.DebitToAccRequest;
import com.dipcoin.bank.services.comm.DebitToAccResponse;
import com.dipcoin.bank.services.utils.BankServiceException;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;

public class DebitToAccountDelegateHelper implements DebitToAccountDelegate {

	@Autowired
	BankAPIServices bankAPIservices;

	@Autowired
	BankDBService bankDBService;
	
	@Autowired
	DipcoinDBService coinDBService;
	
	@Autowired
	CustomerDBService customerDBService;
	
	public static final int DEFAULT_INSTITUTE_ID = 1; // TODO need to check the actual value
	
	DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	NumberFormat formatter = new DecimalFormat("#0.##");
	
	@Override
	public BankTransaction debitToAccount(Integer bankTransactionId){
		BankTransaction bankTransaction = null;
		try {
			bankTransaction = bankDBService.getBankTransactionById(bankTransactionId).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bankTransaction = debitToAccount(bankTransaction);
		return bankTransaction;
	}
	
	@Override
	public BankTransaction debitToAccount(BankTransaction bankTransaction) {
		DebitToAccRequest debitToAccountRequest = createDebitToAccRequest(bankTransaction);
		BankTransaction bankTransactionResult = null,resultedTransaction = null;
		try {
			Future<DebitToAccResponse> asyncResultDebitToAcc = bankAPIservices.debitToAcc(debitToAccountRequest);
			DebitToAccResponse debitToAccResponse = asyncResultDebitToAcc.get();
			bankTransactionResult = mapResponseToBankTransaction(debitToAccResponse);
			
			resultedTransaction = bankDBService.updateBankTransactionStatus(bankTransactionResult).get();

			Dipcoin dipcoin = bankTransaction.getDipcoin();
			dipcoin.setStatusWithBank(resultedTransaction.getStatus()); // set the statuswithbank flag in dipcoin
			resultedTransaction.setDipcoin(dipcoin);
			// Update the Dipcoin Status with Dipcoin with the status received from the bank
			coinDBService.updateCoin(dipcoin ,resultedTransaction.getStatus());
		} catch (BankServiceException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return bankTransactionResult;
	}

	private DebitToAccRequest createDebitToAccRequest(BankTransaction bankTransaction) {
		DebitToAccRequest debitToAccountRequest=new DebitToAccRequest();
		CustomerAccount customerAccount = getCustomerAccountByBankTransaction(bankTransaction);
		debitToAccountRequest.setAccountNum(customerAccount.getAccountNumber()); 
		debitToAccountRequest.setAmountToDebit(String.valueOf(bankTransaction.getAmount())); 
		debitToAccountRequest.setBankId(Integer.valueOf(customerAccount.getBankUId()));
		debitToAccountRequest.setCurrencyType("INR"); // Where to get this from 
		debitToAccountRequest.setCustomerId(String.valueOf(bankTransaction.getCustomerAccountId()));// is it not same as 
		debitToAccountRequest.setAmountLienMarked(String.valueOf(bankTransaction.getAmount()));
//		debitToAccountRequest.setDipcoinReferenceNumber(String.valueOf(bankTransaction.getDipcoinTransaction().getDipcoinT()));
		return debitToAccountRequest;
	}
	
	private BankTransaction mapResponseToBankTransaction(DebitToAccResponse debitToAccResponse){
		BankTransaction bankTransaction = new BankTransaction();
		// TODO map required fields for creating a BankTransaction Object
		DipcoinTransaction dipcoinTransaction = new DipcoinTransaction();
		try {
			bankTransaction.setCustomerAccountId(Integer.valueOf(debitToAccResponse.getCustomerId())); // map string to int
//			dipcoinTransaction.setDipcoinTransactionId(debitToAccResponse.getDipcoinReferenceNumber());
			bankTransaction.setDipcoinTransaction(dipcoinTransaction);
//			bankTransaction.setLienCancelAttempt(removeLienResponse.getLienCancelAttempt());// TODO Check if we need this 
			bankTransaction.setResponseTime(sdf.parse(debitToAccResponse.getTransactionTime())); 
			bankTransaction.setType(Integer.valueOf(debitToAccResponse.getTransactionType())); // map string to int
			bankTransaction.setStatus(debitToAccResponse.getStatus());
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
	
	private CustomerAccount getCustomerAccountByBankTransaction(BankTransaction bankTransaction){
		CustomerAccount customerAccount = null;
		try {
			customerAccount = customerDBService.getAccount(bankTransaction.getCustomerAccountId(),DEFAULT_INSTITUTE_ID).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // TODO check what should be the institute id 
		return customerAccount;
	}
}
