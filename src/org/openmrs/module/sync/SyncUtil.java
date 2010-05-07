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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.OrderType;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.LoginCredential;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.FilePackage;
import org.openmrs.module.sync.serialization.IItem;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.notification.Message;
import org.openmrs.notification.MessageException;
import org.openmrs.util.LocaleUtility;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Collection of helpful methods in sync
 */
public class SyncUtil {

	private static Log log = LogFactory.getLog(SyncUtil.class);		
	
	/**
	 * Get the sync work directory in the openmrs application data directory
	 * @return a file pointing to the sync output dir
	 */
	public static File getSyncApplicationDir() {
		return OpenmrsUtil.getDirectoryInApplicationDataDirectory("sync");
	}
	
	public static Object getRootObject(String incoming)
			throws Exception {
		
		Object o = null;
		
		if ( incoming != null ) {
			Record xml = Record.create(incoming);
			Item root = xml.getRootItem();
			String className = root.getNode().getNodeName();
			o = SyncUtil.newObject(className);
		}
		
		return o;
	}

	public static NodeList getChildNodes(String incoming)
			throws Exception {
		NodeList nodes = null;
		
		if ( incoming != null ) {
			Record xml = Record.create(incoming);
			Item root = xml.getRootItem();
			nodes = root.getNode().getChildNodes();
		}
		
		return nodes;
	}

	public static void setProperty(Object o, Node n, ArrayList<Field> allFields ) 
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		String propName = n.getNodeName();
		Object propVal = null;
		propVal = SyncUtil.valForField(propName, n.getTextContent(), allFields, n);
		
		log.debug("Trying to set value to " + propVal + " when propName is " + propName + " and context is " + n.getTextContent());
		
