package org.red5.server.io.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.collections.BeanMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.io.Deserializer;
import org.red5.server.io.Input;
import org.red5.server.io.Output;
import org.red5.server.io.Serializer;

public abstract class AbstractIOTest extends TestCase {

	protected static Log log =
        LogFactory.getLog(AbstractIOTest.class.getName());
	
	protected Serializer serializer;
	protected Deserializer deserializer;
	protected Input in;
	protected Output out;
	
	protected void setUp(){
		serializer = new Serializer();
		deserializer = new Deserializer();
		setupIO();
	}
	
	abstract void setupIO(); 
	abstract void dumpOutput();
	abstract void resetOutput();

	public void testNull(){
		log.debug("Testing null");
		serializer.serialize(out, null);
		dumpOutput();
		Object val = deserializer.deserialize(in);
		Assert.assertEquals(val, null);
		resetOutput();
	}
	
	public void testBoolean(){
		log.debug("Testing boolean");
		serializer.serialize(out, Boolean.TRUE);
		dumpOutput();
		Boolean val = (Boolean) deserializer.deserialize(in);
		Assert.assertEquals(Boolean.TRUE, val);
		resetOutput();
		serializer.serialize(out, Boolean.FALSE);
		dumpOutput();
		val = (Boolean) deserializer.deserialize(in);
		Assert.assertEquals(Boolean.FALSE, val);
		resetOutput();
	}
	
	public void testNumber(){
		log.debug("Testing number");
		int num = 1000;
		serializer.serialize(out, new Integer(num));
		dumpOutput();
		Number n = (Number) deserializer.deserialize(in);
		Assert.assertEquals(n.intValue(), num);
		resetOutput();
	}
	
	public void testString(){
		log.debug("Testing string");
		String inStr = "hello world";
		serializer.serialize(out, inStr);
		dumpOutput();
		String outStr = (String) deserializer.deserialize(in);
		Assert.assertEquals(inStr, outStr);
		resetOutput();
	}
	
	public void testDate(){
		log.debug("Testing date");
		Date dateIn = new Date();
		serializer.serialize(out, dateIn);
		dumpOutput();
		Date dateOut = (Date) deserializer.deserialize(in);
		Assert.assertEquals(dateIn, dateOut);
		resetOutput();
	}

	public void testArray(){
		log.debug("Testing array");
		String[] strArrIn = new String[]{"This","Is","An","Array","Of","Strings"};
		serializer.serialize(out, strArrIn);
		dumpOutput();
		Object[] objArrOut = (Object[]) deserializer.deserialize(in);
		for(int i=0; i<strArrIn.length; i++){
			Assert.assertEquals(strArrIn[i], (String) objArrOut[i]);
		}
		resetOutput();
	}
	
	public void testList(){
		log.debug("Testing list");
		List listIn = new LinkedList();
		listIn.add(null);
		listIn.add(Boolean.FALSE);
		listIn.add(Boolean.TRUE);
		listIn.add(new Integer(1));
		listIn.add("This is a test string");
		listIn.add(new Date());
		serializer.serialize(out,listIn);
		dumpOutput();
		Object[] objArrOut = (Object[]) deserializer.deserialize(in);
		Assert.assertNotNull(objArrOut);
		Assert.assertEquals(listIn.size(), objArrOut.length);
		for(int i=0; i<objArrOut.length; i++){
			Assert.assertEquals(objArrOut[i], listIn.get(i));
		}
		resetOutput();
	}

	public void testJavaBean(){
		log.debug("Testing list");
		TestJavaBean beanIn = new TestJavaBean();
		beanIn.setTestString("test string here");
		beanIn.setTestBoolean((System.currentTimeMillis()%2==0) ? true : false);
		beanIn.setTestBooleanObject((System.currentTimeMillis()%2==0) ? Boolean.TRUE : Boolean.FALSE );
		beanIn.setTestNumberObject(new Integer((int)System.currentTimeMillis()/1000));
		serializer.serialize(out,beanIn);
		dumpOutput();
		Object mapOrBean = deserializer.deserialize(in);
		Assert.assertEquals(beanIn.getClass().getName(), mapOrBean.getClass().getName());
		Map map = (mapOrBean instanceof Map) ? (Map) mapOrBean : new BeanMap(mapOrBean);
		Set entrySet = map.entrySet();
		Iterator it = entrySet.iterator();
		Map beanInMap = new BeanMap(beanIn);
		Assert.assertEquals(beanInMap.size(), map.size());
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry) it.next();
			String propOut = (String) entry.getKey();
			Object valueOut = entry.getValue();
			Assert.assertTrue(beanInMap.containsKey(propOut));
			Assert.assertEquals(valueOut, beanInMap.get(propOut));
		}
		resetOutput();
	}
	
	public void testMap(){
		Map mapIn = new HashMap();
		mapIn.put("testNumber",new Integer(34));
		mapIn.put("testString","wicked");
		mapIn.put("testBean",new SimpleJavaBean());
		serializer.serialize(out,mapIn);
		
		dumpOutput();
		Map mapOut = (Map) deserializer.deserialize(in);
		Assert.assertNotNull(mapOut);
		Assert.assertEquals(mapIn.size(), mapOut.size());
		
		Set entrySet = mapOut.entrySet();
		Iterator it = entrySet.iterator();
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry) it.next();
			String propOut = (String) entry.getKey();
			Object valueOut = entry.getValue();
			
			Assert.assertTrue(mapIn.containsKey(propOut));
			Object valueIn = mapIn.get(propOut);
			Assert.assertEquals(valueOut, valueIn);
		}
		resetOutput();
		
	}

	public void testSimpleReference(){
		Map mapIn = new HashMap();
		Object bean = new SimpleJavaBean();
		mapIn.put("thebean",bean);
		mapIn.put("thesamebeanagain",bean);
		//mapIn.put("thismap",mapIn);
		serializer.serialize(out,mapIn);
		
		dumpOutput();
		Map mapOut = (Map) deserializer.deserialize(in);
		Assert.assertNotNull(mapOut);
		Assert.assertEquals(mapIn.size(), mapOut.size());
		
		Set entrySet = mapOut.entrySet();
		Iterator it = entrySet.iterator();
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry) it.next();
			String propOut = (String) entry.getKey();
			SimpleJavaBean valueOut = (SimpleJavaBean) entry.getValue();
			
			Assert.assertTrue(mapIn.containsKey(propOut));
			SimpleJavaBean valueIn = (SimpleJavaBean) mapIn.get(propOut);
			Assert.assertEquals(valueOut.getNameOfBean(), valueIn.getNameOfBean());
		}
		resetOutput();
		
	}
	
	public void testCirularReference(){
		CircularRefBean beanIn = new CircularRefBean();
		beanIn.setRefToSelf(beanIn);
		serializer.serialize(out,beanIn);
		
		dumpOutput();
		CircularRefBean beanOut = (CircularRefBean) deserializer.deserialize(in);
		Assert.assertNotNull(beanOut);
		Assert.assertEquals(beanOut,beanOut.getRefToSelf());
		Assert.assertEquals(beanIn.getNameOfBean(),beanOut.getNameOfBean());
		resetOutput();
		
	}
	

}
