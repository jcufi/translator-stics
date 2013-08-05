package org.agmip.translators.stics.output;

import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.stics.util.Report;
import org.agmip.translators.stics.util.SticsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for Stics file generation
 * 
 * @author jucufi
 * 
 */
public class SticsOutput implements TranslatorOutput {
	private static final Logger log = LoggerFactory.getLogger(SticsOutput.class);

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDir;
		String jsonFile;
		SticsOutput sticsOut;
		Map data;

		String dataFolder = new File(System.getProperty("user.dir")).getParent() + File.separator + "json-translation-samples" + File.separator;
		//jsonFile = "/mach_fast.json";
		//jsonFile = "/Survey_data_import.json";
		//jsonFile = "Wheat_HSC_All_63_3.3.json";
		jsonFile = "/mach_full.json";
		// jsonFile = "/hsc.json";
		// jsonFile = "/new_version.json";
		// jsonFile = "/KSAS8101WH_1.json";
		// jsonFile = "/MACH0001MZ.json";
		// jsonFile = "/MACH0004MZ.json";
		// Get JSON data
		data = SticsUtil.getDataFrom(jsonFile);
		outputDir = System.getProperty("user.dir") + File.separator + "workspace";
		sticsOut = new SticsOutput();
		sticsOut.writeFile(outputDir, data);
	}

	/**
	 * Entry point for generating all stics files
	 *
	 *@param outputDir
	 *@param data
	 */
	public void writeFile(String outputDir, Map data) {
		WeatherOutput weatherOutput;
		ManagementOutput mgmtOutput;
		try {
			weatherOutput = new WeatherOutput();
			mgmtOutput = new ManagementOutput();
			log.info("Starting Stics translation ...");
			// Run weather and station
			weatherOutput.writeFile(outputDir, data);
			// Run management
			mgmtOutput.writeFile(outputDir, data);
			// Run USM generation
			UsmOutput usm = new UsmOutput(mgmtOutput.getExpInfo(), weatherOutput.getStationFilesById(), weatherOutput.getWeatherFilesById());
			usm.writeFile(outputDir);
		} catch (Exception e) {
			log.error("Unexpected error", e);
		}finally{
			try {
				log.info("Generating file : README.txt");
				newFile(Report.getContent(), outputDir, "README.txt");
				log.info("Translation done!");
			} catch (IOException e) {
				log.error("Unable to generate report");
				log.error(e.toString());
			}
		}
	}

}
