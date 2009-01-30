﻿package org.red5.samples.echo.model 
{
	/**
	 * RED5 Open Source Flash Server - http://www.osflash.org/red5
	 *
	 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
	 *
	 * This library is free software; you can redistribute it and/or modify it under the
	 * terms of the GNU Lesser General Public License as published by the Free Software
	 * Foundation; either version 2.1 of the License, or (at your option) any later
	 * version.
	 *
	 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
	 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
	 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
	 *
	 * You should have received a copy of the GNU Lesser General Public License along
	 * with this library; if not, write to the Free Software Foundation, Inc.,
	 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
	*/
	
	import org.red5.samples.echo.model.tests.*;
	
	/**
	 * Sample data for AMF0 and AMF3 data type tests.
	 * 
	 * @author Joachim Bauch (jojo@struktur.de)
	 * @author Thijs Triemstra (info@collab.nl)
	 */
	public class EchoTestData
	{
		private var _items				: Array;
		private var _amf0count 			: Number;
		
		private var _nullTest			: NullTest = new NullTest();
		private var _undefinedTest		: UndefinedTest = new UndefinedTest();
		private var _booleanTest		: BooleanTest = new BooleanTest();
		private var _stringTest			: StringTest = new StringTest();
		private var _numberTest			: NumberTest = new NumberTest();
		private var _arrayTest			: ArrayTest = new ArrayTest();
		private var _objectTest			: ObjectTest = new ObjectTest();
		private var _dateTest			: DateTest = new DateTest();
		private var _xmlDocumentTest	: XMLDocumentTest = new XMLDocumentTest();
		private var _customClassTest	: CustomClassTest = new CustomClassTest();
		private var _remoteClassTest	: RemoteClassTest = new RemoteClassTest();
		private var _xmlTest			: XMLTest = new XMLTest();
		private var _externalizableTest	: ExternalizableTest = new ExternalizableTest();
		private var _arrayCollectionTest: ArrayCollectionTest = new ArrayCollectionTest();
		private var _objectProxyTest	: ObjectProxyTest = new ObjectProxyTest();
		private var _byteArrayTest		: ByteArrayTest = new ByteArrayTest();
		private var _unsupportedTest	: UnsupportedTest = new UnsupportedTest();
		
		/**
		 * @return Test values.
		 */		
		public function get items(): Array
		{
			return _items;
		}
		
		/**
		 * @return Number of AMF0 tests.
		 */		
		public function get AMF0COUNT(): Number
		{
			return _amf0count;
		}
		
		private function addTest(arr:Array):void 
		{
			for (var s:int=0;s<arr.length;s++) {
				_items.push(arr[s]);
			}
		}
		
		/**
		 * @param tests
		 */				
		public function EchoTestData( tests:Array )
		{
			_items = new Array();
			
			// AMF0 specific tests below
			
			// null
			if ( tests[0].selected ) {
				addTest(_nullTest.tests);
			}
			// undefined
			if ( tests[1].selected ) {
				addTest(_undefinedTest.tests);
			}
			// Boolean
			if ( tests[2].selected ) {
				addTest(_booleanTest.tests);
			}
			// String
			if ( tests[3].selected ) {
				addTest(_stringTest.tests);
			}
			// Number
			if ( tests[4].selected ) {
				addTest(_numberTest.tests);
			}
			// Array
			if ( tests[5].selected ) {
				addTest(_arrayTest.tests);
			}
			// Object
			if ( tests[6].selected ) {
				addTest(_objectTest.tests);
			}
			// Date
			if ( tests[7].selected ) {
				addTest(_dateTest.tests);
			}
			// XML for ActionScript 1.0 and 2.0
			if ( tests[8].selected ) {
				addTest(_xmlDocumentTest.tests);
			}
			// Custom class
			if ( tests[9].selected ) {
				addTest(_customClassTest.tests);
			}
			// Remote class
			if ( tests[10].selected ) {
				addTest(_remoteClassTest.tests);
			}
			
			_amf0count = _items.length;
			
			// AMF3 specific tests below
			
			// XML top-level class for ActionScript 3.0
			if ( tests[11].selected ) {
				addTest(_xmlTest.tests);
			}
			// Externalizable
			if ( tests[12].selected ) {
				addTest(_externalizableTest.tests);
			}
			// ArrayCollection
			if ( tests[13].selected ) {
				addTest(_arrayCollectionTest.tests);
			}
			// ObjectProxy
			if ( tests[14].selected ) {
				addTest(_objectProxyTest.tests);
			}
			// ByteArray
			if ( tests[15].selected ) {
				addTest(_byteArrayTest.tests);
			}
			// Unsupported
			if ( tests[16].selected ) {
				addTest(_unsupportedTest.tests);
			}
		}
		
	}
}