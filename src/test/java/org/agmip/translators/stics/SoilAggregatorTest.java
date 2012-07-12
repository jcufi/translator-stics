package org.agmip.translators.stics;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

public class SoilAggregatorTest {
	static String SLLL = "slll";
	static String SLDUL = "sldul";
	static String SLBDM = "slbdm";
	static String SKSAT = "sksat";

	@Test
	public void test() {
		SoilAggregator aggregator;
		String[] keys = new String[] { SKSAT, SLLL, SLDUL, SLBDM };
		aggregator = new SoilAggregator();
		LinkedHashMap<String, String> soil1 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil2 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil3 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> expectedSoilResult1 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> expectedSoilResult2 = new LinkedHashMap<String, String>();
		// First reference soil layer
		soil1.put(SLLL, "1.0");
		soil1.put(SLDUL, "1.0");
		soil1.put(SLBDM, "1.0");
		soil1.put(SKSAT, "5.0");
		// Second layer
		soil2.put(SLLL, "1.01");
		soil2.put(SLDUL, "1.01");
		soil2.put(SLBDM, "1.01");
		soil2.put(SKSAT, "15.0");
		// Missing values should be taken from the first layer
		soil3.put(SLLL, "1.5");
		soil3.put(SKSAT, "30.0");

		// The merge result should be 2 soils layer
		// First soil is a aggregated soil and the second is like the third but
		// with reference values of the first

		// First reference soil layer
		expectedSoilResult1.put(SLLL, "1.005");
		expectedSoilResult1.put(SLDUL, "1.005");
		expectedSoilResult1.put(SLBDM, "1.005");
		expectedSoilResult1.put(SKSAT, "20.0");
		// Second layer
		expectedSoilResult2.put(SLLL, "1.5");
		expectedSoilResult2.put(SLDUL, "1.0");
		expectedSoilResult2.put(SLBDM, "1.0");
		expectedSoilResult2.put(SKSAT, "30.0");

		ArrayList<LinkedHashMap<String, String>> list = new ArrayList<LinkedHashMap<String, String>>();
		list.add(soil1);
		list.add(soil2);
		list.add(soil3);

		List<HashMap<String, String>> result = aggregator.merge(list);
		System.out.println(result.get(0));
		System.out.println(result.get(1));
		for (String key : keys) {
			assertEquals(expectedSoilResult1.get(key), result.get(0).get(key));
			assertEquals(expectedSoilResult2.get(key), result.get(1).get(key));
		}

	}

}
