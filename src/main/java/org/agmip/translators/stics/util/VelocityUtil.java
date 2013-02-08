package org.agmip.translators.stics.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class for velocity. This class is a singleton to avoid recreating
 * VelocityEngine, all the templates are retrieved from the classpath.
 * 
 * @author jucufi
 * 
 */
public class VelocityUtil {
	private static VelocityUtil instance;
	private static VelocityEngine engine;
	private static final Logger log = LoggerFactory.getLogger(VelocityUtil.class);

	private VelocityUtil() {
		engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		engine.init();
	}

	public static synchronized VelocityUtil getInstance() {
		if (instance == null) {
			instance = new VelocityUtil();
		}
		return instance;
	}

	/**
	 * Run velocity
	 * 
	 * @param context
	 * @param templateFile
	 * @return the content of the new file
	 */
	public String runVelocity(Context context, String templateFile) {
		StringWriter strWritter = new StringWriter();
		Template template = null;
		try {
			try {
				template = engine.getTemplate(templateFile);
			} catch (ResourceNotFoundException rnfe) {
				log.error("Example : error : cannot find template " + templateFile);
			} catch (ParseErrorException pee) {
				log.error("Example : Syntax error in template " + templateFile + ":" + pee);
			}
			BufferedWriter writer = new BufferedWriter(strWritter);
			
			if (template != null) {
				template.merge(context, writer);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			log.error("IO error during velocity generation");
			log.error(e.toString());
		}

		return strWritter.getBuffer().toString();
	}

	/**
	 * Instanciating a new Velocity context
	 * 
	 * @return
	 */
	public static VelocityContext newVelocitycontext() {
		VelocityContext context = new VelocityContext();
		return context;
	}

	/**
	 * Fill velocity context with map set as input parameters
	 * 
	 * @param firstLevelValues
	 * @param nestedValues
	 * @return
	 */
	public static VelocityContext fillVelocityContext(HashMap<String, String> firstLevelValues, List<HashMap<String, String>> nestedValues) {
		VelocityContext context = newVelocitycontext();
		context.put("listOfMaps", nestedValues);
		context.put("firstLevelFields", firstLevelValues);
		return context;
	}

}
