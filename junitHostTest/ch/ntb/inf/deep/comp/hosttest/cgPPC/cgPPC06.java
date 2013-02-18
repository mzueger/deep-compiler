package ch.ntb.inf.deep.comp.hosttest.cgPPC;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.ntb.inf.deep.cgPPC.RegAllocator;
import ch.ntb.inf.deep.classItems.CFR;
import ch.ntb.inf.deep.classItems.Class;
import ch.ntb.inf.deep.config.Configuration;
import ch.ntb.inf.deep.strings.HString;

public class cgPPC06 extends TestCgPPC {

	@BeforeClass
	public static void setUp() {
		Configuration.setActiveProject(project);
		project.setActiveTargetConfiguration("BootFromRam");
		HString[] rootClassNames = new HString[] { HString.getHString("ch/ntb/inf/deep/comp/hosttest/testClasses/T06Operators") };
		try {
			CFR.buildSystem(rootClassNames, Configuration.getSearchPaths(), Configuration.getSystemClasses(), attributes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (Class.nofRootClasses > 0) {
			createCgPPC(Class.rootClasses[0]);
		}
	}

//	@Ignore
	@Test
	public void conditionalOperator1() {
		getCode("conditionalOperator1");
		assertTrue("wrong join", checkJoin(getJoin(0), 5, 9, vol, true));
		assertTrue("wrong join", checkJoin(getJoin(0).next, 13, 22, vol, false));
		for (int i = 1; i < RegAllocator.maxNofJoins; i++)
			assertNull("wrong join", getJoin(i));
	}
	
//	@Ignore
	@Test
	public void conditionalOperator2() {
		getCode("conditionalOperator2");
		assertTrue("wrong join", checkJoin(getJoin(0), 9, 13, vol, true));
		assertTrue("wrong join", checkJoin(getJoin(0).next, 17, 26, vol, false));
		for (int i = 1; i < RegAllocator.maxNofJoins; i++)
			assertNull("wrong join", getJoin(i));
	}
	
//	@Ignore
	@Test
	public void conditionalOperator3() {
		getCode("conditionalOperator3");
		assertTrue("wrong join", checkJoin(getJoin(0), 5, 9, vol, false));
		for (int i = 1; i < RegAllocator.maxNofJoins; i++)
			assertNull("wrong join", getJoin(i));
	}

//	@Ignore
	@Test
	public void conditionalOperator4() {
		getCode("conditionalOperator4");
		assertTrue("wrong join", checkJoin(getJoin(0), 4, 8, vol, false));
		for (int i = 1; i < RegAllocator.maxNofJoins; i++)
			assertNull("wrong join", getJoin(i));
	}

}