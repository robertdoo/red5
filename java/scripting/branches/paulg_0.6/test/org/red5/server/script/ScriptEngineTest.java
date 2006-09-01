package org.red5.server.script;

import static org.junit.Assert.assertFalse;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.Test;

/**
 * Simple script engine tests. Some of the hello world scripts found here:
 * http://www.roesler-ac.de/wolfram/hello.htm
 * 
 * @author paul.gregoire
 */
public class ScriptEngineTest
{

	// ScriptEngine manager
	private static ScriptEngineManager mgr = new ScriptEngineManager();

	// Javascript
	@Test
	public void testJavascriptHelloWorld()
	{
		ScriptEngine jsEngine = mgr.getEngineByName("rhino");
		try
		{
			jsEngine.eval("print('Javascript - Hello, world!')");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// Ruby
	@Test
	public void testRubyHelloWorld()
	{
		ScriptEngine rbEngine = mgr.getEngineByName("ruby");
		try
		{
			rbEngine.eval("puts 'Ruby - Hello, world!'");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// Python
	@Test
	public void testPythonHelloWorld()
	{
		ScriptEngine pyEngine = mgr.getEngineByName("python");
		try
		{
			pyEngine.eval("print \"Python - Hello, world!\"");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// Groovy
	@Test
	public void testGroovyHelloWorld()
	{
		ScriptEngine gvyEngine = mgr.getEngineByName("groovy");
		try
		{
			gvyEngine.eval("println  \"Groovy - Hello, world!\"");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// Judoscript
	@Test
	public void testJudoscriptHelloWorld()
	{
		ScriptEngine jdEngine = mgr.getEngineByName("judo");
		try
		{
			jdEngine.eval(". \"Judoscript - Hello World\";");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// Haskell
	@Test
	public void testHaskellHelloWorld()
	{
		ScriptEngine hkEngine = mgr.getEngineByName("jaskell");
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append("module Hello where ");
			sb.append("hello::String ");
			sb.append("hello = 'Haskell - Hello World!'");
			hkEngine.eval(sb.toString());
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// Tcl
	@Test
	public void testTclHelloWorld()
	{
		ScriptEngine tEngine = mgr.getEngineByName("tcl");
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append("#!/usr/local/bin/tclsh\n");
			sb.append("puts \"Tcl - Hello World!\"");
			tEngine.eval(sb.toString());
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// Awk
	@Test
	public void testAwkHelloWorld()
	{
		ScriptEngine aEngine = mgr.getEngineByName("awk");
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append("BEGIN { print 'Awk - Hello World!' } END");
			aEngine.eval(sb.toString());
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// E4X
	@Test
	public void testE4XHelloWorld()
	{
		ScriptEngine eEngine = mgr.getEngineByName("rhino");
		try
		{
			eEngine.eval("var d = <d><item>Hello</item><item>World!</item></d>;");
			eEngine.eval("print 'E4X - ' + d..item;");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	// PHP
//	@Test
//	public void testPHPHelloWorld()
//	{
//		//have to add php lib to java env
//		//java.library.path 
//		//System.setProperty("java.library.path", "C:\\PHP;" + System.getProperty("java.library.path"));
//		ScriptEngine pEngine = mgr.getEngineByName("php");
//		try
//		{
//			pEngine.eval("<? echo 'PHP - Hello World'; ?>");
//		}
//		catch (Exception ex)
//		{
//			assertFalse(true);
//			ex.printStackTrace();
//		}
//	}	
	
//	@Test
//	public void testE4X()
//	{
//		// Javascript
//		ScriptEngine jsEngine = mgr.getEngineByName("rhino");
//		try
//		{
//			System.out.println("Engine: " + jsEngine.getClass().getName());
//			jsEngine.eval(new FileReader("samples/E4X/e4x_example.js"));
//		}
//		catch (Exception ex)
//		{
//			assertFalse(true);
//			ex.printStackTrace();
//		}
//	}

	@Test
	public void testRubyApplication()
	{
		ScriptEngine rbEngine = mgr.getEngineByName("ruby");
		try
		{	
			rbEngine.eval(new FileReader("samples/application.rb"));
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}

	@Test
	public void testJavascriptApplication()
	{
		ScriptEngine jsEngine = mgr.getEngineByName("rhino");
		try
		{
			jsEngine.eval(new FileReader("samples/application.js"));
			//jsEngine.eval("var ap = new Application();print(ap.toString());");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}	
	
	@Test
	public void testGroovyApplication()
	{
		ScriptEngine gvyEngine = mgr.getEngineByName("groovy");
		try
		{
			gvyEngine.eval(new FileReader("samples/application.groovy"));
			gvyEngine.eval("def ap = new Application();println ap.toString();");
		}
		catch (Exception ex)
		{
			assertFalse(true);
			ex.printStackTrace();
		}
	}	
	
	@Test
	public void testEngines()
	{
		Map<String, ScriptEngineFactory> engineFactories = new HashMap<String, ScriptEngineFactory>(7);
		ScriptEngineFactory[] factories = mgr.getEngineFactories();
		for (ScriptEngineFactory factory : factories)
		{
			System.out.println("\n--------------------------------------------------------------");
			String engName = factory.getEngineName();
			String engVersion = factory.getEngineVersion();
			String langName = factory.getLanguageName();
			String langVersion = factory.getLanguageVersion();
			System.out.printf("Script Engine: %s (%s) Language: %s (%s)", engName, engVersion, langName, langVersion);
			engineFactories.put(engName, factory);
			String[] engNames = factory.getNames();
			System.out.print("\nEngine Alias(es):");
			for (String name : engNames)
			{
				System.out.printf("%s ", name);
			}
			String[] ext = factory.getExtensions();
			System.out.printf("\nExtension: ");
			for (String name : ext)
			{
				System.out.printf("%s ", name);
			}
		}

	}

}
