package org.agmip.translators.stics;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.stics.util.IcasaCode;
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.VelocityContext;

public class ManagementOutput implements TranslatorOutput {

	public static String BUCKET_MANAGEMENT = "management";
	public static String BUCKET_INTIAL_CONDITIONS = "initial_condition";
	public static String EVENT_FERTILIZER = "fertilizer";
	public static String EVENT_IRRIGATION = "irrigation";
	public static String EVENT_HARVEST = "harvest";
	public static String EVENT_PLANTING = "planting";
	public static String EVENT_TILLAGE = "tillage";
	public static String EVENT_KEY = "event";
	public static String DATE_FORMAT = "yyyyMMdd";
	public String TEC_TEMPLATE_FILE = "tec_template.xml";
	public HashMap<String, String[]> codeToMapByEvent = new HashMap<String, String[]>();

	private File mngmtFile;

	public ManagementOutput() {
		codeToMapByEvent.put(EVENT_FERTILIZER, new String[] { "fecd", "feacd" });
		codeToMapByEvent.put(EVENT_IRRIGATION, new String[] { "irop" });
		codeToMapByEvent.put(EVENT_HARVEST, new String[] { "harm" });
	}

	public File getManagementFile() {
		return mngmtFile;
	}

	public void writeFile(String file, Map data) {

		BucketEntry managementBucket;
		BucketEntry initialConditionBucket;
		ArrayList<LinkedHashMap<String, String>> fertilizerList;
		ArrayList<LinkedHashMap<String, String>> irigationList;
		LinkedHashMap<String, LinkedHashMap<String, String>> mgmtDataByEvent;
		LinkedHashMap<String, String> initialConditions;
		try {
			initialConditionBucket = MapUtil.getBucket(data, BUCKET_INTIAL_CONDITIONS);
			managementBucket = MapUtil.getBucket(data, BUCKET_MANAGEMENT);

			initialConditions = initialConditionBucket.getValues();

			mgmtDataByEvent = new LinkedHashMap<String, LinkedHashMap<String, String>>();
			fertilizerList = new ArrayList<LinkedHashMap<String, String>>();
			irigationList = new ArrayList<LinkedHashMap<String, String>>();

			// fill missing values with default values
			// management default values for : tidep, plrs, tidep, plrs, ireff,
			// irmdp, dtwt, harm
			SticsUtil.convertNestedRecords(managementBucket.getDataList());

			// process buckets
			for (LinkedHashMap<String, String> mgmtData : managementBucket.getDataList()) {
				String event = mgmtData.get(EVENT_KEY);
				// replace icasa code by stics code
				if (codeToMapByEvent.containsKey(event)) {
					convertCode(event, mgmtData);
				}
				if (event.equals(EVENT_IRRIGATION)) {
					irigationList.add(mgmtData);
				}
				if (event.equals(EVENT_FERTILIZER)) {
					fertilizerList.add(mgmtData);
				} else {
					mgmtDataByEvent.put(event, mgmtData);
				}
			}
			// We need to sort fertilizer data by date
			sortListByDate(fertilizerList);
			// Convert date in julian day
			convertListOfDate(fertilizerList);
			convertListOfDate(irigationList);
			convertDate(mgmtDataByEvent.get(EVENT_HARVEST));
			convertDate(mgmtDataByEvent.get(EVENT_PLANTING));
			convertDate(mgmtDataByEvent.get(EVENT_TILLAGE));
			String content = generateTecfile(mgmtDataByEvent, fertilizerList, initialConditions, irigationList);
			mngmtFile = SticsUtil.newFile(content, file, mgmtDataByEvent.get(EVENT_PLANTING).get("crid") + "_tec.xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void convertCode(String event, LinkedHashMap<String, String> data) {
		for (String code : codeToMapByEvent.get(event)) {
			if (data.containsKey(code)) {
				System.out.println("Code replaced, old val : " + data.get(code) + " new val : " + IcasaCode.toSticsCode(data.get(code)));
				data.put(code, IcasaCode.toSticsCode(data.get(code)));
			}
		}
	}

	public String generateTecfile(LinkedHashMap<String, LinkedHashMap<String, String>> mgmtDataByevent, ArrayList<LinkedHashMap<String, String>> fertilizerList,
			LinkedHashMap<String, String> initialConditions, ArrayList<LinkedHashMap<String, String>> irrigationList) {
		VelocityContext context = VelocityUtil.newVelocitycontext();
		context.put(EVENT_FERTILIZER, fertilizerList);
		context.put(EVENT_HARVEST, mgmtDataByevent.get(EVENT_HARVEST));
		context.put(EVENT_TILLAGE, mgmtDataByevent.get(EVENT_TILLAGE));
		context.put(EVENT_PLANTING, mgmtDataByevent.get(EVENT_PLANTING));
		context.put(EVENT_IRRIGATION, irrigationList);
		context.put(BUCKET_INTIAL_CONDITIONS, initialConditions);
		return VelocityUtil.runVelocity(context, TEC_TEMPLATE_FILE);
	}

	public void convertDate(LinkedHashMap<String, String> data) {
		try {
			// if the structure contains date field, we convert it into julian
			// day

			if (data.containsKey("date")) {
				Date date = formatter.parse(data.get("date"));
				int julianDayDate = SticsUtil.getJulianDay(date);
				data.put("date", String.valueOf(julianDayDate));
			}
		} catch (ParseException e) {
			// TODO throw runtime exception ?
			System.err.println("Invalid date format");
			e.printStackTrace();
		}
	}

	SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

	public void convertListOfDate(ArrayList<LinkedHashMap<String, String>> dataList) {
		for (LinkedHashMap<String, String> data : dataList) {
			convertDate(data);
		}
	}

	public void sortListByDate(ArrayList<LinkedHashMap<String, String>> fertilizerList) {
		Collections.sort(fertilizerList, new Comparator<LinkedHashMap<String, String>>() {
			SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

			/**
			 * 
			 * @return a negative integer, zero, or a positive integer as the
			 *         first argument is less than, equal to, or greater than
			 *         the second.
			 */
			public int compare(LinkedHashMap<String, String> bucket1, LinkedHashMap<String, String> bucket2) {

				Date fertilizationDate1;
				Date fertilizationDate2;
				int result;
				//
				try {
					fertilizationDate1 = formatter.parse(bucket1.get("date"));
					fertilizationDate2 = formatter.parse(bucket2.get("date"));

					// check if both are the same
					if (fertilizationDate1.equals(fertilizationDate2)) {
						result = 0;
					} else if (fertilizationDate1.after(fertilizationDate2)) {
						result = 1;
					} else {
						result = -1;
					}
					return result;
				} catch (ParseException e) {
					// TODO throw runtime exception ?
					System.err.println("Unable to parse date");
					e.printStackTrace();
					return 0;
				}

			}
		});

	}

}
