package org.luoyh.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class ResourceUtils {

	private static final ConcurrentMap<String, ResourceBundle> CACHE = Maps.newConcurrentMap();

	private static ResourceBundle initBundle(String fileName) {
		ResourceBundle bundle = CACHE.get(fileName);
		if (null == bundle) {
			bundle = ResourceBundle.getBundle(fileName, Locale.getDefault());
			CACHE.putIfAbsent(fileName, bundle);
		}
		return bundle;
	}

	public static ResourceBundle remove(String key) {
		return CACHE.remove(key);
	}

	public static Map<String, String> resourceToMap(String fileName) {
		ResourceBundle bundle = initBundle(fileName);
		Map<String, String> map = new HashMap<String, String>();
		for (String key : bundle.keySet()) {
			map.put(key, StringUtils.trim(bundle.getString(key)));
		}
		return map;
	}

	public static String get(String fileName, String key) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		ResourceBundle bundle = initBundle(fileName);
		return StringUtils.trim(bundle.getString(key.trim()));
	}

}
