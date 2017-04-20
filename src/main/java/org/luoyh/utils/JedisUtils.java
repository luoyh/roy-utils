package org.luoyh.utils;

import redis.clients.jedis.Jedis;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class JedisUtils {

	public static <T> void set(Jedis jedis, String key, T value, Class<T> clazz) {
		jedis.set(SerializerUtils.serialize(key), SerializerUtils.serialize(value));
	}

	public static byte[] get(Jedis jedis, String key) {
		return jedis.get(SerializerUtils.serialize(key));
	}

	public static <T> T get(Jedis jedis, String key, Class<T> clazz) {
		return SerializerUtils.deserialize(jedis.get(SerializerUtils.serialize(key)), clazz);
	}

}
