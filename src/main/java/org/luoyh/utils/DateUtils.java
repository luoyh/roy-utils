package org.luoyh.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class DateUtils {

	/** 时间格式: yyyy-MM-dd HH:mm:ss */
	public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	/** 时间格式: yyyy-MM-dd */
	public static final String YYYY_MM_DD = "yyyy-MM-dd";
	public static final String YYYYMMDD = "yyyyMMdd";
	/** 时间格式: yyyy-MM */
	public static final String YYYY_MM = "yyyy-MM";

	private static final ConcurrentMap<String, SimpleDateFormat> CACHE = Maps.newConcurrentMap();
	private static final SimpleDateFormat BASE = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);

	public static void main(String[] args) {
	}

	static {
		// 获取东八区时间
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		TimeZone.setDefault(tz);

		CACHE.put(YYYY_MM_DD_HH_MM_SS, BASE);
	}

	/**
	 * Get now Date.
	 * 
	 * @return
	 */
	public static Date now() {
		return new Date();
	}

	public static long getTime() {
		return now().getTime();
	}

	/**
	 * 获取当前日期<br>
	 * 
	 * <pre>
	 * 格式： yyyy-MM-dd HH:mm:ss
	 * </pre>
	 * 
	 * @param format
	 *            自定义日期格式
	 * @return 字符串格式
	 */
	public static String getNow(String format) {
		return getNowFormat(format);
	}

	public static String getNow() {
		return getNowFormat(null);
	}

	/**
	 * 获取以days为偏移量之后的日期<br>
	 * 
	 * @param days
	 *            天数
	 * @return days天过后的日期
	 */
	public static Date getDaysLater(int days) {
		// long c = getTime();
		// c += days * 24 * 60 * 60 * 1000L;
		// return new Date(c);

		// 把当前日期的天数减days天
		// 获取日历对象
		Calendar c = Calendar.getInstance();
		// 把日期设置到日历对象中
		c.setTime(now());
		// 设置天数偏移量
		c.add(Calendar.DAY_OF_YEAR, days);

		return c.getTime();
	}

	public static Date getDateLater(Date date, int field, int amount) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(field, amount);
		return c.getTime();
	}

	public static Date getDateLater(int field, int amount) {
		return getDateLater(now(), field, amount);
	}

	public static Date getMinutesLater(int minutes) {
		Calendar c = Calendar.getInstance();
		c.setTime(now());
		c.add(Calendar.MINUTE, minutes);
		return c.getTime();
	}

	/**
	 * 获取指定2个日期相差的天数。 包含结束那一天。 比如开始时间和结束时间都是同一天，那么返回1<br>
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static int getBetweenDays(Date start, Date end) {
		Calendar _s = Calendar.getInstance();
		_s.setTime(start);
		Calendar s = Calendar.getInstance();
		s.set(_s.get(Calendar.YEAR), _s.get(Calendar.MONTH), _s.get(Calendar.DAY_OF_MONTH));

		Calendar _e = Calendar.getInstance();
		_e.setTime(end);
		Calendar e = Calendar.getInstance();
		e.set(_e.get(Calendar.YEAR), _e.get(Calendar.MONTH), _e.get(Calendar.DAY_OF_MONTH));
		e.add(Calendar.DAY_OF_YEAR, 1);

		return (int) ((e.getTimeInMillis() - s.getTimeInMillis()) / 3600000 / 24);
	}

	/**
	 * 获取当前时间所在周的开始日期<br>
	 * 
	 * @param date
	 * @return
	 */
	public static String getFirstDayOfWeek() {
		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTime(now());
		c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek()); // Monday
		return getSdf(YYYY_MM_DD).format(c.getTime());
	}

	/**
	 * 获取当前时间所在周的结束日期<br>
	 * 
	 * @param date
	 * @return
	 */
	public static String getLastDayOfWeek() {
		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTime(now());
		c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek() + 6); // Sunday
		return getSdf(YYYY_MM_DD).format(c.getTime());
	}

	/**
	 * 获得当前时间所在月份的前一月的第一天和最后一天所在日期<br>
	 * 
	 */
	public static String[] getLastMonth() {
		// 获取日历对象
		Calendar cal = Calendar.getInstance();
		// 把日期设置到日历对象中
		cal.setTime(now());
		int year = 0;
		int month = cal.get(Calendar.MONTH); // 上个月月份
		// 设置年月
		if (month == 0) {
			year = cal.get(Calendar.YEAR) - 1;
			month = 12;
		} else {
			year = cal.get(Calendar.YEAR);
		}
		// 设置天数
		String temp = year + "-" + month;
		Date date = null;
		try {
			date = getSdf(YYYY_MM).parse(temp);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		cal.setTime(date);
		int startDay = cal.getActualMinimum(Calendar.DAY_OF_MONTH); // 起始天数
		int endDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH); // 结束天数

		String[] result = { year + "-" + month + "-" + startDay, year + "-" + month + "-" + endDay };
		return result;
	}

	/**
	 * 获取当月的第一天日期<br>
	 */
	public static String getCurrentMonthFirstDay() {

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, 0);
		// 设置为1号,当前日期既为本月第一天
		cal.set(Calendar.DAY_OF_MONTH, 1);

		return getSdf(YYYY_MM_DD).format(cal.getTime());
	}

	/**
	 * 获取当月的最后一天日期<br>
	 */
	public static String getCurrentMonthLastDay() {

		Calendar cal = Calendar.getInstance();
		// 设置为最大号,当前日期既为本月最后一天
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		return getSdf(YYYY_MM_DD).format(cal.getTime());
	}

	/**
	 * 获得当前时间所在年份的前一年的第一天所在日期<br>
	 * 
	 */
	public static String getLastYearOfFirstDay() {
		// 获取日历对象
		Calendar cal = Calendar.getInstance();
		// 把日期设置到日历对象中
		cal.setTime(now());
		// 获取年份
		int year = cal.get(Calendar.YEAR) - 1;
		String result = year + "-01-01";
		return result;
	}

	/**
	 * 获得当前时间所在年份的前一年的最后一天所在日期<br>
	 * 
	 */
	public static String getLastYearOfLastDay() {
		// 获取日历对象
		Calendar cal = Calendar.getInstance();
		// 把日期设置到日历对象中
		cal.setTime(now());
		// 获取年份
		int year = cal.get(Calendar.YEAR) - 1;
		String result = year + "-12-31";
		return result;
	}

	/**
	 * 根据格式化字符串格式化当前时间
	 * 
	 * @param format
	 *            格式化时间字符串 如："yyyy-MM-dd HH:mm:ss"
	 * @return
	 */
	public static String getNowFormat(String format) {
		return date2String(now(), format);
	}

	/**
	 * 字符串格式的日期解析成日期
	 * 
	 * @param source
	 *            字符串格式日期
	 * @param format
	 *            格式化字符串
	 * @return 解析后的日期
	 */
	public static Date string2Date(String source, String format) {
		try {
			return getSdf(format).parse(source);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Date string2Date(String source) {
		try {
			return getSdf(null).parse(source);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 根据毫秒数格式化日期<br>
	 * 
	 * @param longs
	 *            毫秒数字符串, 为空则返回空字符串
	 * @param format
	 *            格式化字符串，null则格式化为：yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	public static String longs2Format(String longs, String format) {
		return StringUtils.isBlank(longs) ? null : getSdf(format).format(new Date(Long.parseLong(longs)));
	}

	private static SimpleDateFormat getSdf(String format) {
		if (StringUtils.isBlank(format)) {
			return BASE;
		}
		SimpleDateFormat ret = null;
		if (null == (ret = CACHE.get(format))) {
			ret = new SimpleDateFormat(format);
			CACHE.putIfAbsent(format, ret);
		}
		return ret;
	}

	/**
	 * 根据格式化字符串得到指定日期的格式字符串<br>
	 * 
	 * @param date
	 * @param format
	 * @return
	 */
	public static String date2String(Date date, String format) {
		return getSdf(format).format(date);
	}

	public static String date2String(Date date) {
		return getSdf(null).format(date);
	}

	/**
	 * 根据参数日期和现在日期的差，算出两个日期差的分钟数<br>
	 * 
	 * @param date
	 *            旧日期
	 * @return 间隔分钟数
	 */
	public static int getMinuteByDateDiff(Date date) {

		if (date == null) {
			return 0;
		}
		// 每一分钟多少毫秒
		int minute = 1000 * 60;
		// 传入日期时间
		BigDecimal otime = new BigDecimal(date.getTime());
		// 系统当前时间
		BigDecimal ntime = new BigDecimal(getTime());
		// (ntime - otime) / minute[相除取整，小数位为0，小数位直接舍去（不四舍五入）]
		int result = ntime.subtract(otime).divide(new BigDecimal(minute), 0, RoundingMode.DOWN).intValue();

		return result;
	}

	public static int getField(int field) {
		return getField(now(), field);
	}

	public static int getField(Date date, int field) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c.get(field);
	}

	/**
	 * 获取指定日期的年份*1000+日期的天数（在这一年中的天数）<br>
	 * 
	 * @param date
	 * @return
	 */
	public static int getDateDays(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
	}

	/**
	 * 获取指定的两个日期之间所有的日期<br>
	 * 
	 * @param st
	 *            开始日期
	 * @param et
	 *            结束日期
	 * @param format
	 *            日期格式
	 * @return
	 */
	public static List<String> getDateBetweenDate(Date st, Date et, String format) {
		List<String> dates = new ArrayList<String>();
		int days = DateUtils.getBetweenDays(st, et);
		for (int i = 0; i < days; i++) {
			long laterDate = st.getTime() + i * 24 * 60 * 60 * 1000;
			dates.add(DateUtils.longs2Format(laterDate + "", format));
		}
		return dates;
	}

	public static String getDayLaterDate(Date begin, int amount) {
		Calendar c = Calendar.getInstance();
		// 把日期设置到日历对象中
		c.setTime(begin);
		// 设置天数偏移量
		c.add(Calendar.DAY_OF_YEAR, amount);
		return String.format("%d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * 获取当前月的上一个月</br>
	 * 
	 * @return
	 */
	public static String getLastMouth() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, -1);
		SimpleDateFormat sdf = getSdf("yyyy-MM");
		return sdf.format(cal.getTime());
	}

	/**
	 * 获取当前时间是今年的第几周<br>
	 * 
	 * @return
	 */
	public static int getNowWeek() {
		Calendar cal = Calendar.getInstance();
		// 设置在一年中第一个星期所需最少天数
		cal.setMinimalDaysInFirstWeek(7);
		int weeks = cal.get(Calendar.WEEK_OF_YEAR);
		return weeks;
	}

	/**
	 * 获取给定时间所在年的周数<br>
	 * 
	 * @param date
	 * @return
	 */
	public static int getWeekOfYear(String date) {
		Calendar cal = Calendar.getInstance();
		// 设置一周的第一天
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		// 设置在一年中第一个星期所需最少天数
		cal.setMinimalDaysInFirstWeek(7);
		cal.setTime(string2Date(date, YYYY_MM_DD));
		return cal.get(Calendar.WEEK_OF_YEAR);
	}

	/**
	 * 获取某月的最后一天 
	 */
	public static String getLastDayOfMonth(int year, int month) {
		Calendar cal = Calendar.getInstance();
		// 设置年份
		cal.set(Calendar.YEAR, year);
		// 设置月份
		cal.set(Calendar.MONTH, month - 1);
		// 获取某月最大天数
		int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		// 设置日历中月份的最大天数
		cal.set(Calendar.DAY_OF_MONTH, lastDay);
		// 格式化日期
		SimpleDateFormat sdf = getSdf("yyyy-MM-dd");
		String lastDayOfMonth = sdf.format(cal.getTime());

		return lastDayOfMonth;
	}

}
