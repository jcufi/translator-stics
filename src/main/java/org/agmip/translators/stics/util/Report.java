package org.agmip.translators.stics.util;

import java.util.HashMap;

public class Report {

	public static Report getInstance() {
		if (singleton == null) {
			singleton = new Report();
		}
		return singleton;
	}

	private static Report singleton;
	private StringBuffer buffer;
	private StringBuffer bufferParam;

	private Report() {
		buffer = new StringBuffer();
		bufferParam = new StringBuffer();
		buffer.append("***********************\n");
		buffer.append("Stics translator report\n");
		buffer.append("***********************\n");
		buffer.append("For some parameters default values has been used : \n");

	}

	public static String getContent() {
		getInstance().buffer.append(getInstance().bufferParam);
		return getInstance().buffer.toString();
	}

	HashMap<String, Integer> paramInfo = new HashMap<String, Integer>();

	public static void addParamInfo(String param, String defaultValue) {
		if (!getInstance().paramInfo.containsKey(param)) {
			getInstance().paramInfo.put(param, 1);
			getInstance().bufferParam.append(param + "=" + defaultValue + "\n");
		} else {
			// ok information already recorded
		}
	}

	public static void addInfo(String info) {
		getInstance().buffer.append(info);
	}
}
