package com.inexas.config;

import java.io.File;
import java.math.*;
import java.util.*;
import java.util.Map.Entry;
import com.inexas.exception.UnexpectedException;
import com.inexas.oak.Oak;
import com.inexas.oak.advisory.OakException;
import com.inexas.oak.ast.Node;
import com.inexas.tad.Tad;
import com.inexas.util.FileU;

/**
 * Config reads in a number of configuration files written in Oak and can be
 * queried to get values from them.
 */
public class Config implements Tad {
	/**
	 * This class is thrown if the config files has a null for a value and a
	 * caller tries to convert it to a Java primitive, e.g. an int.
	 */
	public static class NullValueException extends RuntimeException {
		private static final long serialVersionUID = -7248210736043642035L;

		public NullValueException(String path) {
			super("Null cannot be converted to a Java primitive type: '" + path + '\'');
		}
	}

	public static class NoSuchPathException extends RuntimeException {
		private static final long serialVersionUID = 5182463621882188013L;

		public NoSuchPathException(String path) {
			super("No such path: '" + path + '\'');
		}
	}

	/**
	 * This is thrown if an attempt is made to convert a number in config to a
	 * data type that cannot properly represent it. For example if a minimum or
	 * maximum value is exceeded, or trying to convert 1.1 to an integer (the
	 * decimal part would be lost).
	 */
	public static class OverflowException extends RuntimeException {
		private static final long serialVersionUID = -4962691443844037348L;

		public OverflowException(String path, Number n, String expectedClass) {
			super("Overflow exception, "
					+ n.toString() + " cannot be converted to: " + expectedClass
					+ ". Path: " + path);
		}
	}

	public static class TypeMismatchException extends RuntimeException {
		private static final long serialVersionUID = 5182463621882188013L;

		public TypeMismatchException(String path, Class<? extends Object> got, Class<?> expected) {
			super("Class mismatch for Config path: '" + path +
					"' got " + got.getCanonicalName() +
					" expected " + expected.getCanonicalName());
		}
	}

	private final static BigInteger BigInteger_MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
	private final static BigInteger BigInteger_MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
	private final static BigDecimal BigDecimal_MIN_INT = new BigDecimal(String.valueOf(Integer.MIN_VALUE));
	private final static BigDecimal BigDecimal_MAX_INT = new BigDecimal(String.valueOf(Integer.MAX_VALUE));

	private final static BigInteger BigInteger_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
	private final static BigInteger BigInteger_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
	private final static BigDecimal BigDecimal_MIN_LONG = new BigDecimal(String.valueOf(Long.MIN_VALUE));
	private final static BigDecimal BigDecimal_MAX_LONG = new BigDecimal(String.valueOf(Long.MAX_VALUE));

	private final static int TYPE_integer = 0;
	private final static int TYPE_INTEGER = 1;
	private final static int TYPE_decimal = 2;
	private final static int TYPE_DECIMAL = 3;
	private final Map<String, Object> map = new HashMap<>();

	/**
	 * Load the config given a path. The path may be either absolute (starts
	 * with a '/' or a contains a ':' or relative.
	 *
	 * @param configPath
	 *            the path to load from.
	 */
	public static Config newInstance(String configPath) {
		return load(configPath);
	}

	/**
	 * Load the configuration from the default path of "config/"
	 *
	 * @return
	 */
	public static Config newInstance() {
		return load("config/");
	}

	// Constructor...

	private Config() {
		// Quick! Hide!!
	}

	/**
	 * Set the value associated with a path.
	 *
	 * @param path
	 *            The path, e.g. "/MyConfig/Database/password".
	 * @param value
	 *            The value to associate with the path. Use the correct Object
	 *            type that will be compatible with the getXxxx() methods you
	 *            intend to use.
	 * @deprecated This method is deprecated because in most situations config
	 *             should be immutable. However, for changing or setting values
	 *             might be useful.
	 */
	@Deprecated
	public void set(String path, Object value) {
		map.put(path, value);
	}

	/**
	 * Return the String value associated with the given path. The associated
	 * value may have any data type.
	 *
	 * @param path
	 *            The path, e.g. "/MyConfig/Database/password".
	 * @return The value associated with the path. If the value is null, null is
	 *         returned otherwise the result of toString() is returned.
	 * @throws NoSuchPathException
	 *             The config files do not contain a mapping for the give path.
	 */
	public String getString(String path) throws NoSuchPathException {
		final Object value = get(path, null);
		return value == null ? null : value.toString();
	}

