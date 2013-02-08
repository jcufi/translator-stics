package org.agmip.translators.stics;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.util.JSONAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModelOutputTest {

	WeatherOutput output;

	@Before
	public void setUp() throws Exception {
		output = new WeatherOutput();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testwriteFile() throws IOException {
		Map jsonMap = JSONAdapter.fromJSON(SticsUtil.getDataFromTestFile("/new_version.json"));
		output.writeFile("station name", jsonMap);
	}

	@Test
	public void testFormatLine() {
		String[] params = new String[] { "wst_name", "tmin", "tmax", "srad", "eoaa", "rain", "wind", "vprs", "co2d" };

		String stationName = "test_wst_name";
		HashMap mapParams = new HashMap();
		for (String param : params) {
			mapParams.put(param, "test_" + param);
		}
		mapParams.put("wind", "123");
		mapParams.put("vprs", "123");
		mapParams.put("w_date", "20120708");

		// "wst_name", "w_date", "tmin", "tmax", "srad", "eoaa", "rain", "wind",
		// "vprs", "co2d"
		String resultLine = output.formatLine(stationName, mapParams);
		System.out.println("result line : " + resultLine);
		System.out.println("expected    : " +"test_wst_name 2012 07 08 190 test_tmin test_tmax test_srad test_eoaa test_rain 1.423611 1230.0 test_co2d");
		assertEquals("test_wst_name 2012 07 08 190 test_tmin test_tmax test_srad test_eoaa test_rain 1.423611 1230.0 test_co2d", resultLine);
	}
	@Test
	public void testGetJulianDay() throws ParseException {
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");
		assertEquals(1, SticsUtil.getJulianDay(formatter.parse("19920101")));
		assertEquals(190, SticsUtil.getJulianDay(formatter.parse("20120708")));
		assertEquals(192, SticsUtil.getJulianDay(formatter.parse("20000710")));
	}

}
