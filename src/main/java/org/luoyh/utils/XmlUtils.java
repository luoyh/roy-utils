package org.luoyh.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class XmlUtils {

	private static final XmlMapper xmlMapper = new XmlMapper();
	private static final String DEFAULT_ROOT_NAME = "xml";

	public static Map<String, String> readTree(String xml) {
		Map<String, String> map = null;
		JsonNode root = null;
		try {
			root = xmlMapper.readTree(xml);
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

	public static <T> List<T> readList(String xml, TypeReference<List<T>> reference) {
		try {
			return xmlMapper.readValue(xml, reference);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T readObject(String xml, Class<T> clazz) {
		try {
			return xmlMapper.readValue(xml, clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String writeObjectToXml(Object value) {
		return writeObjectToXml(value, DEFAULT_ROOT_NAME);
	}

	public static String writeObjectToXml(Object value, String rootName) {
		try {
			return xmlMapper.writer().withRootName(rootName).writeValueAsString(value);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

}
