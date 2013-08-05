package org.agmip.translators.stics.util;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

import org.junit.Test;

public class UnitConverterTest {

	UnitConverter converter = new UnitConverter();
	
	@Test
	public void test() {
		//645.200
		assertEquals("645200.0",converter.convertToIcasaUnit("masec(n)", "645.200"));
		assertEquals("1000.0",converter.convertToIcasaUnit("masec(n)", "001.000"));
	}

}
