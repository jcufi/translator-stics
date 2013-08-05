package org.agmip.translators.stics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class TranslatorTestUtil {
	static String SLLL = "slll";
	static String SLDUL = "sldul";
	static String SLBDM = "slbdm";
	static String SKSAT = "sksat";
	static String SLLB = "sllb";
	static String ICH2O = "ich2o";
	static String ICNO3 = "icno3";
	static String ICNH4 = "icnh4";
	
	public static List<HashMap<String, String>> createSoilData() {
		LinkedHashMap<String, String> soil1 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil2 = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> soil3 = new LinkedHashMap<String, String>();
		List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		
		// First reference soil layer
		soil1.put(SLLL, "0.07");
		soil1.put(SLDUL, "0.12");
		soil1.put(SLBDM, "1.31");
		soil1.put(SKSAT, "1.0");
		soil1.put(ICNH4, "5.0");
		soil1.put(ICNO3, "8.0");
		soil1.put(SLLB, "5.0");
		soil1.put(ICH2O, "0.02");
		// Second layer
		soil2.put(SLLL, "0.07");
		soil2.put(SLDUL, "0.12");
		soil2.put(SLBDM, "1.31");
		soil2.put(ICNH4, "5.0");
		soil2.put(ICNO3, "8.0");
		soil2.put(SLLB, "10.0");
		soil2.put(ICH2O, "0.02");
		// Missing values should be taken from the first layer
		soil3.put(SLLL, "1.5");
		soil3.put(SLLB, "37.0");
		soil3.put(ICNH4, "8.0");
		soil3.put(ICNO3, "8.0");
		soil3.put(SLDUL, "0.12");
		soil3.put(SLBDM, "20");
		soil3.put(ICH2O, "0.02");

		list.add(soil1);
		list.add(soil2);
		list.add(soil3);
		return list;
	}
}