	/**
	 * Return the int value associated with the given path. Not that Config
	 * stores numbers according to the Oak data types: integer (like Java's
	 * Long), decimal (like Java's double), INTEGER (like Java's BigInteger),
	 * and DECIMAL (like Java's BigDecimal). An internal conversion is done
	 * which may result in an overflow and an exception being thrown.
	 *
	 * @param path
	 *            The path, e.g. "/MyConfig/Database/password".
	 * @return The value associated with the path.
	 * @return The value associated with the path. If the value is null, null is
	 *         returned otherwise the result of toString() is returned.
	 * @throws NoSuchPathException
	 *             The config files do not contain a mapping for the give path.
	 * @throws NullValueException
	 *             Thrown if the config files contain a null value for the given
	 *             path.
	 * @throws OverflowException
	 *             Thrown if the value in the configuration file cannot be
	 *             properly converted to int.
	 */
	public int getInt(String path)
			throws NoSuchPathException, TypeMismatchException, NullValueException, OverflowException {
		final int result;

		final Number number = (Number)get(path, Number.class);
		if(number == null) {
			throw new NullValueException(path);
		}

		final int type = getOakTypeAsInt(number.getClass());
		switch(type) {
		case TYPE_integer: {
			final long l = ((Long)number).longValue();
			if(l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
				throw new OverflowException(path, number, "int");
			}
			result = (int)l;
			break;
		}

		case TYPE_INTEGER: {
			final BigInteger bi = (BigInteger)number;
			if(bi.compareTo(BigInteger_MIN_INT) < 0 || bi.compareTo(BigInteger_MAX_INT) > 0) {
				throw new OverflowException(path, number, "int");
			}
			result = bi.intValue();
			break;
		}

		case TYPE_decimal: {
			final double d = number.doubleValue();
			if(d % 1 != 0 || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
				throw new OverflowException(path, number, "int");
			}
			result = number.intValue();
			break;
		}

		case TYPE_DECIMAL: {
			final BigDecimal bd = (BigDecimal)number;
			if(bd.compareTo(BigDecimal_MIN_INT) < 0 || bd.compareTo(BigDecimal_MAX_INT) > 0) {
				throw new OverflowException(path, number, "int");
			}
			result = number.intValue();
			break;
		}

		default:
			throw new UnexpectedException("Value: " + type);
		}

		return result;
	}

	/**
	 * Return the Integer value associated with the given path. Not that Config
	 * stores numbers according to the Oak data types: integer (like Java's
	 * Long), decimal (like Java's double), INTEGER (like Java's BigInteger),
	 * and DECIMAL (like Java's BigDecimal). An internal conversion is done
	 * which may result in an overflow and an exception being thrown.
	 *
	 * @param path
	 *            The path, e.g. "/MyConfig/Database/password".
	 * @return The value associated with the path.
	 * @return The value associated with the path. If the value is null, null is
	 *         returned otherwise the result of toString() is returned.
	 * @throws NoSuchPathException
	 *             The config files do not contain a mapping for the give path.
	 * @throws OverflowException
	 *             Thrown if the value in the configuration file cannot be
	 *             properly converted to int.
	 */
	public Integer getInteger(String path)
			throws NoSuchPathException, TypeMismatchException, OverflowException {
		try {
			return new Integer(getInt(path));
		} catch(final NullValueException e) {
			return null;
		}
	}

	/**
	 * Return the long value associated with the given path. Not that Config
	 * stores numbers according to the Oak data types: integer (like Java's
	 * Long), decimal (like Java's double), INTEGER (like Java's BigInteger),
	 * and DECIMAL (like Java's BigDecimal). An internal conversion is done
	 * which may result in an overflow and an exception being thrown.
	 *
	 * @param path
	 *            The path, e.g. "/MyConfig/Database/password".
	 * @return The value associated with the path.
	 * @return The value associated with the path. If the value is null, null is
	 *         returned otherwise the result of toString() is returned.
	 * @throws NoSuchPathException
	 *             The config files do not contain a mapping for the give path.
	 * @throws NullValueException
	 *             Thrown if the config files contain a null value for the given
	 *             path.
	 * @throws OverflowException
	 *             Thrown if the value in the configuration file cannot be
	 *             properly converted to long.
	 */
	public long getLong(String path)
			throws NoSuchPathException, TypeMismatchException, NullValueException, OverflowException {
		final long result;

		final Number number = (Number)get(path, Number.class);
		if(number == null) {
			throw new NullValueException(path);
		}

		final int type = getOakTypeAsInt(number.getClass());
		switch(type) {
		case TYPE_integer: {
			result = ((Long)number).longValue();
			break;
		}

		case TYPE_INTEGER: {
			final BigInteger bi = (BigInteger)number;
			if(bi.compareTo(BigInteger_MIN_LONG) < 0 || bi.compareTo(BigInteger_MAX_LONG) > 0) {
				throw new OverflowException(path, number, "long");
			}
			result = bi.longValue();
			break;
		}

		case TYPE_decimal: {
			final double d = number.doubleValue();
			if(d % 1 != 0 || d < Long.MIN_VALUE || d > Long.MAX_VALUE) {
				throw new OverflowException(path, number, "long");
			}
			result = number.longValue();
			break;
		}

		case TYPE_DECIMAL: {
			final BigDecimal bd = (BigDecimal)number;
			if(bd.compareTo(BigDecimal_MIN_LONG) < 0 || bd.compareTo(BigDecimal_MAX_LONG) > 0) {
				throw new OverflowException(path, number, "long");
			}
			result = number.longValue();
			break;
		}

		default:
			throw new UnexpectedException("Value: " + type);
		}

		return result;
	}

