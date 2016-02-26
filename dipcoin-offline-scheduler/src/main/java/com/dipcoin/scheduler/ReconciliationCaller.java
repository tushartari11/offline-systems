package com.dipcoin.scheduler;

import java.util.Date;

public class ReconciliationCaller {

//	@Autowired
//	ReconciliationDelegate reconciliationDelegate;
	
	public void callReconciliation(){
		System.out
		.println("callReconciliation -> Method executed at every 5 seconds. Current time is :: "
				+ new Date());
//		reconciliationDelegate.performMerchantReconciliation(0);
	}
}
