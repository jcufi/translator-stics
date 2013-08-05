package org.agmip.translators.stics;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.agmip.translators.stics.output.SticsOutput;
import org.agmip.translators.stics.util.SticsUtil;

public class SticsOutputTest {
	SticsOutput output;

	public SticsOutputTest() {
		output = new SticsOutput();
	}

	public void run(String json) throws IOException {
		SticsOutput sticsOut;
		File outputDir;
		File jsonFile;
		Map data;
		jsonFile = new File(json);
		// Get JSON data
		data = SticsUtil.getDataFrom(json);
		outputDir = new File(System.getProperty("user.dir") + File.separator + "workspace"+File.separator+jsonFile.getName());
		outputDir.createNewFile();
		sticsOut = new SticsOutput();
		sticsOut.writeFile(outputDir.getAbsolutePath(), data);
	}

	public void testExpected() {

		String[] jsonFiles = { "mach_fast.json" };
		// jsonFile = "/hsc.json";
		// jsonFile = "/new_version.json";
		// jsonFile = dataFolder + "mach_fast.json";
		// jsonFile = "/KSAS8101WH_1.json";
		// jsonFile = "/MACH0001MZ.json";
		// jsonFile = "/MACH0004MZ.json";
	}
}
