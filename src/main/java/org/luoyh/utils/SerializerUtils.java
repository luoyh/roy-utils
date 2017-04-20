package org.luoyh.utils;

import java.util.concurrent.ConcurrentMap;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.google.common.collect.Maps;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * 
 * @author luoyh(Roy)
 */
@SuppressWarnings("unchecked")
public abstract class SerializerUtils {

	private static ConcurrentMap<Class<?>, Schema<?>> cache = Maps.newConcurrentMap();
	private static Objenesis objenesis = new ObjenesisStd(true);
	private static ThreadLocal<LinkedBuffer> buffer = new ThreadLocal<>();

	private static <T> Schema<T> getSchema(Class<T> cls) {
		Schema<T> schema = (Schema<T>) cache.get(cls);
		if (null == schema) {
			schema = RuntimeSchema.getSchema(cls);
			if (null != schema) {
				cache.putIfAbsent(cls, schema);
			}
		}
		return schema;
	}

	private static LinkedBuffer getBuffer() {
		LinkedBuffer buf = buffer.get();
		if (null == buf) {
			buf = LinkedBuffer.allocate();
			buffer.set(buf);
		} else {
			buf.clear();
		}
		return buf;
	}

	public static <T> byte[] serialize(T value) {
		Schema<T> schema = (Schema<T>) getSchema(value.getClass());
		LinkedBuffer buffer = getBuffer();
		try {
			return ProtostuffIOUtil.toByteArray(value, schema, buffer);
		} finally {
			buffer.clear();
		}
	}

	public static <T> T deserialize(byte[] buffer, Class<T> cls) {
		T t = objenesis.newInstance(cls);
		ProtostuffIOUtil.mergeFrom(buffer, t, getSchema(cls));
		return t;
	}

}
