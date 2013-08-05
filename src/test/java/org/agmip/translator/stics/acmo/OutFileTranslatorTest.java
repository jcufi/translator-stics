package org.agmip.translator.stics.acmo;

import static org.junit.Assert.*;

import org.agmip.translators.stics.acmo.OutFileTranslator;
import org.junit.Test;

public class OutFileTranslatorTest {

	OutFileTranslator translator = new OutFileTranslator();

	@Test
	public void testReadInt() {
		assertEquals(100, translator.readInt("100.000", "test"));
		assertEquals(100, translator.readInt("100", "test"));
		assertEquals(0, translator.readInt("", "test"));

	}

	@Test
	public void testToDate() {
		// 10 avril 2013
		String res = translator.toDate(100, 2013);
		assertEquals("2013-04-10", res);
		// 31 dec 2020
		res = translator.toDate(366, 2020);
		assertEquals("2020-12-31", res);
		// 31 dec 2021
		res = translator.toDate(365, 2021);
		assertEquals("2021-12-31", res);

		// 465 julian day on 2 years
		// first year is leap (2012) 
		// second is regular (2013)
		res = translator.toDate(466, 2012);
		assertEquals("2013-04-10", res);

		// 465 julian day on 2 years
		// first year is regular (2013) 
		// second is regular (2014)
		res = translator.toDate(465, 2013);
		assertEquals("2014-04-10", res);

	}

}
