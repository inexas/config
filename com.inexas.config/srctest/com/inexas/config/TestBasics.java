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

import static org.junit.Assert.*;
import org.junit.Test;

public class TestBasics {

	@Test
	public void test() {
		final Config config = Config.newInstance("datatest/config");
		assertEquals("A", config.getString("test.a"));
		assertEquals("overridden", config.getString("test.b"));
		assertEquals(1, config.getInt("test.c"));
		assertNull(config.getString("test.d"));
		assertEquals(Boolean.TRUE, config.getBooleanObject("test.e"));
	}
}
