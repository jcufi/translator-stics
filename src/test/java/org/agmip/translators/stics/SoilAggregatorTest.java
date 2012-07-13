package org.agmip.translators.stics;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.Test;

public class SoilAggregatorTest {
	static String SLLL = "slll";
	static String SLDUL = "sldul";
	static String SLBDM = "slbdm";
	static String SKSAT = "sksat";
	static String SLLB = "sllb";

	@Test
	public void testFormat() {
		LinkedHashMap<String, String> soil1 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil2 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil3 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil4 = new LinkedHashMap<String, String>();
		ArrayList<LinkedHashMap<String, String>> soilsData = new ArrayList<LinkedHashMap<String, String>>();
		// First reference soil layer
		soil1.put(SLLL, "0.07");
		soil1.put(SLDUL, "0.12");
		soil1.put(SLBDM, "1.31");
		soil1.put(SKSAT, "1.0");
		soil1.put(SLLB, "5.0");
		// Second layer
		soil2.put(SLLL, "0.07");
		soil2.put(SLDUL, "0.12");
		soil2.put(SLBDM, "1.31");
		soil2.put(SLLB, "10.0");
		
		soil3.put(SLLL, "0.07");
		soil3.put(SLDUL, "0.12");
		soil3.put(SLBDM, "1.31");
		soil3.put(SKSAT, "1.0");
		soil3.put(SLLB, "30.0");
		// Second layer
		soil4.put(SLLL, "0.07");
		soil4.put(SLDUL, "0.12");
		soil4.put(SLBDM, "1.31");
		soil4.put(SLLB, "60.0");
		
		SoilAggregator aggregator = new SoilAggregator();
		soilsData.add(soil1);
		soilsData.add(soil2);
		soilsData.add(soil3);
		soilsData.add(soil4);
		
		ArrayList<LinkedHashMap<String, String>> resultData = aggregator.formatSoilLayers(soilsData);
		// First reference soil layer
		assertEquals("5.0", resultData.get(0).get(SLLB));
		assertEquals("5.0", resultData.get(1).get(SLLB));
		assertEquals("20.0", resultData.get(2).get(SLLB));
		assertEquals("30.0", resultData.get(3).get(SLLB));
		ArrayList<LinkedHashMap<String, String>> soilsData2 = new ArrayList<LinkedHashMap<String, String>>();
		soilsData2.add(soil1);
		soilsData2.add(soil2);
		ArrayList<LinkedHashMap<String, String>> result = aggregator.mergeSoilLayers(soilsData2);
		assertEquals("10.0", result.get(0).get(SLLB));
		
	}

	@Test
	public void test() {
		SoilAggregator aggregator;
		String[] keys = new String[] { SLLB, SKSAT, SLLL, SLDUL, SLBDM };
		aggregator = new SoilAggregator();
		LinkedHashMap<String, String> soil1 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil2 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil3 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> expectedSoilResult1 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> expectedSoilResult2 = new LinkedHashMap<String, String>();

		// First reference soil layer
		soil1.put(SLLL, "0.07");
		soil1.put(SLDUL, "0.12");
		soil1.put(SLBDM, "1.31");
		soil1.put(SKSAT, "1.0");
		soil1.put(SLLB, "5.0");
		// Second layer
		soil2.put(SLLL, "0.07");
		soil2.put(SLDUL, "0.12");
		soil2.put(SLBDM, "1.31");
		soil2.put(SLLB, "10.0");
		// Missing values should be taken from the first layer
		soil3.put(SLLL, "15");
		soil3.put(SLLB, "37.0");
		soil3.put(SLDUL, "10");
		soil3.put(SLBDM, "20");

		// The merge result should be 2 soils layer
		// First soil is a aggregated soil and the second is like the third but
		// with reference values of the first

		// First reference soil layer
		expectedSoilResult1.put(SLLL, "5.3435116");
		expectedSoilResult1.put(SLDUL, "9.160306");
		expectedSoilResult1.put(SLBDM, "1.31");
		expectedSoilResult1.put(SKSAT, "1.0");
		expectedSoilResult1.put(SLLB, "10.0");
		// Second layer
		expectedSoilResult2.put(SLLL, "1.5");
		expectedSoilResult2.put(SLDUL, "0.12");
		expectedSoilResult2.put(SLBDM, "1.31");
		expectedSoilResult2.put(SKSAT, "1.0");
		expectedSoilResult2.put(SLLB, "27.0");

		ArrayList<LinkedHashMap<String, String>> list = new ArrayList<LinkedHashMap<String, String>>();
		list.add(soil1);
		list.add(soil2);
		//list.add(soil3);

		ArrayList<LinkedHashMap<String, String>> result = aggregator.mergeSoilLayers(list);
		System.out.println("First    result       : " + result.get(0));
		System.out.println("First Expected result : " + expectedSoilResult1);
		//System.out.println("Second    result       : " + result.get(1));
		//System.out.println("Second Expected result : " + expectedSoilResult2);
		// System.out.println(result.get(1));
		for (String key : keys) {
			System.out.println(key + " ");
			assertEquals(expectedSoilResult1.get(key), result.get(0).get(key));
			// System.out.println("Sec soil");
			// assertEquals(expectedSoilResult2.get(key),
			// result.get(1).get(key));
		}

	}

}
