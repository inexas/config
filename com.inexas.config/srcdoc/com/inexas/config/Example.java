/*
 * Copyright (C) 2015 Processwide AG. All Rights Reserved. DO NOT ALTER OR
 * REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is provided as-is without warranty of merchantability or fitness for a
 * particular purpose.
 *
 * See http://www.inexas.com/license for license details.
 */

package com.inexas.config;

import com.inexas.tad.Context;

public class Example {

	public static void main(String[] args) {
		final Example example = new Example();
		example.doYourThing();
	}

	private void doYourThing() {
		final Config config = Config.newInstance("datatest/config");
		Context.attach(config);

		theBodyOfYourProgram();

		Context.detach(config);
	}

	private void theBodyOfYourProgram() {
		final Config config = Context.get(Config.class);
		final int version = config.getInt("/MyComponent/version");
		System.out.println("The version is: " + version);
	}

}