	/**
	 * Return the Long value associated with the given path. Not that Config
	 * stores numbers according to the Oak data types: integer (like Java's
	 * Long), decimal (like Java's double), INTEGER (like Java's BigInteger),
	 * and DECIMAL (like Java's BigDecimal). An internal conversion is done
	 * which may result in an overflow and an exception being thrown.
	 *
	 * @param path
	 *            The path, e.g. "/MyConfig/Database/password".
	 * @return The value associated with the path.
	 * @return The value associated with the path. If the value is null, null is
	 *         returned otherwise the result of toString() is returned.
	 * @throws NoSuchPathException
	 *             The config files do not contain a mapping for the give path.
	 * @throws OverflowException
	 *             Thrown if the value in the configuration file cannot be
	 *             properly converted to int.
	 */
	public Long getLongObject(String path)
			throws NoSuchPathException, TypeMismatchException, OverflowException {
		try {
			return new Long(getLong(path));
		} catch(final NullValueException e) {
			return null;
		}
	}

	/**
	 * Read a boolean value given the path.
	 *
	 * @param path
	 *            The path in the config file.
	 * @return The boolean value corresponding to the path.
	 * @throws NoSuchPathException
	 *             Thrown if there is no such path in the config files.
	 * @throws TypeMismatchException
	 *             Thrown if the data type in the config file cannot be
	 *             converted to a boolean.
	 * @throws NullValueException
	 *             Thrown if the value in the config is null - null cannot be
	 *             converted to boolean, perhaps use getBooleanObject() instead?
	 */
	public boolean getBoolean(String path)
			throws NoSuchPathException, TypeMismatchException, NullValueException {
		final Boolean result = (Boolean)get(path, Boolean.class);
		if(result == null) {
			throw new NullValueException(path);
		}
		return result.booleanValue();
	}

	/**
	 * Read a Boolean value given the path.
	 *
	 * @param path
	 *            The path in the config file.
	 * @return The Boolean value corresponding to the path, may be null.
	 * @throws NoSuchPathException
	 *             Thrown if there is no such path in the config files.
	 * @throws TypeMismatchException
	 *             Thrown if the data type in the config file cannot be
	 *             converted to a boolean.
	 */
	public Boolean getBooleanObject(String path) throws NoSuchPathException, TypeMismatchException {
		return (Boolean)get(path, Boolean.class);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for(final Entry<String, Object> entry : map.entrySet()) {
			sb.append(entry.getKey());
			sb.append(": ");
			final Object value = entry.getValue();
			sb.append(value == null ? "<null>" : value.toString());
			sb.append('\n');
		}
		return sb.toString();
	}

	private static Config load(String path) {
		assert path != null;

		final Config result = new Config();

		// !todo This doesn't work on Windows...
		final boolean absolute = path.startsWith("/");
		final File directory = absolute ? new File(path) : FileU.getHome(path);
		assert directory.isDirectory();

		load(directory, "[^\\.]*\\.base", result);
		load(directory, "[^\\.]*\\.config", result);

		return result;
	}

	private static void load(File directory, String pattern, Config result) {
		final File[] files = FileU.getChildren(directory, FileU.Type.FILE, pattern);
		for(int i = 0; i < files.length; i++) {
			// Read in each file...
			final File file = files[i];

			try {
				final Oak oak = new Oak(file);
				final Node root = oak.toAst();
				final ConfigVisitor visitor = new ConfigVisitor();
				root.accept(visitor);
				result.map.putAll(visitor.getMap());
			} catch(final OakException e) {
				throw new RuntimeException("Error loading: " + file.getName(), e);
			}
		}
	}

	private Object get(String path, Class<?> expectedClass)
			throws NoSuchPathException, TypeMismatchException {

		// Make sure there's a matching path...
		if(!map.containsKey(path)) {
			throw new NoSuchPathException(path);
		}

		final Object result = map.get(path);

		if(expectedClass != null && result != null) {
			// Get it and check the class is as expected...
			if(!expectedClass.isAssignableFrom(result.getClass())) {
				throw new TypeMismatchException(path, result.getClass(), expectedClass);
			}
		}
		return result;
	}

	private int getOakTypeAsInt(Class<? extends Object> clazz) {
		final int result;

		if(clazz == Long.class) {
			result = TYPE_integer;
		} else if(clazz == BigInteger.class) {
			result = TYPE_INTEGER;
		} else if(clazz == Double.class) {
			result = TYPE_decimal;
		} else if(clazz == BigDecimal.class) {
			result = TYPE_DECIMAL;
		} else {
			throw new UnexpectedException("Type: " + clazz.getName());
		}

		return result;
	}

}
