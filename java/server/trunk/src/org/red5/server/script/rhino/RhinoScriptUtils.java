/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.script.rhino;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.PatternSyntaxException;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleNamespace;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for handling Rhino / Javascript objects.
 * 
 * @author Paul Gregoire
 * @since 0.6
 */
public class RhinoScriptUtils {

	// ScriptEngine manager
	private static ScriptEngineManager mgr = new ScriptEngineManager();
	
	/**
	 * Create a new Rhino-scripted object from the given script source.
	 * 
	 * @param scriptSource
	 *            the script source text
	 * @param interfaces
	 *            the interfaces that the scripted Java object is supposed to
	 *            implement
	 * @return the scripted Java object
	 * @throws ScriptCompilationException
	 *             in case of Rhino parsing failure
	 */
	public static Object createRhinoObject(String scriptSource,
			Class[] interfaces, Class extendedClass)
			throws ScriptCompilationException, IOException, Exception {
		//System.out.println("\n" + scriptSource + "\n");
		//JSR223 style
		//ScriptEngineManager mgr = new ScriptEngineManager();
		System.out.println("Script Engine Manager: " + mgr.getClass().getName());
		ScriptEngine engine = mgr.getEngineByName("rhino");
		// set engine scope namespace
		Namespace nameSpace = engine.createNamespace();
		if (null == nameSpace) {
			RhinoScriptFactory.log
					.debug("Engine namespace not created, using simple");
			nameSpace = new SimpleNamespace();
			//set namespace
			RhinoScriptFactory.log.debug("Setting namespace");
			engine.setNamespace(nameSpace, ScriptContext.ENGINE_SCOPE);
		}
		//get the function name ie. class name / ctor
		String funcName = RhinoScriptUtils.getFunctionName(scriptSource);
		RhinoScriptFactory.log.debug("New script function: " + funcName);
		//set the 'filename'
		nameSpace.put(ScriptEngine.FILENAME, funcName);
		// add the logger to the script
		nameSpace.put("log", RhinoScriptFactory.log);
		if (null != extendedClass) {
			RhinoScriptFactory.log
					.debug("Extended: " + extendedClass.getName());
			nameSpace.put("supa", extendedClass.newInstance());
		}
		//compile the script
		CompiledScript script = ((Compilable) engine).compile(scriptSource);
		//eval the script with the associated namespace
		Object o = script.eval(nameSpace);
		RhinoScriptFactory.log.debug("Result of script call: " + o);
		//null result so try constructor
		if (null != o) {
			dump(o);
			//if function name is not null call it
			if (null != funcName) {
				Object attr = engine.getContext().getAttribute(funcName);
				RhinoScriptFactory.log
						.debug("Result of script attribute call: " + attr);
				if (null != attr) {
					dump(attr);
					Context cx = Context.enter();
					try {
						Scriptable scope = cx.initStandardObjects();
						scope.put("className", scope, funcName);
						Function f = (Function) attr;
						Scriptable instance = f.construct(cx, scope,
								new Object[] {});
						if (null != instance) {
							dump(instance);
						}
						o = instance;
					} finally {
						Context.exit();
					}
					RhinoScriptFactory.log
							.debug("Result of script constructor call: " + o);
				} else {
					RhinoScriptFactory.log.debug("Script: "
							+ o.getClass().getName());
					NativeObject no = (NativeObject) o;
					RhinoScriptFactory.log.debug("Native object: "
							+ no.getClassName());
				}
			}
		}
		return Proxy.newProxyInstance(ClassUtils.getDefaultClassLoader(),
				interfaces, new RhinoObjectInvocationHandler(engine, o));
	}

	/**
	 * InvocationHandler that invokes a Rhino script method.
	 */
	private static class RhinoObjectInvocationHandler implements
			InvocationHandler {

		private final ScriptEngine engine;

		private final Object instance;

		public RhinoObjectInvocationHandler(ScriptEngine engine, Object instance) {
			this.engine = engine;
			this.instance = instance;
		}

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			Object o = null;
			//ensure a set of args are available
			if (args == null || args.length == 0) {
				args = new Object[] { "" };
			}
			String name = method.getName();
			RhinoScriptFactory.log.debug("Calling: " + name);
			try {
				if (instance instanceof NativeObject) {
					o = ScriptableObject.callMethod((NativeObject) instance,
							name, args);
					RhinoScriptFactory.log.debug("ScriptableObject result: "
							+ o);
				} else if (null == instance) {
					Invocable invocable = (Invocable) engine;
					o = invocable.call(name, args);
					RhinoScriptFactory.log.debug("Invoke result: " + o);
				} else {
					Invocable invocable = (Invocable) engine;
					o = invocable.call(name, instance, args);
					RhinoScriptFactory.log.debug("Invocable result: " + o);
				}
				//not unwrapping can cause issues...
				if (o instanceof NativeJavaObject) {
					o = ((NativeJavaObject) o).unwrap();
				}
			} catch (NoSuchMethodException nex) {
				RhinoScriptFactory.log.warn("Method not found");
			} catch (Throwable t) {
				RhinoScriptFactory.log.warn(t);
			}
			return o;
		}
	}

	private static void dump(Object c) {
		if (!RhinoScriptFactory.log.isDebugEnabled()) {
			return;
		}
		System.out
				.println("==============================================================================");
		System.out.println("Name: " + c.getClass().getName());
		System.out.println("Result is a function: "
				+ Function.class.isInstance(c) + " compiled script: "
				+ CompiledScript.class.isInstance(c) + " undefined: "
				+ Undefined.class.isInstance(c) + " rhino native: "
				+ NativeObject.class.isInstance(c));
		Method[] methods = c.getClass().getMethods();
		Method m = null;
		//String name = null;
		for (Method element : methods) {
			m = element;
			//name = m.getName();			
			RhinoScriptFactory.log.debug("Method: " + m.toGenericString());
		}
		System.out
				.println("==============================================================================");
	}

	/**
	 * Uses a regex to get the first "function" name, this name
	 * is used to create an instance of the javascript object.
	 * 
	 * @param scriptSource
	 * @return
	 */
	private static String getFunctionName(String scriptSource) {
		String ret = "undefined";
		try {
			ret = scriptSource.replaceAll(
					"[\\S\\w\\s]*?function ([\\w]+)\\(\\)[\\S\\w\\s]+", "$1");
		} catch (PatternSyntaxException ex) {
			// Syntax error in the regular expression
		} catch (IllegalArgumentException ex) {
			// Syntax error in the replacement text (unescaped $ signs?)
		} catch (IndexOutOfBoundsException ex) {
			// Non-existent backreference used the replacement text
		}
		return ret;
	}

}
