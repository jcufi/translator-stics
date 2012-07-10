package org.agmip.translators.stics;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModelOutputTest {

	ModelOutput output;
	
	@Before
	public void setUp() throws Exception {
		output = new ModelOutput();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFormatLine(){
		String[] params = new String[] { "wst_name", "w_date", "tmin", "tmax", "srad", "eoaa", "rain", "wind", "vprs", "co2d" };
		FirstLevelValues values = new FirstLevelValues();
		values.stationName = "test_wst_name";
		HashMap map = new HashMap();
		for(String param : params){
			map.put(param, "test_"+param);
		}
		
		//"wst_name", "w_date", "tmin", "tmax", "srad", "eoaa", "rain", "wind", "vprs", "co2d"
		//formatLine(FirstLevelValues firstLevel, Map<String, Object> weatherRecord) ;
	}
	
	@Test
	public void testGetJulianDay() throws ParseException {
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");
		assertEquals(1, output.getJulianDay( formatter.parse("19920101")));
		assertEquals(190, output.getJulianDay( formatter.parse("20120708")));
		assertEquals(192, output.getJulianDay( formatter.parse("20000710")));
	}

}
