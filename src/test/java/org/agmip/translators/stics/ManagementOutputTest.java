package org.agmip.translators.stics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.junit.Test;

public class ManagementOutputTest extends TestCase{

	ManagementOutput converter;
	String jsonTestFile1 = "/test-management-date.json";
	String jsonTestFile2 = "/test-management.json";
	
	public ManagementOutputTest() {
		converter = new ManagementOutput();
	}
	
	public ArrayList<HashMap<String, String>> getFertilizerList(String jsonFile){
		
		// Get JSON data
		Map data = SticsUtil.getDataFrom(jsonFile);
		BucketEntry managementBucket = MapUtil.getBucket(data, "management");
		ArrayList<HashMap<String, String>> fertilizerList;
		fertilizerList = new ArrayList<HashMap<String, String>>();
		for (HashMap<String, String> mgmtData : managementBucket.getDataList()) {
			String key = mgmtData.get("event");
			if (key.equals("fertilizer")) {
				// We need to sort fertilizer data by date
				fertilizerList.add(mgmtData);
			}
		}
		return fertilizerList;
	}

	@Test
	public void testWrite(){
		Map data = SticsUtil.getDataFrom(jsonTestFile2);
		converter.writeFile("", data);
	}
	
	
	
	public void testGenerate(){
		ArrayList<HashMap<String, String>> fertilizerList;
		fertilizerList = getFertilizerList(jsonTestFile1);
		//String data = converter.generateTecfile(null, fertilizerList, null);
		//System.out.println(data);
	}
	
	
	@Test
	public void testSortByDate() {
		String outputDir = System.getProperty("user.dir") + File.separator + "workspace";
		ArrayList<HashMap<String, String>> fertilizerList = getFertilizerList(jsonTestFile1);
		String[] date = {"19820101","19820105","19820105","19820110","19820201","20000101"};
		converter.sortListByDate(fertilizerList);
		for(int i=0; i<date.length; i++){
			String currentDate = fertilizerList.get(i).get("date");
			System.out.println(currentDate);
			assertEquals(currentDate, date[i]);
		}
		
	}

}
