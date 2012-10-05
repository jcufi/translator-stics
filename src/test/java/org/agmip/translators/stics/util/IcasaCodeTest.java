package org.agmip.translators.stics.util;

import junit.framework.TestCase;

import org.junit.Test;

public class IcasaCodeTest extends TestCase {

	public IcasaCodeTest() {
		// TODO Auto-generated constructor stub
	}
	
	@Test
	public void testToSticsCode(){
		assertEquals( "1", IcasaCode.toSticsCode("HM002"));
	}
	
}
