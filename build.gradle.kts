//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

plugins {
	id("java-library")
	id("sandpolis-java")
	id("sandpolis-module")
	id("sandpolis-protobuf")
	id("sandpolis-publish")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")
	testImplementation("org.awaitility:awaitility:4.0.1")

	// https://github.com/netty/netty
	api("io.netty:netty-common:4.1.60.Final")
	api("io.netty:netty-codec:4.1.60.Final")
	api("io.netty:netty-codec-dns:4.1.60.Final")
	api("io.netty:netty-transport:4.1.60.Final")
	api("io.netty:netty-handler:4.1.65.Final")
	api("io.netty:netty-resolver-dns:4.1.60.Final")

	if (project.getParent() == null) {
		api("com.sandpolis:core.instance:0.1.0")
	} else {
		api(project(":module:com.sandpolis.core.instance"))
	}
}
