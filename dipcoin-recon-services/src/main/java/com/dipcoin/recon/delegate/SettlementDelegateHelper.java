package com.dipcoin.recon.delegate;

import org.springframework.stereotype.Component;

@Component("settlementDelegate")
public class SettlementDelegateHelper implements SettlementDelegate{

	@Override
	public boolean performSettlement() {
		System.out.println("Settlement Called ...");
		return true;
	}

}
