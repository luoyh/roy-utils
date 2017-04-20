package org.luoyh.utils;

import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class UuidUtils {
	private static final String MILLISECOND_FORMAT = "yyMMddHHmmssSSS";
	private static final int MAX_RAND_SIZE = 20;

	private static String getRandLongString() {
		String val = String.valueOf(Math.abs(rand()));
		return StringUtils.rightPad(val, MAX_RAND_SIZE, "0");
	}

	private static String millisecond() {
		return DateUtils.getNow(MILLISECOND_FORMAT);
	}

	public static String randSubIndex(int start, int end) {
		if (start >= end)
			throw new IllegalArgumentException("the start must less than end.");
		if (start < 0 || start > MAX_RAND_SIZE)
			throw new IllegalArgumentException("the start index must >= 0 and < " + MAX_RAND_SIZE);
		if (end <= 0 || end > MAX_RAND_SIZE)
			throw new IllegalArgumentException("the end index must <= " + MAX_RAND_SIZE);
		return getRandLongString().substring(start, end);
	}

	public static String randSub() {
		return randSubIndex(2, 8);
	}

	public static String millsRandomFormat() {
		return millisecond() + randSub();
	}

	private static long currentMilliseconds() {
		return System.currentTimeMillis();
	}

	/**
	 * Get default JVM UUID random strings.
	 * 
	 * @return
	 */
	public static String uuidString() {
		return uuid().toString();
	}

	/**
	 * The JVM UUID random string take out '-' to 62 radix.
	 * 
	 * @return
	 */
	public static String uuid62String() {
		UUID uuid = uuid();
		return uuid62String(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
	}

	public static String uuid32() {
		return uuidString().replace("-", "");
	}

	/**
	 * Get current time in milliseconds to 62 radix.
	 * 
	 * @see java.lang.System#currentTimeMillis()
	 * @return
	 */
	public static String millis62String() {
		return to62String(currentMilliseconds());
	}

	/**
	 * <pre>
	 * Get current time in milliseconds + 
	 * the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds 
	 * to 62 radix.
	 * </pre>
	 * 
	 * @see java.lang.System#currentTimeMillis()
	 * @see java.lang.System#nanoTime()
	 * @return
	 */
	public static String millisNano62String() {
		return to62String(currentMilliseconds() + System.nanoTime());
	}

	private static long rand() {
		return new Random().nextLong();
	}

	/**
	 * Get the JVM random string to 62 radix.
	 * 
	 * @see Math#random()
	 * @return
	 */
	public static String random62String() {
		return to62String(rand());
	}

	private static String uuid62String(long mostSigBits, long leastSigBits) {
		return (digits(mostSigBits >> 32, 8) + digits(mostSigBits >> 16, 4) + digits(mostSigBits, 4) + digits(leastSigBits >> 48, 4)
				+ digits(leastSigBits, 12));
	}

	private static String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return to62String(hi | (val & (hi - 1))).substring(1);
	}

	private static String to62String(long i) {
		i = i < 0 ? -i : i;
		char[] buf = new char[65];
		int charPos = 64, radix = 62;
		boolean negative = (i < 0);

		if (!negative) {
			i = -i;
		}

		while (i <= -radix) {
			buf[charPos--] = digits[(int) (-(i % radix))];
			i = i / radix;
		}
		buf[charPos] = digits[(int) (-i)];

		if (negative) {
			buf[--charPos] = '-';
		}

		return new String(buf, charPos, (65 - charPos));
	}

	public static void main(String[] args) {
		long st1 = System.nanoTime();
		System.out.println("UUID ==>> " + uuidString());

		long st2 = System.nanoTime();
		System.out.println(st2 - st1);

		System.out.println("MILL ==>> " + millis62String());

		long st3 = System.nanoTime();
		System.out.println(st3 - st2);

		System.out.println("MANA ==>> " + millisNano62String());

		long st4 = System.nanoTime();
		System.out.println(st4 - st3);

		System.out.println("RAND ==>> " + random62String());

		long st5 = System.nanoTime();
		System.out.println(st5 - st4);

		System.out.println("UU62 ==>> " + uuid62String());

		long st6 = System.nanoTime();
		System.out.println(st6 - st5);

		System.out.println(millisecond());

		System.out.println(rand());

		System.out.println(millsRandomFormat());

		System.out.println("abc".substring(1, 1));
	}

	private static UUID uuid() {
		return UUID.randomUUID();
	}

	private final static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
	// private final static char[] digits = "q02u456s1lXbpOeIL9CokYvnJSarQtjZwxyfA8H3Ec7zgBKDMNhPmVFTUiWdGR".toCharArray();

}
