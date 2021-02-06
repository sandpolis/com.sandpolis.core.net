//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.util;

import static com.sandpolis.core.net.util.CvidUtil.cvid;
import static com.sandpolis.core.net.util.CvidUtil.extractInstance;
import static com.sandpolis.core.net.util.CvidUtil.extractInstanceFlavor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;

class CvidUtilTest {

	@Test
	@DisplayName("Check for Instance ID overflows")
	void iid_1() {
		for (InstanceType instance : InstanceType.values())
			if (instance != InstanceType.UNRECOGNIZED)
				assertTrue(instance.getNumber() <= (1 << CvidUtil.IID_SPACE) - 1,
						"Maximum ID exceeded: " + instance.getNumber());
	}

	@Test
	@DisplayName("Check for InstanceFlavor ID overflows")
	void fid_1() {
		for (InstanceFlavor flavor : InstanceFlavor.values())
			if (flavor != InstanceFlavor.UNRECOGNIZED)
				assertTrue(flavor.getNumber() <= (1 << CvidUtil.FID_SPACE) - 1,
						"Maximum ID exceeded: " + flavor.getNumber());
	}

	@Test
	@DisplayName("Check a few random CVIDs for validity")
	void cvid_1() {
		for (InstanceType instance : InstanceType.values()) {
			for (InstanceFlavor flavor : InstanceFlavor.values()) {
				if (instance == InstanceType.UNRECOGNIZED || flavor == InstanceFlavor.UNRECOGNIZED)
					continue;

				for (int i = 0; i < 1000; i++) {
					int cvid = cvid(instance, flavor);
					assertTrue(0 < cvid, "Invalid CVID: " + cvid);
					assertEquals(instance, extractInstance(cvid));
					assertEquals(flavor, extractInstanceFlavor(cvid));
				}
			}
		}
	}

}
