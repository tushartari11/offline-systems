package com.dipcoin.recon;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.dipcoin.recon.delegate.SettlementDelegate;
import com.dipcoin.recon.delegate.SettlementDelegateHelper;

@ContextConfiguration(locations = { "classpath:/dipcoin-recon-system-application-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("dev")
public class SettlementDelegateHelperTest {

	@Autowired
	private SettlementDelegate settlementDelegate;

	@Autowired
	private SettlementDelegateHelper settlementDelegateHelper;

	@Test
	public void testPerformSettlement() {
		assertTrue(settlementDelegate.performSettlement());
	}

}
