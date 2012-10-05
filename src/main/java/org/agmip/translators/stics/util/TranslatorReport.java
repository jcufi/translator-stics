package org.agmip.translators.stics.util;

public class TranslatorReport {

	private static TranslatorReport report;
	
	private StringBuffer buffer;
	private TranslatorReport() {
		buffer = new StringBuffer();
	}
	public TranslatorReport getInstance(){
		if(report == null){
			report = new TranslatorReport();
		}
		return report;
	}
	public void add(String info){
		buffer.append(info);
	}
	
	public void toFile(String path, String fileName){
		
	}
	
}
