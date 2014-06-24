package org.eclipse.concierge;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class VersionRangePerformanceTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		final Version ver = new Version("1.2.43");
		System.out.println("VERSION RANGE TEST");

		boolean compare = false;

		long time = System.nanoTime();
		for (int i = 0; i < 1000000; i++) {
			final VersionRange range = new VersionRange("[1.0.0,2.0.0)");
			compare = range.includes(ver);
		}
		long time1 = System.nanoTime() - time;

		System.out.println("VersionRange: \t\t" + time1);

		boolean compare2 = false;
		time = System.nanoTime();
		for (int i = 0; i < 1000000; i++) {
			compare2 = Utils.isVersionInRange(ver, "[1.0.0,2.0.0)");
		}
		long time2 = System.nanoTime() - time;

		System.out.println("Utils.isVersionInRange: \t\t" + time2);

		if (compare != compare2) {
			throw new IllegalStateException();
		}

		assertTrue(time2 < time1);
	}

}
