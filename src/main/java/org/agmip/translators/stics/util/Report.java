package org.agmip.translators.stics.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used to build translator report at the end of the process.
 * 
 * @author jucufi
 * 
 */
public final class Report {

	public static Report getInstance() {
		if (singleton == null) {
			singleton = new Report();
		}
		return singleton;
	}

	private static Report singleton;
	private StringBuffer buffer;
	private StringBuffer bufferSummary;
	private StringBuffer bufferParam;
	private Map<String, Integer> paramInfoAdded = new HashMap<String, Integer>();
	private Map<String, String> summaryAdded = new HashMap<String, String>();

	private Report() {
		buffer = new StringBuffer();
		bufferParam = new StringBuffer();
		bufferSummary = new StringBuffer();
		buffer.append("***********************\n");
		buffer.append("Stics translator report\n");
		buffer.append("***********************\n");
	}

	public static String getContent() {
		String result;
		getInstance().buffer.append(getInstance().bufferSummary.toString());
		if (!getInstance().paramInfoAdded.isEmpty()) {
			getInstance().buffer.append("For some parameters default values has been used : \n");
			getInstance().buffer.append(getInstance().bufferParam);
		}
		result = getInstance().buffer.toString();
		return result;
	}

	public static void addParamInfo(String param, String defaultValue) {
		if (!getInstance().paramInfoAdded.containsKey(param)) {
			// ok the information is not recorded
			getInstance().paramInfoAdded.put(param, 1);
			getInstance().bufferParam.append(param + "=" + defaultValue + "\n");
		} 
	}

	public static void addSummary(String summary) {
		if (!getInstance().summaryAdded.containsKey(summary)) {
			getInstance().summaryAdded.put(summary, summary);
			getInstance().bufferSummary.append(summary);
		}
	}

	public static void addInfo(String info) {
		getInstance().buffer.append(info);
	}
}
