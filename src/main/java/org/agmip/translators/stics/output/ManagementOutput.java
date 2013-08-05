package org.agmip.translators.stics.output;

import static org.agmip.translators.stics.util.IcasaCode.toSticsCode;
import static org.agmip.translators.stics.util.SticsUtil.defaultValue;
import static org.agmip.translators.stics.util.SticsUtil.defaultValueForMap;
import static org.agmip.translators.stics.util.SticsUtil.getJulianDay;
import static org.agmip.translators.stics.util.SticsUtil.newFile;
import static org.agmip.translators.stics.util.SticsUtil.toWeatherId;
import static org.agmip.util.MapUtil.getBucket;
import static org.agmip.util.MapUtil.getPackageContents;
import static org.agmip.util.MapUtil.getValueOr;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.stics.model.ExperimentInfo;
import org.agmip.translators.stics.util.Const;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagementOutput extends SticsFileGenerator implements TranslatorOutput {
	private static final Logger log = LoggerFactory.getLogger(ManagementOutput.class);
	public static final String BUCKET_MANAGEMENT = "management";
	public static final String BUCKET_EXPERIMENTS = "experiments";
	public static final String BUCKET_SOILS = "soils";
	public static final String BUCKET_INTIAL_CONDITIONS = "initial_conditions";
	public static final String EVENT_FERTILIZER = "fertilizer";
	public static final String EVENT_IRRIGATION = "irrigation";
	public static final String EVENT_HARVEST = "harvest";
	public static final String EVENT_PLANTING = "planting";
	public static final String EVENT_TILLAGE = "tillage";
	public static final String EVENT_KEY = "event";
	private static final String DATE = "date";
	public static final String TEC_TEMPLATE_FILE = "tec_template.vm";
	private static final String[] MANAGEMENT_PARAMETERS_TO_CONVERT = new String[] { "plrs", "tidep", "plrs", "ireff", "irmdp", "dtwt", "harm" };
	private static final String[] DEFAULT_IRRIGATION_PARAMS = new String[] { "ireff", "irop", "irmdp", "dtwt", "irn" };
	private static final String[] DEFAULT_FERT_PARAMS = new String[] { "fedep", "feacd", "irn", "fecd" };

	private Map<String, String[]> codeToMapByEvent = new HashMap<String, String[]>();
	private SimpleDateFormat formatter = new SimpleDateFormat(Const.DATE_FORMAT);
	private Map<String, ExperimentInfo> expInfoByExpId;
	private Map<String, BucketEntry> soilById;

	public ManagementOutput() {
		codeToMapByEvent.put(EVENT_FERTILIZER, new String[] { "fecd", "feacd" });
		codeToMapByEvent.put(EVENT_IRRIGATION, new String[] { "irop" });
		codeToMapByEvent.put(EVENT_HARVEST, new String[] { "harm" });
		expInfoByExpId = new HashMap<String, ExperimentInfo>();
		soilById = new HashMap<String, BucketEntry>();
	}

	public Map<String, ExperimentInfo> getExpInfo() {
		return expInfoByExpId;
	}

	/**
	 * Util method for indexing soil by their id.
	 * 
	 * @param data
	 */
	public void indexSoilById(Map data) {
		ArrayList<BucketEntry> soilList = getPackageContents(data, BUCKET_SOILS);
		for (BucketEntry soil : soilList) {
			soilById.put(soil.getValues().get("soil_id"), soil);
		}
	}

	@SuppressWarnings("unchecked")
	public void writeFile(String file, Map data) {
		BucketEntry soil;
		BucketEntry managementBucket;
		BucketEntry initialConditionBucket;
		ArrayList<LinkedHashMap<String, String>> listOfExperiments;
		ArrayList<HashMap<String, String>> fertilizerList;
		ArrayList<HashMap<String, String>> irigationList;
		ArrayList<HashMap<String, String>> tillageList;
		HashMap<String, HashMap<String, String>> mgmtDataByEvent;
		HashMap<String, String> initialConditions;
		SoilAndInitOutput soilOut;
		ExperimentInfo experimentInfo;
		String soilId;
		String experimentId;
		String weatherStationId;
		String content;
		String crid;
		String icpcr;

		try {
			listOfExperiments = (ArrayList<LinkedHashMap<String, String>>) data.get(BUCKET_EXPERIMENTS);
			// First index soil by id
			indexSoilById(data);
			soilOut = new SoilAndInitOutput(file);
			for (Map experiment : listOfExperiments) {

				soilId = (String) experiment.get("soil_id");
				weatherStationId = toWeatherId((String) experiment.get("wst_id"));
				experimentId = (String) experiment.get("exname");
				experimentInfo = new ExperimentInfo(experimentId, soilId, weatherStationId);
				soil = soilById.get(experimentInfo.getSoilId());
				log.info("Processing experiment : " + experimentInfo.getExpId());
				// 1 - Generating soil and initialization files for this experiment
				soilOut.writeFile(experiment, experimentInfo, soil);

				// Generating management files
				initialConditionBucket = getBucket(experiment, BUCKET_INTIAL_CONDITIONS);
				managementBucket = getBucket(experiment, BUCKET_MANAGEMENT);
				initialConditions = initialConditionBucket.getValues();
				defaultValueForMap(new String[] { "icrli" }, initialConditions);
				getConverter().convertFirstLevelRecords(initialConditions);

				mgmtDataByEvent = new HashMap<String, HashMap<String, String>>();
				fertilizerList = new ArrayList<HashMap<String, String>>();
				irigationList = new ArrayList<HashMap<String, String>>();
				tillageList = new ArrayList<HashMap<String, String>>();

				// Fill missing values with default values
				getConverter().convertNestedRecords(managementBucket.getDataList(), MANAGEMENT_PARAMETERS_TO_CONVERT);
				computeExperimentDuration(initialConditions, experiment, experimentInfo);

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
				crid = getCropIdFromPlantingEvent(mgmtDataByEvent);
				icpcr = toSticsCode(getValueOr(initialConditions, "icpcr", defaultValue("icpcr")));
				initialConditions.put("icpcr", icpcr == null ? toSticsCode(crid) : icpcr);
				processingDates(fertilizerList, irigationList, tillageList, mgmtDataByEvent);
				content = generateTecfile(mgmtDataByEvent, fertilizerList, initialConditions, irigationList, tillageList);
				experimentInfo.setCropId(crid);
				experimentInfo.setMngmtFile(newFile(content, file, experimentInfo.getExpId() + "_" + crid + "_tec.xml").getName());
				expInfoByExpId.put((String) experiment.get("exname"), experimentInfo);
			}
			soilOut.generateSoilsFile();
		} catch (IOException e) {
			log.error("Unable to generate tec file.");
			log.error(e.toString());
		}

	}

	private void processingDates(List<HashMap<String, String>> fertilizerList, List<HashMap<String, String>> irigationList, List<HashMap<String, String>> tillageList,
			HashMap<String, HashMap<String, String>> mgmtDataByEvent) {
		// We need to sort fertilizer data by date
		sortListByDate(fertilizerList);
		// Convert date in julian day
		convertListOfDate(fertilizerList);
		convertListOfDate(irigationList);
		convertListOfDate(tillageList);
		convertDate(mgmtDataByEvent.get(EVENT_HARVEST));
		convertDate(mgmtDataByEvent.get(EVENT_PLANTING));
	}

	private String getCropIdFromPlantingEvent(Map<String, HashMap<String, String>> mgmtDataByEvent) {
		return mgmtDataByEvent.get(EVENT_PLANTING).get("crid") == null ? defaultValue("crid") : mgmtDataByEvent.get(EVENT_PLANTING).get("crid");
	}

	/**
	 * if the list contains at least one element take it to put values into the context else take the default value
	 * 
	 * @param values
	 * @param params
	 */
	private void defaultIfEmpty(List<HashMap<String, String>> values, String[] params, VelocityContext context) {
		if (values.size() == 0) {
			for (String p : params) {
				context.put(p, defaultValue(p));
			}
		} else {
			for (String p : params) {
				context.put(p, values.get(0).get(p));
			}
		}
	}

	private void computeExperimentDuration(Map<String, String> initialConditions, Map experiment, ExperimentInfo expInfo) {
		Date date;
		String expDur;
		int duration;
		String julianDaydate;

		try {
			if (initialConditions.get("icdat") == null) {
				log.error("The icdat is not avalaible for parsing");
			} else {
				date = formatter.parse((String) initialConditions.get("icdat"));
				expDur = experiment.containsKey("exp_dur") ? (String) experiment.get("exp_dur") : defaultValue("exp_dur");
				duration = Integer.parseInt(expDur);
				expInfo.setDuration(duration);
				julianDaydate = String.valueOf(getJulianDay(date));
				initialConditions.put("icdat", julianDaydate);
				expInfo.setIcdat(julianDaydate);
				expInfo.setStartingDate(date);
			}
			expInfo.setLatitude((String) getValueOr(experiment, "fl_lat", defaultValue("fl_lat")));
		} catch (ParseException e) {
			log.error("Unable to parse icdat field");
			log.error(e.toString());
		}
	}

	public void convertCode(String event, Map<String, String> data) {
		for (String code : codeToMapByEvent.get(event)) {
			if (data.containsKey(code)) {
				log.debug("Code replaced, old val : " + data.get(code) + " new val : " + toSticsCode(data.get(code)));
				data.put(code, toSticsCode(data.get(code)));
			}
		}
	}

	public String generateTecfile(Map<String, HashMap<String, String>> mgmtDataByevent, List<HashMap<String, String>> fertilizerList, Map<String, String> initialConditions,
			List<HashMap<String, String>> irrigationList, List<HashMap<String, String>> tillageList) {
		VelocityContext context = VelocityUtil.newVelocitycontext();
		defaultIfEmpty(fertilizerList, DEFAULT_FERT_PARAMS, context);
		defaultIfEmpty(irrigationList, DEFAULT_IRRIGATION_PARAMS, context);
		context.put(EVENT_FERTILIZER, fertilizerList);
		context.put(EVENT_HARVEST, mgmtDataByevent.get(EVENT_HARVEST));
		context.put(EVENT_TILLAGE, tillageList);
		context.put(EVENT_PLANTING, mgmtDataByevent.get(EVENT_PLANTING));
		context.put(EVENT_IRRIGATION, irrigationList);
		context.put(BUCKET_INTIAL_CONDITIONS, initialConditions);
		return VelocityUtil.getInstance().runVelocity(context, TEC_TEMPLATE_FILE);
	}

	public void convertDate(Map<String, String> data) {
		try {
			// if the structure contains date field, we convert it into julian
			// day
			if (data != null && data.containsKey(DATE)) {
				Date date = formatter.parse(data.get(DATE));
				int julianDayDate = getJulianDay(date);
				data.put(DATE, String.valueOf(julianDayDate));
			}
		} catch (ParseException e) {
			log.error("Invalid date format");
			log.error(e.toString());
		}
	}

	public void convertListOfDate(List<HashMap<String, String>> dataList) {
		for (HashMap<String, String> data : dataList) {
			convertDate(data);
		}
	}

	public void sortListByDate(List<HashMap<String, String>> fertilizerList) {
		Collections.sort(fertilizerList, new Comparator<HashMap<String, String>>() {
			private SimpleDateFormat formatter = new SimpleDateFormat(Const.DATE_FORMAT);
			/**
			 * 
			 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or
			 * greater than the second.
			 */
			public int compare(HashMap<String, String> bucket1, HashMap<String, String> bucket2) {
				Date date1;
				Date date2;
				int result;
				try {
					date1 = formatter.parse(bucket1.get(DATE));
					date2 = formatter.parse(bucket2.get(DATE));
					// check if both are the same
					if (date1.equals(date2)) {
						result = 0;
					} else if (date1.after(date2)) {
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