		if ( propVal !=  null ) {
			SyncUtil.setProperty(o, propName, propVal);
			log.debug("Successfully called set" + SyncUtil.propCase(propName) + "(" + propVal + ")" );
		}
	}

	public static void setProperty(Object o, String propName, Object propVal)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Object[] setterParams = new Object[] { propVal };
		
		log.debug("getting setter method");
		Method m = SyncUtil.getSetterMethod(o.getClass(), propName, propVal.getClass());

		boolean acc = m.isAccessible();
		m.setAccessible(true);
		log.debug("about to call " + m.getName());
		try {
			Object voidObj = m.invoke(o, setterParams);
		}
		finally {
			m.setAccessible(acc);
		}
	}
	
	public static String getAttribute(NodeList nodes, String attName, ArrayList<Field> allFields ) {
		String ret = null;
		if ( nodes != null && attName != null ) {
			for ( int i = 0; i < nodes.getLength(); i++ ) {
				Node n = nodes.item(i);
				String propName = n.getNodeName();
				if ( attName.equals(propName) ) {
					Object obj = SyncUtil.valForField(propName, n.getTextContent(), allFields, n);
					if ( obj != null ) ret = obj.toString();
				}
			}
		}

		return ret;
	}
	
	public static String propCase(String text) {
		if ( text != null ) {
			return text.substring(0, 1).toUpperCase() + text.substring(1);
		} else {
			return null;
		}
	}

	public static Object newObject(String className) throws Exception {
		Object o = null;
		if ( className != null ) {
				Class clazz = Context.loadClass(className);
				Constructor ct = clazz.getConstructor();
				o = ct.newInstance();
		}
		return o;
	}
	
	public static ArrayList<Field> getAllFields(Object o) {
		Class clazz = o.getClass();
		ArrayList<Field> allFields = new ArrayList<Field>();
		if ( clazz != null ) {
			Field[] nativeFields = clazz.getDeclaredFields();
			Field[] superFields = null;
			Class superClazz = clazz.getSuperclass();
			while ( superClazz != null && !(superClazz.equals(Object.class)) ) {
				// loop through to make sure we get ALL relevant superclasses and their fields
                if (log.isDebugEnabled())
                    log.debug("Now inspecting superclass: " + superClazz.getName());
                
				superFields = superClazz.getDeclaredFields();
				if ( superFields != null ) {
					for ( Field f : superFields ) {
						allFields.add(f);
					}
				}
				superClazz = superClazz.getSuperclass();
			}
			if ( nativeFields != null ) {
				// add native fields
				for ( Field f : nativeFields ) {
					allFields.add(f);
				}
			}
		}

		return allFields;
	}
	
	public static OpenmrsObject getOpenmrsObj(String className, String uuid) {
		try {
			OpenmrsObject o = Context.getService(SyncService.class).getOpenmrsObjectByUuid((Class<OpenmrsObject>) Context.loadClass(className), uuid);
			
	        if (log.isDebugEnabled()) {
	    		if ( o == null ) {
	    			log.debug("Unable to get an object of type " + className + " with Uuid " + uuid + ";");
	    		}			
	        }
			return o;
		} catch (ClassNotFoundException ex) {
			log.warn("getOpenmrsObj couldn't find class: " + className, ex);
			return null;
		}		
	}
	
	public static Object valForField(String fieldName, String fieldVal, ArrayList<Field> allFields, Node n) {
		Object o = null;
		
		// the String value on the node specifying the "type"
		String nodeDefinedClassName = null;
		if (n != null) {
			Node tmpNode = n.getAttributes().getNamedItem("type");
			if (tmpNode != null)
				nodeDefinedClassName = tmpNode.getTextContent();
		}
		
		// TODO: Speed up sync by passing in a Map of String fieldNames instead of list of Fields ? 
		// TODO: Speed up sync by returning after "o" is first set?  Or are we doing "last field wins" ?
		for ( Field f : allFields ) {
			//log.debug("field is " + f.getName());
			if ( f.getName().equals(fieldName) ) {
				log.debug("found Field " + fieldName + " with type is " + f.getGenericType());
				Class classType = null;
				String className = f.getGenericType().toString(); // the string class name for the actual field
				
				// if its a collection, set, list, etc
				if (ParameterizedType.class.isAssignableFrom(f.getGenericType().getClass())) {
					ParameterizedType pType = (ParameterizedType)f.getGenericType();
					classType = (Class)pType.getRawType(); // can this be anything but Class at this point?!
				}
				
				if ( className.startsWith("class ") ) {
					className = className.substring("class ".length());
					classType = (Class)f.getGenericType();
				}
				else {
					log.trace("Abnormal className for " + f.getGenericType());
				}

				// we have to explicitly create a new value object here because all we have is a string - won't know how to convert
				if ( OpenmrsObject.class.isAssignableFrom(classType) ) {
					o = getOpenmrsObj(className, fieldVal);
				}
				else if ("java.lang.Integer".equals(className) && !("integer".equals(nodeDefinedClassName) || "java.lang.Integer".equals(nodeDefinedClassName) )) {
					// if we're dealing with a field like PersonAttributeType.foreignKey, the actual value was changed from
					// an integer to a uuid by the HibernateSyncInterceptor.  The nodeDefinedClassName is the node.type which is the 
					// actual classname as defined by the PersonAttributeType.format.  However, the field.getClassName is 
					// still an integer because thats what the db stores.  we need to convert the uuid to the pk integer and return it
					OpenmrsObject obj = getOpenmrsObj(nodeDefinedClassName, fieldVal);
					o = obj.getId();
				}
				else if ("java.lang.String".equals(className) && !("string".equals(nodeDefinedClassName) || "java.lang.String".equals(nodeDefinedClassName) || "integer".equals(nodeDefinedClassName) || "java.lang.Integer".equals(nodeDefinedClassName) || fieldVal.isEmpty())) {
					// if we're dealing with a field like PersonAttribute.value, the actual value was changed from
					// a string to a uuid by the HibernateSyncInterceptor.  The nodeDefinedClassName is the node.type which is the 
					// actual classname as defined by the PersonAttributeType.format.  However, the field.getClassName is 
					// still String because thats what the db stores.  we need to convert the uuid to the pk integer/string and return it
					OpenmrsObject obj = getOpenmrsObj(nodeDefinedClassName, fieldVal);
					o = obj.getId().toString(); // call toString so the class types match when looking up the setter
				}
				else if ((o = convertStringToObject(fieldVal, className)) != null) {
					log.trace("Converted " + fieldVal + " into " + className);
				}
				else if (Collection.class.isAssignableFrom(classType)) {
					// this is a collection of items. this is intentionally not in the convertStringToObject method
					
					Collection tmpCollection = null;
					if (Set.class.isAssignableFrom(classType))
						tmpCollection = new LinkedHashSet();
					else
						tmpCollection = new Vector();
					
					// get the type of class held in the collection
					String collectionTypeClassName = null;
					java.lang.reflect.Type collectionType = ((java.lang.reflect.ParameterizedType)f.getGenericType()).getActualTypeArguments()[0];
					if ( collectionType.toString().startsWith("class ") )
						collectionTypeClassName = collectionType.toString().substring("class ".length());
					
					// change the string to just a comma delimited list
					fieldVal = fieldVal.replaceFirst("\\[", "").replaceFirst("\\]", "");
					
					for (String eachFieldVal : fieldVal.split(",")) {
						eachFieldVal = eachFieldVal.trim(); // take out whitespace
						// convert to a simple object
						Object tmpObject = convertStringToObject(eachFieldVal, collectionTypeClassName);
						if (tmpObject == null)
							log.error("Unable to convert: " + eachFieldVal + " to a " + collectionTypeClassName);
						else
							tmpCollection.add(tmpObject);
					}
					
					o = tmpCollection;
				}
				else {
					log.debug("Don't know how to deserialize class: " + className);
				}
			}
		}
		
		if ( o == null )
			log.debug("Never found a property named: " + fieldName + " for this class");
		
		return o;
	}

    /**
     * Converts the given string into an object of the given className.  Supports basic objects like String, 
     * Integer, Long, Float, Double, Boolean, Date, and Locale. 
     * 
     * @param fieldVal the string object representation
     * @param className the name of the class that is represented by this string. e.g. "java.lang.Integer"
     * @return object of type "className"
     */
    private static Object convertStringToObject(String fieldVal, String className) {
    	
    	Object o = null; // the object to return
    	
    	if ( "java.lang.String".equals(className) ) {
    		o = (Object)(new String(fieldVal));
        } else if ( "java.lang.Short".equals(className) ) {
            try {
                o = (Object)(Short.valueOf(fieldVal));
            } catch (NumberFormatException nfe) {
                log.debug("NumberFormatException trying to turn " + fieldVal + " into a Short");
            }
    	} else if ( "java.lang.Integer".equals(className) ) {
    		try {
    			o = (Object)(Integer.valueOf(fieldVal));
    		} catch (NumberFormatException nfe) {
    			log.debug("NumberFormatException trying to turn " + fieldVal + " into a Integer");
    		}
    	} else if ( "java.lang.Long".equals(className) ) {
    		try {
    			o = (Object)(Long.valueOf(fieldVal));
    		} catch (NumberFormatException nfe) {
    			log.debug("NumberFormatException trying to turn " + fieldVal + " into a Long");
    		}
    	} else if ( "java.lang.Float".equals(className) ) {
    		try {
    			o = (Object)(Float.valueOf(fieldVal));
    		} catch (NumberFormatException nfe) {
    			log.debug("NumberFormatException trying to turn " + fieldVal + " into a Float");
    		}
    	} else if ( "java.lang.Double".equals(className) ) {
    		try {
    			o = (Object)(Double.valueOf(fieldVal));
    		} catch (NumberFormatException nfe) {
    			log.debug("NumberFormatException trying to turn " + fieldVal + " into a Double");
    		}
    	} else if ( "java.lang.Boolean".equals(className) ) {
    		o = (Object)(Boolean.valueOf(fieldVal));
    	} else if ( "java.util.Date".equals(className) ) {
    		SimpleDateFormat sdf = new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK);
    		Date d;
    		try {
    			d = sdf.parse(fieldVal);
    			o = (Object)(d);
    		} catch (ParseException e) {
    			log.debug("DateParsingException trying to turn " + fieldVal + " into a date, so retrying with backup mask");
    			try {
    				SimpleDateFormat sdfBackup = new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK_BACKUP);
    				d = sdfBackup.parse(fieldVal);
    				o = (Object)(d);
    			} catch (ParseException pee) {
    				log.debug("Still getting DateParsingException trying to turn " + fieldVal + " into a date, so retrying with backup mask");
    			}
    		}
    	} else if ( "java.util.Locale".equals(className) ) {
    		o = LocaleUtility.fromSpecification(fieldVal);
    	}
    	
    	return o;
    }

	/**
     * 
     * Finds property 'get' accessor based on target type and property name.
     * 
     * @return Method object matching name and param, else null
     * 
     * @see getPropertyAccessor(Class objType, String methodName, Class propValType)
     */
    public static Method getGetterMethod(Class objType, String propName) {
        String methodName = "get" + propCase(propName);
        return SyncUtil.getPropertyAccessor(objType, methodName, null);
    }

    /**
     * 
     * Finds property 'set' accessor based on target type, property name, and set method parameter type.
     * 
     * @return Method object matching name and param, else null
     * 
     * @see getPropertyAccessor(Class objType, String methodName, Class propValType)
     */
    public static Method getSetterMethod(Class objType, String propName, Class propValType) {
        String methodName = "set" + propCase(propName);
        return SyncUtil.getPropertyAccessor(objType, methodName, propValType);
    }
    
    /**
     * 
     * Constructs a Method object for invocation on instances of objType class 
     * based on methodName and the method parameter type. Handles only propery accessors - thus takes
     * Class propValType and not Class[] propValTypes.
     * <p>
     * If necessary, this implementation traverses both objType and  propValTypes type hierarchies in search for the 
     * method signature match.
     * 
     * @param objType Type to examine.
     * @param methodName Method name.
     * @param propValType Type of the parameter that method takes. If none (i.e. getter), pass null.
     * @return Method object matching name and param, else null
     */
    private static Method getPropertyAccessor(Class objType, String methodName, Class propValType) {
		// need to try to get setter, both in this object, and its parent class 
		Method m = null;
        boolean continueLoop = true;
        
        // Fix - CA - 22 Jan 2008 - extremely odd Java Bean convention that says getter/setter for fields
        // where 2nd letter is capitalized (like "aIsToB") first letter stays lower in getter/setter methods
        // like "getaIsToB()".  Hence we need to try that out too
        String altMethodName = methodName.substring(0, 3) + methodName.substring(3, 4).toLowerCase() + methodName.substring(4);

        try {
			Class[] setterParamClasses = null;
            if (propValType != null) { //it is a setter
                setterParamClasses = new Class[1];
                setterParamClasses[0] = propValType;
            }
			Class clazz = objType;
    
            // it could be that the setter method itself is in a superclass of objectClass/clazz, so loop through those
			while ( continueLoop && m == null && clazz != null && !clazz.equals(Object.class) ) {
				try {
					m = clazz.getMethod(methodName, setterParamClasses);
					continueLoop = false;
					break; //yahoo - we got it using exact type match
				} catch (SecurityException e) {
					m = null;
				} catch (NoSuchMethodException e) {
					m = null;
				}
				
				//not so lucky: try to find method by name, and then compare params for compatibility 
				//instead of looking for the exact method sig match 
                Method[] mes = objType.getMethods();
                for (Method me : mes) {
                	if (me.getName().equals(methodName) || me.getName().equals(altMethodName) ) {
                		Class[] meParamTypes = me.getParameterTypes();
                		if (propValType != null && meParamTypes != null && meParamTypes.length == 1 && meParamTypes[0].isAssignableFrom(propValType)) {
                			m = me;
            				continueLoop = false; //aha! found it
            				break;
                		}
                	}
                }
                
                if ( continueLoop ) clazz = clazz.getSuperclass();
    		}
        }
        catch(Exception ex) {
            //whatever happened, we didn't find the method - return null
            m = null;
            log.warn("Unexpected exception while looking for a Method object, returning null",ex);
        }
        
        if (m == null) {
	        if (log.isWarnEnabled())
	            log.warn("Failed to find matching method. type: " + objType.getName() + ", methodName: " + methodName);
        }
				
		return m;
	}

    private static OpenmrsObject findByUuid(Collection<? extends OpenmrsObject> list, OpenmrsObject toCheck){

    	for(OpenmrsObject element:list){
    		if(element.getUuid().equals(toCheck.getUuid()))
    			return element;
    	}
    	
    	return null;
    }
    
    /**
     * Uses the generic hibernate API to perform the save<br/>
     * Possible exceptions that need post-processing:<br/>
     * <br />Form - may need rebuild XSN  
     *  
     * @param o object to save
     * @param className type
     * @param Uuid unique id of the object that is being saved
     * @param preCommitRecordActions actions set to be added to if needed, also SyncIngestServiceImpl.processOpenmrsObject 
     * 
     */
    public static synchronized void updateOpenmrsObject(OpenmrsObject o, 
    		String className, 
    		String Uuid, 
    		List<SyncPreCommitAction> preCommitRecordActions) {
    	
    	// TODO allow this to be done by modules somehow
    	// some pre-flush modifications
    	// if an obs comes through with a non-null voidReason, make sure we change it back to using a PK
    	if ("org.openmrs.Obs".equals(className)) {
    		Obs obs = (Obs)o;
			String voidReason = obs.getVoidReason();
			if (StringUtils.hasLength(voidReason)) {
	    		int start = voidReason.lastIndexOf(" ") + 1; // assumes uuids don't have spaces 
				int end = voidReason.length() - 1;
				String uuid = voidReason.substring(start, end);
				try {
					OpenmrsObject openmrsObject = getOpenmrsObj("org.openmrs.Obs", uuid);
					Integer obsId = openmrsObject.getId();
					obs.setVoidReason(voidReason.substring(0, start) + obsId + ")");
				}
				catch (Exception e) {
					log.trace("unable to get uuid from obs uuid: " + uuid, e);
				}
			}
    	}
    	else if ("org.openmrs.api.db.LoginCredential".equals(className)) {
    		LoginCredential login = (LoginCredential)o;
			OpenmrsObject openmrsObject = getOpenmrsObj("org.openmrs.User", login.getUuid());
			Integer userId = openmrsObject.getId();
			login.setUserId(userId);
    	}
    	
    	// now do the save
    	
    	if ( o != null ) {
			Context.getService(SyncService.class).saveOrUpdate(o);
		} else {
			log.warn("Will not update OpenMRS object that is NULL");
		}
    	
    	// for forms, we need signal to rebuild XSN, do it here
    	if ("org.openmrs.Form".equals(className)) {
    		if (preCommitRecordActions != null)
    			preCommitRecordActions.add(new SyncPreCommitAction(SyncPreCommitAction.PreCommitActionName.REBUILDXSN, o));
    	}
    	// if conceptName change comes on its own, trigger clean up of related concept words also
    	else if ("org.openmrs.ConceptName".equals(className)) {
    		if (preCommitRecordActions != null && o != null) {
    			Concept c = ((ConceptName)o).getConcept();
    			c.addName((ConceptName)o);
    			preCommitRecordActions.add(new SyncPreCommitAction(SyncPreCommitAction.PreCommitActionName.UPDATECONCEPTWORDS, c));
    		}
    	}
//    	else if (Concept.class.isAssignableFrom(o.getClass())) {
//    		if (preCommitRecordActions != null && o != null) {
//    			preCommitRecordActions.add(new SyncPreCommitAction(SyncPreCommitAction.PreCommitActionName.UPDATECONCEPTWORDS, o));
//    		}
//    	}
    	
    }  
	
    /**
     * Helper method for the {@link UUID#randomUUID()} method.
     * 
     * @return a generated random uuid
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }
    
    public static String displayName(String className, String Uuid) {

    	String ret = "";
    	
        // get more identifying info about this object so it's more user-friendly
        if ( className.equals("Person") || className.equals("User") || className.equals("Patient") ) {
            Person person = Context.getPersonService().getPersonByUuid(Uuid);
            if ( person != null ) ret = person.getPersonName().toString();
        }
        if ( className.equals("Encounter") ) {
            Encounter encounter = Context.getEncounterService().getEncounterByUuid(Uuid);
            if ( encounter != null ) {
                ret = encounter.getEncounterType().getName() 
                               + (encounter.getForm() == null ? "" : " (" + encounter.getForm().getName() + ")");
            }
        }
        if ( className.equals("Concept") ) {
            Concept concept = Context.getConceptService().getConceptByUuid(Uuid);
            if ( concept != null ) ret = concept.getName(Context.getLocale()).getName();
        }
        if ( className.equals("Drug") ) {
            Drug drug = Context.getConceptService().getDrugByUuid(Uuid);
            if ( drug != null ) ret = drug.getName();
        }
        if ( className.equals("Obs") ) {
            Obs obs = Context.getObsService().getObsByUuid(Uuid);
            if ( obs != null ) ret = obs.getConcept().getName(Context.getLocale()).getName();
        }
        if ( className.equals("DrugOrder") ) {
            DrugOrder drugOrder = (DrugOrder)Context.getOrderService().getOrderByUuid(Uuid);
            if ( drugOrder != null ) ret = drugOrder.getDrug().getConcept().getName(Context.getLocale()).getName();
        }
        if ( className.equals("Program") ) {
        	Program program = Context.getProgramWorkflowService().getProgramByUuid(Uuid);
        	if ( program != null ) ret = program.getConcept().getName(Context.getLocale()).getName();
        }
        if ( className.equals("ProgramWorkflow") ) {
        	ProgramWorkflow workflow = Context.getProgramWorkflowService().getWorkflowByUuid(Uuid);
        	if ( workflow != null ) ret = workflow.getConcept().getName(Context.getLocale()).getName();
        }
        if ( className.equals("ProgramWorkflowState") ) {
        	ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(Uuid);
        	if ( state != null ) ret = state.getConcept().getName(Context.getLocale()).getName();
        }
        if ( className.equals("PatientProgram") ) {
        	PatientProgram patientProgram = Context.getProgramWorkflowService().getPatientProgramByUuid(Uuid);
        	String pat = patientProgram.getPatient().getPersonName().toString();
        	String prog = patientProgram.getProgram().getConcept().getName(Context.getLocale()).getName();
        	if ( pat != null && prog != null ) ret = pat + " - " + prog;
        }
        if ( className.equals("PatientState") ) {
        	PatientState patientState = Context.getProgramWorkflowService().getPatientStateByUuid(Uuid);
        	String pat = patientState.getPatientProgram().getPatient().getPersonName().toString();
        	String st = patientState.getState().getConcept().getName(Context.getLocale()).getName();
        	if ( pat != null && st != null ) ret = pat + " - " + st;
        }

        if ( className.equals("PersonAddress") ) {
        	PersonAddress address = Context.getPersonService().getPersonAddressByUuid(Uuid);
        	String name = address.getPerson().getFamilyName() + " " + address.getPerson().getGivenName();
        	name += address.getAddress1() != null && address.getAddress1().length() > 0 ? address.getAddress1() + " " : "";
        	name += address.getAddress2() != null && address.getAddress2().length() > 0 ? address.getAddress2() + " " : "";
        	name += address.getCityVillage() != null && address.getCityVillage().length() > 0 ? address.getCityVillage() + " " : "";
        	name += address.getStateProvince() != null && address.getStateProvince().length() > 0 ? address.getStateProvince() + " " : "";
        	if ( name != null ) ret = name;
        }

        if ( className.equals("PersonName") ) {
        	PersonName personName = Context.getPersonService().getPersonNameByUuid(Uuid);
        	String name = personName.getFamilyName() + " " + personName.getGivenName();
        	if ( name != null ) ret = name;
        }

        if ( className.equals("Relationship") ) {
        	Relationship relationship = Context.getPersonService().getRelationshipByUuid(Uuid);
        	String from = relationship.getPersonA().getFamilyName() + " " + relationship.getPersonA().getGivenName();
        	String to = relationship.getPersonB().getFamilyName() + " " + relationship.getPersonB().getGivenName();
        	if ( from != null && to != null ) ret += from + " to " + to;
        }

        if ( className.equals("RelationshipType") ) {
        	RelationshipType type = Context.getPersonService().getRelationshipTypeByUuid(Uuid);
        	ret += type.getaIsToB() + " - " + type.getbIsToA();
        }

        if ( className.equals("PersonAttributeType") ) {
        	PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(Uuid);
        	ret += type.getName();
        }

        if ( className.equals("Location") ) {
        	Location loc = Context.getLocationService().getLocationByUuid(Uuid);
        	ret += loc.getName();
        }

        if ( className.equals("EncounterType") ) {
        	EncounterType type = Context.getEncounterService().getEncounterTypeByUuid(Uuid);
        	ret += type.getName();
        }

        if ( className.equals("OrderType") ) {
        	OrderType type = Context.getOrderService().getOrderTypeByUuid(Uuid);
        	ret += type.getName();
        }

        return ret;
    }
        
    /**
     * Deletes instance of OpenmrsObject. Used to process SyncItems with state of deleted.
     */
	public static synchronized void deleteOpenmrsObject(OpenmrsObject o) {
		Context.getService(SyncService.class).deleteOpenmrsObject(o);
		
		if (o instanceof org.openmrs.Concept || o instanceof org.openmrs.ConceptName) {
			//delete concept words explicitly
			//TODO
		}
	}

	public static void sendSyncErrorMessage(SyncRecord syncRecord, RemoteServer server, Exception exception) { 
		
		try {
			String adminEmail = Context.getService(SyncService.class).getAdminEmail();
			
			if (adminEmail == null || adminEmail.length() == 0 ) { 
				log.warn("Sync error message could not be sent because " + SyncConstants.PROPERTY_SYNC_ADMIN_EMAIL + " is not configured.");
			} 
			else if (adminEmail != null) { 
				log.info("Preparing to send sync error message via email to " + adminEmail);
			
				Message message = new Message();
				message.setSender("info@openmrs.org");
				message.setSentDate(new Date());
				message.setSubject(exception.getMessage());
				message.addRecipient(adminEmail);
			

			
				StringBuffer content = new StringBuffer();
			
			
				content.
					append("ALERT: Synchronization has stopped between\n").
					append("local server (").append(Context.getService(SyncService.class).getServerName()).
					append(") and remote server ").append(server.getNickname()).append("\n\n").
					append("Summary of failing record\n").
					append("Original Uuid:          " + syncRecord.getOriginalUuid()).
					append("Contained classes:      " + syncRecord.getContainedClassSet()).
					append("Contents:\n");
			
		        try {
		        	log.info("Sending email with sync record: " + syncRecord);
	
		        	for (SyncItem item :syncRecord.getItems()) { 
		        		log.info("Sync item content: " + item.getContent());
		        	}
		        	
					FilePackage pkg = new FilePackage();
			        Record record = pkg.createRecordForWrite("SyncRecord");
			        Item top = record.getRootItem();
			        ((IItem) syncRecord).save(record, top);
			        content.append(record.toString());
			        
		        } catch (Exception e) {
		        	log.warn("An error occurred while retrieving sync record payload", e);
		        	log.warn("Sync record: " + syncRecord.toString());
		        }			
				message.setContent(content.toString());
				
				// Send message
				Context.getMessageService().sendMessage(message);
			
		        log.info("Sent sync error message to " + adminEmail);
			}			
			
		} catch (MessageException e) { 
			log.error("An error occurred while sending the sync error message", e);
		} 
		
	}
	
	
	
	/**
	 * 
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	public static String readContents(InputStream inputStream, boolean isCompressed) throws Exception { 
		StringBuffer contents = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, SyncConstants.UTF8));
		
		String line = "";
		while ((line = reader.readLine()) != null) {
			contents.append(line);
		}
		
		return contents.toString();		
	}
	

	public static byte [] compress(String content) throws IOException { 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CheckedOutputStream cos = new CheckedOutputStream(baos, new CRC32());			
		GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(cos));			
		IOUtils.copy(new ByteArrayInputStream(content.getBytes()), zos);		
		return baos.toByteArray();
	}
	
	public static String decompress(byte[] data) throws IOException { 		
		ByteArrayInputStream bais2 = new ByteArrayInputStream(data);	        
        CheckedInputStream cis = new CheckedInputStream(bais2, new CRC32());
        GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(cis));            
        InputStreamReader reader = new InputStreamReader(zis);
		BufferedReader br = new BufferedReader(reader);
		StringBuffer buffer = new StringBuffer();
		String line = "";
		while ((line = br.readLine()) != null) {
			buffer.append(line);
		}
		return buffer.toString();
	}
	
	/**
	 * Rebuilds XSN form. This is needed for ingest when form is received from remote server; template files that are contained in xsn
	 * in fromentry_xsn table need to be updated. Supported way to do this is to ask formentry module to rebuild XSN. Invoking method via
	 * reflection is temporary workaround until sync is in trunk: at that point advice point should be registered on sync service that 
	 * formentry could respond to by calling rebuild. 
	 * 
	 * @param form form to rebuild xsn for
	 */
	private static void rebuildXSN(Form form) {
		Object o = null;
		Class c = null;
		Method m = null;
		String msg = null;
		
		if (form == null) {
			return;
		}
		
		try {
			msg = "Processing form with id: " + form.getFormId().toString();
			
			boolean rebuildXsn = true;
			try {
				c = Context.loadClass("org.openmrs.module.formentry.FormEntryService");
			} catch(Exception e){}
			if (c==null) {
				log.warn("Failed to find FormEntryService in formentry module; is module loaded? " + msg);
				return;
			}
			try {
				Object formentryservice = Context.getService(c);
				m = formentryservice.getClass().getDeclaredMethod("getFormEntryXsn", new Class[]{form.getClass()});
				Object xsn = m.invoke(formentryservice, form);
				if (xsn == null)
					rebuildXsn = false;
			}
			catch (Exception e) {
				log.warn("Failed to test for formentry xsn existance");
			}
			
			if (rebuildXsn) {
				try {
					c = Context.loadClass("org.openmrs.module.formentry.FormEntryUtil");
				} catch(Exception e){}
				if (c==null) {
					log.warn("Failed to retrieve handle to FormEntryUtil in formentry module; is module loaded? " + msg);
					return;
				}
				
				try {
				    m = c.getDeclaredMethod("rebuildXSN", new Class[]{form.getClass()});
				} catch(Exception e) {}
			    if (m==null) {
			    	log.warn("Failed to retrieve handle to rebuildXSN method in FormEntryUtil; is module loaded? " + msg);
			    	return;
			    }
			    
			    //finally execute it
			    m.invoke(null, form);
			}
					
		}	
		catch (Exception e) {
			log.error("FormEntry module present but failed to rebuild XSN, see stack for error detail." + msg,e);
			throw new SyncException("FormEntry module present but failed to rebuild XSN, see server log for the stacktrace for error details " + msg, e);
		}
		return;
	}
	
	/**
	 * Applies the 'actions' identified during the processing of the record that need to be 
	 * processed (for whatever reason) just before the sync record is to be committed.
	 * 
	 * The actions understood by this method are those listed in SyncUtil.PreCommitActionName enum:
	 * <br/>REBUILDXSN 
	 * <br/>- call to formentry module and attempt to rebuild XSN, 
	 * <br/>- HashMap object will contain instance of Form object to be rebuilt
	 * <br/>UPDATECONCEPTWORDS 
	 * <br/>- call to concept service to update concept words for given concept 
	 * <br/>- HashMap object will contain instance of Concept object which concept words are to be rebuilt
	 * 
	 * @param preCommitRecordActions actions to be applied
	 * 
	 */
	public static void applyPreCommitRecordActions(List<SyncPreCommitAction> preCommitRecordActions) {
		
		if(preCommitRecordActions == null)
			return;
		
		for(SyncPreCommitAction action : preCommitRecordActions) {
			if(action ==null)
				break; //this should never happen
			
			//actions.
			//now process actions
			if (action.getName().equals(SyncPreCommitAction.PreCommitActionName.REBUILDXSN)) {
				
				Object o = action.getParam();
				if (o != null && (o instanceof Form) ) {
					SyncUtil.rebuildXSN((Form)action.getParam());
				} else {
					//error: action was scheduled as rebuild XSN but param passed was not form
					throw new SyncException("REBUILDXSN action was scheduled for 'PreCommitRecordActions' exection but param passed was not From, param passed was:" + action.getParam() );					
				}
			}
			else if (action.getName().equals(SyncPreCommitAction.PreCommitActionName.UPDATECONCEPTWORDS)) {
				
				Object o = action.getParam();
				if (o != null && Concept.class.isAssignableFrom(o.getClass()) ) {
					Context.getConceptService().updateConceptWord((Concept)o);
				} else {
					//error: action was scheduled as updateconceptwords but param passed was not form
					throw new SyncException("UPDATECONCEPTWORDS action was scheduled for 'PreCommitRecordActions' exection but param passed was not Concept, param passed was:" + action.getParam() );					
				}
			}
			else {
				//error: action was scheduled for execution that is not understood by this method, throw exception
				throw new SyncException("Action was scheduled for 'PreCommitRecordActions' exection that is not understood, action name:" + action.getName().toString() );
			}
		}
		
		return;
	}
}
