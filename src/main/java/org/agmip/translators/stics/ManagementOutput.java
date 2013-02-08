package org.agmip.translators.stics;

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
import org.agmip.translators.stics.util.ExperimentInfo;
import org.agmip.translators.stics.util.IcasaCode;
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagementOutput implements TranslatorOutput {
	private static final Logger log = LoggerFactory.getLogger(ManagementOutput.class);
	public static String BUCKET_MANAGEMENT = "management";
	public static String BUCKET_EXPERIMENTS = "experiments";
	public static String BUCKET_SOILS = "soils";
	public static String BUCKET_INTIAL_CONDITIONS = "initial_conditions";
	public static String EVENT_FERTILIZER = "fertilizer";
	public static String EVENT_IRRIGATION = "irrigation";
	public static String EVENT_HARVEST = "harvest";
	public static String EVENT_PLANTING = "planting";
	public static String EVENT_TILLAGE = "tillage";
	public static String EVENT_KEY = "event";
	public String TEC_TEMPLATE_FILE = "tec_template.vm";
	public HashMap<String, String[]> codeToMapByEvent = new HashMap<String, String[]>();
	public static String DATE_FORMAT = "yyyyMMdd";
	private SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
	private HashMap<String, ExperimentInfo> expInfoByExpId;
	private HashMap<String, BucketEntry> soilById;

	public HashMap<String, ExperimentInfo> getExpInfo() {
		return expInfoByExpId;
	}

	public ManagementOutput() {
		codeToMapByEvent.put(EVENT_FERTILIZER, new String[] { "fecd", "feacd" });
		codeToMapByEvent.put(EVENT_IRRIGATION, new String[] { "irop" });
		codeToMapByEvent.put(EVENT_HARVEST, new String[] { "harm" });
		expInfoByExpId = new HashMap<String, ExperimentInfo>();
		soilById = new HashMap<String, BucketEntry>();
	}

	public void indexSoilById(Map data) {
		ArrayList<BucketEntry> soilList = MapUtil.getPackageContents(data, BUCKET_SOILS);
		for (BucketEntry soil : soilList) {
			soilById.put(soil.getValues().get("soil_id"), soil);
		}

	}

	public void writeFile(String file, Map data) {
		BucketEntry managementBucket;
		BucketEntry initialConditionBucket;
		ArrayList<HashMap<String, String>> fertilizerList;
		ArrayList<HashMap<String, String>> irigationList;
		ArrayList<HashMap<String, String>> tillageList;
		HashMap<String, HashMap<String, String>> mgmtDataByEvent;
		HashMap<String, String> initialConditions;
		SoilAndInitOutput soilOut;
		indexSoilById(data);
		try {
			@SuppressWarnings("unchecked")
			ArrayList<LinkedHashMap<String, String>> listOfExperiments = (ArrayList<LinkedHashMap<String, String>>) data.get(BUCKET_EXPERIMENTS);
			indexSoilById(data);
			soilOut = new SoilAndInitOutput(file);
			for (Map experiment : listOfExperiments) {
				ExperimentInfo expInfo = new ExperimentInfo((String) experiment.get("exname"), (String) experiment.get("soil_id"), SticsUtil.toWeatherId((String) experiment.get("wst_id")));
				log.info("Processing experiment : " + expInfo.getExpId());
				// generating soil and initialization files
				soilOut.writeFile(experiment, expInfo.getExpId(), soilById.get(expInfo.getSoilId()));
				expInfo.setIniFile(soilOut.getInitializationFile().getName());

				// generating management files
				initialConditionBucket = MapUtil.getBucket(experiment, BUCKET_INTIAL_CONDITIONS);
				managementBucket = MapUtil.getBucket(experiment, BUCKET_MANAGEMENT);
				initialConditions = initialConditionBucket.getValues();
				SticsUtil.defaultValueForMap(new String[] { "icrli" }, initialConditions);
				SticsUtil.convertValues(initialConditions);
				mgmtDataByEvent = new HashMap<String, HashMap<String, String>>();
				fertilizerList = new ArrayList<HashMap<String, String>>();
				irigationList = new ArrayList<HashMap<String, String>>();
				tillageList = new ArrayList<HashMap<String, String>>();
				// fill missing values with default values
				// management default values for : tidep, plrs, tidep, plrs,
				// ireff, irmdp, dtwt, harm
				SticsUtil.convertNestedRecords(managementBucket.getDataList(), new String[] { "plrs", "tidep", "plrs", "ireff", "irmdp", "dtwt", "harm" });

				computeExperimentDuration(initialConditions, experiment, expInfo);

				for (HashMap<String, String> mgmtData : managementBucket.getDataList()) {
					String event = mgmtData.get(EVENT_KEY);
					// replace icasa code by stics code
					if (codeToMapByEvent.containsKey(event)) {
						convertCode(event, mgmtData);
					}
					if (event.equals(EVENT_IRRIGATION)) {
						irigationList.add(mgmtData);
					}
					if (event.equals(EVENT_TILLAGE)) {
						tillageList.add(mgmtData);
					}
					if (event.equals(EVENT_FERTILIZER)) {
						fertilizerList.add(mgmtData);
					} else {
						mgmtDataByEvent.put(event, mgmtData);
					}
				}
				// TODO verifier le mapping
				String crid = mgmtDataByEvent.get(EVENT_PLANTING).get("crid") == null ? SticsUtil.defaultValue("crid") : mgmtDataByEvent.get(EVENT_PLANTING).get("crid");
				String icpcr = IcasaCode.toSticsCode(initialConditions.get("icpcr"));
				initialConditions.put("icpcr", icpcr == null ? IcasaCode.toSticsCode(crid) : icpcr);
				
				
				
				// We need to sort fertilizer data by date
				sortListByDate(fertilizerList);
				// Convert date in julian day
				convertListOfDate(fertilizerList);
				convertListOfDate(irigationList);
				convertListOfDate(tillageList);
				convertDate(mgmtDataByEvent.get(EVENT_HARVEST));
				convertDate(mgmtDataByEvent.get(EVENT_PLANTING));
				
				String content = generateTecfile(mgmtDataByEvent, fertilizerList, initialConditions, irigationList, tillageList);

				expInfo.setCropId(crid);
				expInfo.setMngmtFile(SticsUtil.newFile(content, file, expInfo.getExpId() + "_" + crid + "_tec.xml").getName());
				expInfoByExpId.put((String) experiment.get("exname"), expInfo);
			}
			soilOut.generateSoilsFile();
		} catch (IOException e) {
			log.error("Unable to generate tec file.");
			log.error(e.toString());
		}

	}

	/**
	 * if the list contains at least one element take it to put values into the context
	 * else take the default value
	 * @param values
	 * @param params
	 */
	private void defaultIfEmpty(ArrayList<HashMap<String, String>> values, String[] params, VelocityContext context){
		
		if(values.size() == 0){
			for(String p : params){
				context.put(p, SticsUtil.defaultValue(p));	
			}
		}else {
			for(String p : params){
				context.put(p, values.get(0).get(p));	
			}
		}
	}
	
	
	private void computeExperimentDuration(HashMap<String, String> initialConditions, Map experiment, ExperimentInfo expInfo) {
		try {
			Date date = formatter.parse((String) initialConditions.get("icdat"));
			int duration = Integer.parseInt((String) experiment.get("exp_dur"));
			expInfo.setDuration(duration);
			String julianDaydate = String.valueOf(SticsUtil.getJulianDay(date));
			initialConditions.put("icdat", julianDaydate);
			expInfo.setIcdat(julianDaydate);
			expInfo.setStartingDate(date);
		} catch (ParseException e) {
			log.error("Unable to parse icdat field");
			log.error(e.toString());
		}
	}

	public void convertCode(String event, HashMap<String, String> data) {
		for (String code : codeToMapByEvent.get(event)) {
			if (data.containsKey(code)) {
				log.debug("Code replaced, old val : " + data.get(code) + " new val : " + IcasaCode.toSticsCode(data.get(code)));
				data.put(code, IcasaCode.toSticsCode(data.get(code)));
			}
		}
	}
	String[] defaultParamIrrigation = new String[] { "ireff", "irop", "irmdp", "dtwt", "irn"};
	String[] defaultParamFertilization = new String[] { "fedep", "feacd", "irn", "fecd"};
	
	public String generateTecfile(HashMap<String, HashMap<String, String>> mgmtDataByevent, ArrayList<HashMap<String, String>> fertilizerList, HashMap<String, String> initialConditions,
			ArrayList<HashMap<String, String>> irrigationList,ArrayList<HashMap<String, String>> tillageList) {
		VelocityContext context = VelocityUtil.newVelocitycontext();
		defaultIfEmpty(fertilizerList, defaultParamFertilization, context);
		defaultIfEmpty(irrigationList, defaultParamIrrigation, context);
		defaultIfEmpty(irrigationList, defaultParamIrrigation, context);
		context.put(EVENT_FERTILIZER, fertilizerList);
		context.put(EVENT_HARVEST, mgmtDataByevent.get(EVENT_HARVEST));
		context.put(EVENT_TILLAGE, tillageList);
		context.put(EVENT_PLANTING, mgmtDataByevent.get(EVENT_PLANTING));
		context.put(EVENT_IRRIGATION, irrigationList);
		context.put(BUCKET_INTIAL_CONDITIONS, initialConditions);
		return VelocityUtil.getInstance().runVelocity(context, TEC_TEMPLATE_FILE);
	}

	public void convertDate(HashMap<String, String> data) {
		try {
			// if the structure contains date field, we convert it into julian
			// day
			if (data != null && data.containsKey("date")) {
				Date date = formatter.parse(data.get("date"));
				int julianDayDate = SticsUtil.getJulianDay(date);
				data.put("date", String.valueOf(julianDayDate));
			}
		} catch (ParseException e) {
			log.error("Invalid date format");
			log.error(e.toString());
		}
	}

	public void convertListOfDate(ArrayList<HashMap<String, String>> dataList) {
		for (HashMap<String, String> data : dataList) {
			convertDate(data);
		}
	}

	public void sortListByDate(ArrayList<HashMap<String, String>> fertilizerList) {
		Collections.sort(fertilizerList, new Comparator<HashMap<String, String>>() {
			SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

			/**
			 * 
			 * @return a negative integer, zero, or a positive integer as the
			 *         first argument is less than, equal to, or greater than
			 *         the second.
			 */
			public int compare(HashMap<String, String> bucket1, HashMap<String, String> bucket2) {

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
					log.error("Unable to parse date");
					log.error(e.toString());
					return 0;
				}

			}
		});

	}

}
