package org.luoyh.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class JsonUtils {

	public static void main(String[] args) {
	}

	private static final ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
	}

	public static <T> T readObject(String json, Class<T> t) {
		if (StringUtils.isBlank(json))
			return null;
		try {
			return mapper.readValue(json, t);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T> List<T> readList(String jsonStr, TypeReference<List<T>> reference) {
		if (StringUtils.isBlank(jsonStr))
			return null;
		try {
			return mapper.readValue(jsonStr, reference);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Map<String, String> readTree(String json) {
		if (StringUtils.isBlank(json))
			return null;
		Map<String, String> map = null;
		JsonNode root = null;
		try {
			root = mapper.readTree(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (root != null) {
			map = new HashMap<String, String>();
			for (Iterator<String> it = root.fieldNames(); it.hasNext();) {
				String field = it.next();
				JsonNode node = root.get(field);
				JsonNodeType type = node.getNodeType();
				if (type == JsonNodeType.NULL) {
					map.put(field, null);
				} else {
					map.put(field, node.asText());
				}
			}
		}
		return map;
	}

	public static void writeObj(OutputStream out, Object obj) {
		try {
			mapper.writeValue(out, obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String toJson(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] toByte(Object obj) {
		try {
			return mapper.writeValueAsBytes(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
