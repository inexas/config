package com.inexas.config;

import static org.junit.Assert.*;
import java.util.Map;
import org.junit.Test;
import com.inexas.oak.Oak;
import com.inexas.oak.advisory.OakException;

public class TestOakConfig {
	private class Pair {
		public final String name;
		public final Object value;

		public Pair(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return '[' + name + ", " + value + ']';
		}
	}

	private void doTest(String input, Pair... expectedResult) {
		try {
			final Oak oak = new Oak(input);
			final ConfigVisitor visitor = new ConfigVisitor();
			oak.accept(visitor);
			final Map<String, Object> result = visitor.getMap();
			assertEquals(result.size(), expectedResult.length);
			for(final Pair pair : expectedResult) {
				assertTrue(result.containsKey(pair.name));
				final Object value = result.get(pair.name);
				assertEquals(pair.value, value);
			}
		} catch(final OakException e) {
			fail(e.getAdvisory().toString());
		}
	}

	// @Test(expected = InexasRuntimeException.class)
	public void testEmpty() {
		doTest("");
	}

	// @Test(expected = InexasRuntimeException.class)
	public void testArrayAtRoot() {
		doTest("[]");
	}

	@Test
	public void testComments() {
		doTest("config{a:5;} // Ignore me", new Pair("config.a", new Long(5)));
		doTest("config{a: /* Ignore me*/ 5;}", new Pair("config.a", new Long(5)));
		doTest("config{a:5;b// Ignore me\n:6;}",
				new Pair("config.a", new Long(5)),
				new Pair("config.b", new Long(6)));
	}

	@Test
	public void testDataTypes() {
		doTest("config{a:5;}", new Pair("config.a", new Long(5)));
		doTest("config{a:true;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:false;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:\"abc\";}", new Pair("config.a", "abc"));
		doTest("config{a:null;}", new Pair("config.a", null));
	}

	@Test
	public void testExpressionBasics() {
		doTest("config{a:false^false;}", new Pair("config.a", Boolean.FALSE));

		doTest("config{a:+2;}", new Pair("config.a", new Long(2)));
		doTest("config{a:-2;}", new Pair("config.a", new Long(-2)));
		doTest("config{a:~3;}", new Pair("config.a", new Long(~3)));
		doTest("config{a:2*3;}", new Pair("config.a", new Long(6)));
		doTest("config{a:6/3;}", new Pair("config.a", new Long(2)));
		doTest("config{a:7%3;}", new Pair("config.a", new Long(1)));
		doTest("config{a:2+3;}", new Pair("config.a", new Long(5)));
		doTest("config{a:2-3;}", new Pair("config.a", new Long(-1)));

		// Less than
		doTest("config{a:2<3;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:2<2;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:3<2;}", new Pair("config.a", Boolean.FALSE));

		// Less than or equal to
		doTest("config{a:2<=3;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:2<=2;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:3<=2;}", new Pair("config.a", Boolean.FALSE));

		// Equal to
		doTest("config{a:2=3;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:2=2;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:3=2;}", new Pair("config.a", Boolean.FALSE));

		// Not Equal to
		doTest("config{a:2!=3;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:2!=2;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:3!=2;}", new Pair("config.a", Boolean.TRUE));

		// Greater than or equal to
		doTest("config{a:2>=3;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:2>=2;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:3>=2;}", new Pair("config.a", Boolean.TRUE));

		// Greater than
		doTest("config{a:2>3;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:2>2;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:3>2;}", new Pair("config.a", Boolean.TRUE));

		// Bitwise operations...
		doTest("config{a:1|2;}", new Pair("config.a", new Long(3)));
		doTest("config{a:1|1;}", new Pair("config.a", new Long(1)));
		doTest("config{a:0|0;}", new Pair("config.a", new Long(0)));
		doTest("config{a:1&0;}", new Pair("config.a", new Long(0)));
		doTest("config{a:5&1;}", new Pair("config.a", new Long(1)));
		doTest("config{a:1^1;}", new Pair("config.a", new Long(0)));
		doTest("config{a:1^0;}", new Pair("config.a", new Long(1)));

		// Logical operations...
		doTest("config{a:false||false;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:true||false;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:false||true;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:true||true;}", new Pair("config.a", Boolean.TRUE));

		doTest("config{a:false&&false;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:true&&false;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:false&&true;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:true&&true;}", new Pair("config.a", Boolean.TRUE));

		doTest("config{a:false^false;}", new Pair("config.a", Boolean.FALSE));
		doTest("config{a:true^false;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:false^true;}", new Pair("config.a", Boolean.TRUE));
		doTest("config{a:true^true;}", new Pair("config.a", Boolean.FALSE));

		// Conditional operator
		doTest("config{a:true?1:2;}", new Pair("config.a", new Long(1)));
		doTest("config{a:false?1:2;}", new Pair("config.a", new Long(2)));
		doTest("config{a:true?\"a\":\"b\";}", new Pair("config.a", "a"));
		doTest("config{a:false?\"a\":\"b\";}", new Pair("config.a", "b"));

		// Complex...
		doTest("config{a:1+2*3 + (1 + 2) * 3;}", new Pair("config.a", new Long(16)));
	}

	@Test
	public void testMultiple() {
		doTest("config{a:5;b:6;}",
				new Pair("config.a", new Long(5)),
				new Pair("config.b", new Long(6)));
	}

	@Test
	public void testNested() {
		doTest("config{a:5;b{c:6;d:7;}}",
				new Pair("config.a", new Long(5)),
				new Pair("config.b.c", new Long(6)),
				new Pair("config.b.d", new Long(7)));
	}

	@Test
	public void testHex() {
		doTest("config{a:0x10;}", new Pair("config.a", new Long(16)));
	}

	@Test
	public void testBinary() {
		doTest("config{a:0b111;}", new Pair("config.a", new Long(7)));
	}

	@Test
	public void testFloatingPoint() {
		doTest("config{a:1.6;}", new Pair("config.a", new Double("1.6")));
		doTest("config{a:1.645e2;}", new Pair("config.a", new Double("164.5")));
	}

	@Test
	public void testStringEscapes() {
		doTest("config{a:\"A\\u0042C\\nDE\\tF\\\\G\";}", new Pair("config.a", "ABC\nDE\tF\\G"));
		doTest("config{a:\"A\" + \"B\";}", new Pair("config.a", "AB"));
	}

	@Test
	public void testArrays() {
		// todo Arrays need some work but I need a couple of use cases first
	}

}