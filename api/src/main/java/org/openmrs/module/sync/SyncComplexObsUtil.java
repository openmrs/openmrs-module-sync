/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync;

import java.io.File;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Utility class for working with complex obs
 * This is implemented as a GlobalPropertyListener and ApplicationContextAware in order to ensure that global property
 * values can be accessed reliably without the need to hit the database, as these are accessed by the Interceptor and
 * can cause issues by forcing a flush upon query.
 */
public class SyncComplexObsUtil implements GlobalPropertyListener, ApplicationContextAware {

    private static final Log log = LogFactory.getLog(SyncComplexObsUtil.class);

    public static final String GP_NAME = OpenmrsConstants.GLOBAL_PROPERTY_COMPLEX_OBS_DIR;
    public static final String DEFAULT_VAL = "complex_obs";
    public static String COMPLEX_OBS_DIR = DEFAULT_VAL;

    public static final String VALUE_COMPLEX = "valueComplex";
	public static final String COMPLEX_DATA = "complexData";

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		COMPLEX_OBS_DIR = Context.getAdministrationService().getGlobalProperty(GP_NAME, DEFAULT_VAL);
	}

	@Override
	public boolean supportsPropertyName(String s) {
		return GP_NAME.equals(s);
	}

	@Override
	public void globalPropertyChanged(GlobalProperty globalProperty) {
		log.debug("Global property <" + globalProperty.getProperty() + "> changed");
		COMPLEX_OBS_DIR = globalProperty.getPropertyValue();
	}

	@Override
	public void globalPropertyDeleted(String s) {
		log.debug("Global property <" + s + "> deleted");
		COMPLEX_OBS_DIR = DEFAULT_VAL;
	}

	/**
	 * Adapted from AbstractHandler
	 */
	public static File getComplexDataFile(String valueComplex) {
		File ret = null;
		try {
			if (StringUtils.isNotBlank(valueComplex)) {
				String[] names = valueComplex.split("\\|");
				String filename = names.length < 2 ? names[0] : names[names.length - 1];
				File dir = OpenmrsUtil.getDirectoryInApplicationDataDirectory(COMPLEX_OBS_DIR);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				ret = new File(dir, filename);
			}
		}
		catch (Exception e) {
			log.warn("Error trying to retrieve complex data file for obs: " + valueComplex, e);
		}
		return ret;
	}

	public static String getComplexDataEncoded(String valueComplex) {
		String ret = null;
		try {
			File complexObsFile = getComplexDataFile(valueComplex);
			if (complexObsFile != null && complexObsFile.exists()) {
				log.debug("Found a complex obs file at: " + complexObsFile);
				byte[] fileBytes = FileUtils.readFileToByteArray(complexObsFile);
				ret = Base64.encodeBase64String(fileBytes);
			}
		}
		catch (Exception e) {
			log.warn("Error trying to retrieve complex data for obs: " + valueComplex, e);
		}
		return ret;
	}

	public static void setComplexDataForObs(String valueComplex, String encodedData) {
		try {
			File complexObsFile = getComplexDataFile(valueComplex);
			if (complexObsFile != null && StringUtils.isNotBlank(encodedData)) {
				byte[] bytes = Base64.decodeBase64(encodedData);
				FileUtils.writeByteArrayToFile(complexObsFile, bytes);
			}
		}
		catch (Exception e) {
			log.warn("Error writing complex data for obs: " + valueComplex, e);
		}
	}
}
