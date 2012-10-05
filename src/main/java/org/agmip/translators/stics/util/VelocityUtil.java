package org.agmip.translators.stics.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

public class VelocityUtil {

	/**
	 * Run velocity
	 * 
	 * @param context
	 * @param templateFile
	 * @return the content of the new file
	 */
	public static String runVelocity(Context context, String templateFile) {
		StringWriter strWritter = new StringWriter();
		try {
			Template template = null;
			try {
				template = Velocity.getTemplate(templateFile);
			} catch (ResourceNotFoundException rnfe) {
				System.err.println("Example : error : cannot find template " + templateFile);
			} catch (ParseErrorException pee) {
				System.err.println("Example : Syntax error in template " + templateFile + ":" + pee);
			}
			BufferedWriter writer = new BufferedWriter(strWritter);
			if (template != null) {
				template.merge(context, writer);
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			System.out.println(e);
		}
		return strWritter.getBuffer().toString();
	}

	public static VelocityContext newVelocitycontext() {
		VelocityContext context = new VelocityContext();
		Velocity.init(System.getProperty("user.dir") + File.separator + "velocity.properties");
		return context;
	}

	/**
	 * Fill velocity context with map set as input parameters
	 * 
	 * @param firstLevelValues
	 * @param nestedValues
	 * @return
	 */
	public static VelocityContext fillVelocityContext(LinkedHashMap<String, String> firstLevelValues, List<LinkedHashMap<String, String>> nestedValues) {
		VelocityContext context = newVelocitycontext();
		context.put("listOfMaps", nestedValues);
		context.put("firstLevelFields", firstLevelValues);
		return context;
	}

}
