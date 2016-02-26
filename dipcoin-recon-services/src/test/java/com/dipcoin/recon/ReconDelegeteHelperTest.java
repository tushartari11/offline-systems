package com.dipcoin.recon;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.recon.delegate.ReconciliationDelegate;
import com.dipcoin.recon.delegate.ReconciliationDelegateHelper;

@ContextConfiguration(locations = { "classpath:/dipcoin-recon-system-application-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("dev")
public class ReconDelegeteHelperTest {

	DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	NumberFormat formatter = new DecimalFormat("#0.##");

	@Autowired
	private ReconciliationDelegate reconciliationDelegate;

	@Autowired
	private ReconciliationDelegateHelper reconHelper;

	@Autowired
	private DipcoinDBService dipcoinDBService;

	@Autowired
	private BankDBService bankDBService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testRetrieveFile() {
		try {
			InputStream inputStreamObject = reconHelper.retrieveFile();
			Assert.assertNotNull(inputStreamObject);
			// TODO code to retrieve contents 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetTransactionsForMerchantID() {
		try {
			int merchantId = 0;
			DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			List<DipcoinTransaction> transactionsList = reconHelper
					.getDipcoinTransactionsForMerchant(merchantId);
			Assert.assertNotNull(transactionsList);
			for (DipcoinTransaction dipcoinTransaction : transactionsList) {
				System.out.println(sdf.format(dipcoinTransaction
						.getResponseTime()));
			}
			System.out.println("NUmber of dipcoin transations := "
					+ transactionsList.size());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getBankTransactionForIdTest() {
		try {
			BankTransaction bankTransaction = reconHelper.getBankTransactionForDCTransactionId(1);
			System.out.println("bankTransaction here : " + bankTransaction);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void performBankReconciliation() {
		try {
			reconciliationDelegate.performBankReconciliation(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void performMerchantReconciliation() {
		try {
			int merchantId = 0;
			reconciliationDelegate.performMerchantReconciliation(merchantId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
