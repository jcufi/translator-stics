package org.agmip.translators.stics.output;

import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.agmip.translators.stics.model.ExperimentInfo;
import org.agmip.translators.stics.util.Report;
import org.agmip.translators.stics.util.VelocityUtil;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible of usm file generation (configuration file specific to
 * stics).
 * 
 * @author jucufi
 * 
 */
public class UsmOutput extends SticsFileGenerator {
	private static final Logger log = LoggerFactory.getLogger(UsmOutput.class);
	public static final String EXPERIMENTS = "experiments";
	public static final String DATE_FORMAT = "yyyyMMdd";
	public static final String USM_TEMPLATE_FILE = "/usms_template.vm";
	private Map<String, ExperimentInfo> experimentInfoById;
	private Map<String, ArrayList<String>> weatherById;
	private Map<String, String> stationById;

	public UsmOutput(Map<String, ExperimentInfo> experimentInfoById, Map<String, String> stationById, Map<String, ArrayList<String>> weatherFilesById) {
		this.experimentInfoById = experimentInfoById;
		this.weatherById = weatherFilesById;
		this.stationById = stationById;
	}

	public List<String> filterWeatherFiles(List<String> weatherFiles, int startingYear, int endingYear) {
		List<String> newWeatherFilesList = new ArrayList<String>();
		for (String f : weatherFiles) {
			String[] tmp = f.split("\\.");
			int tmpYear = Integer.parseInt(tmp[1]);
			if ((tmpYear >= startingYear) && (tmpYear <= endingYear)) {
				newWeatherFilesList.add(f);
			}
		}
		return newWeatherFilesList;
	}

	public void writeFile(String file) {
		String content;
		VelocityContext context = VelocityUtil.newVelocitycontext();

		for (String expId : experimentInfoById.keySet()) {
			ExperimentInfo exp = (ExperimentInfo) experimentInfoById.get(expId);
			List<String> newWeatherFilesList = new ArrayList<String>();
			log.debug("Starting date : "+exp.getStartingDate()+", duration for exp "+exp.getExpId()+" : "+exp.getDuration());
			if(exp.getStartingDate() == null){
				Report.addSummary("Starting date for "+exp.getExpId()+" is not set, model will not be able to run.\n");
			}else{
				createWeatherFileList(exp, newWeatherFilesList);	
			}
			exp.setStationFile(stationById.get(exp.getWeatherId()));
		}
		context.put(EXPERIMENTS, experimentInfoById.values());
		content = VelocityUtil.getInstance().runVelocity(context, USM_TEMPLATE_FILE);
		try {
			newFile(content, file, "usms.xml");
			log.info("Generating usm file : usms.xml");
		} catch (IOException e) {
			log.error("Unable to generate usm file.");
			log.error(e.toString());
		}
	}

	private void createWeatherFileList(ExperimentInfo exp, List<String> newWeatherFilesList) {
		Calendar c = Calendar.getInstance();
		c.setTime(exp.getStartingDate());
		int startYear = c.get(Calendar.YEAR);
		newWeatherFilesList.add(exp.getWeatherId()+"."+startYear);
		int size = weatherById.get(exp.getWeatherId()).size();
		newWeatherFilesList.add(weatherById.get(exp.getWeatherId()).get(size - 1));
		exp.setWeatherFiles(newWeatherFilesList);
	}
}
