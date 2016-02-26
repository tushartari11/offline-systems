package com.dipcoin.scheduler;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.dipcoin.recon.delegate.SettlementDelegate;

public class SettlementCaller {

	@Autowired
	SettlementDelegate settlementDelegate;
	
	public void callSettlement()
	{
		System.out.println("callSettlement-> executed at every 10 seconds. Current time is :: "+ new Date());
//		settlementDelegate.performSettlement();
	}
}
