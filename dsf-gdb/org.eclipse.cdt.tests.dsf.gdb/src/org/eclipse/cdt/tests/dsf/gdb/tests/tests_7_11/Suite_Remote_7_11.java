/*******************************************************************************
 * Copyright (c) 2015 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.tests.dsf.gdb.tests.tests_7_11;

import org.eclipse.cdt.tests.dsf.gdb.framework.BaseRemoteSuite;
import org.eclipse.cdt.tests.dsf.gdb.framework.BaseTestCase;
import org.eclipse.cdt.tests.dsf.gdb.tests.ITestConstants;
import org.eclipse.cdt.tests.dsf.gdb.tests.MIBreakpointsTest;
import org.eclipse.cdt.tests.dsf.gdb.tests.MICatchpointsTest;
import org.eclipse.cdt.tests.dsf.gdb.tests.MIRegistersTest;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This class is meant to be empty.  It enables us to define
 * the annotations which list all the different JUnit class we
 * want to run.  When creating a new test class, it should be
 * added to the list below.
 * 
 * This suite is for tests to be run with GDB 7_11
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({
	// We need specific name for the tests of this suite, because of bug https://bugs.eclipse.org/172256
	GDBMultiNonStopRunControlTest_7_11.class,
	GDBRemoteTracepointsTest_7_11.class,
	MIRegistersTest.class,
	MIRunControlTest_7_11.class,
	MIRunControlTargetAvailableTest_7_11.class,
	MIRunControlNonStopTargetAvailableTest_7_11.class,
	MIExpressionsTest_7_11.class,
	MIExpressionsNonStopTest_7_11.class,
	GDBPatternMatchingExpressionsTest_7_11.class,
	MIMemoryTest_7_11.class,
	MIBreakpointsTest.class,
	MICatchpointsTest.class,
	MIDisassemblyTest_7_11.class,
	GDBProcessesTest_7_11.class,
	OperationsWhileTargetIsRunningTest_7_11.class,
	OperationsWhileTargetIsRunningNonStopTest_7_11.class,
	CommandTimeoutTest_7_11.class,
	GDBConsoleBreakpointsTest_7_11.class,
	TraceFileTest_7_11.class,
	GDBConsoleSynchronizingTest_7_11.class,
	StepIntoSelectionTest_7_11.class,
	StepIntoSelectionNonStopTest_7_11.class,
	SourceLookupTest_7_11.class,
	/* Add your test class here */
})

public class Suite_Remote_7_11 extends BaseRemoteSuite {
	@BeforeClass
	public static void beforeClassMethod() {
		BaseTestCase.setGdbProgramNamesLaunchAttributes(ITestConstants.SUFFIX_GDB_7_11);
		BaseTestCase.ignoreIfGDBMissing();
	}
}
