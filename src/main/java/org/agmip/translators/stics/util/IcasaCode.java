package org.agmip.translators.stics.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.agmip.translators.stics.WeatherOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class use to map stics and icasa codes.
 * 
 * @author jucufi
 * 
 */
public class IcasaCode {
	private static final Logger log = LoggerFactory.getLogger(IcasaCode.class);
	private static String CODE_UNKNOWN = "code_unknown";
	private static Properties codeMap;
	static {
		InputStream inputStream = WeatherOutput.class.getResourceAsStream("/code-mapping.properties");
		codeMap = new Properties();
		try {
			codeMap.load(inputStream);
			log.debug(codeMap.toString());
		} catch (IOException e) {
			log.error(e.toString());
		}
	}

	public static String toSticsCode(String icasaCode) {
		String result;
		if (!codeMap.containsKey(icasaCode)) {
			Report.addInfo("Unknown code : " + icasaCode);
			result = CODE_UNKNOWN;
		} else {
			result = (String) codeMap.get(icasaCode);
		}
		return result;
	}
}
