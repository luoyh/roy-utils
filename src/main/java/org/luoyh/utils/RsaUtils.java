package org.luoyh.utils;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;

/**
 * 
 * @author luoyh(Roy)
 */
public abstract class RsaUtils {

	private static final String KEY_ALGORTHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "MD5withRSA";
	private static final String CHARSET = "UTF-8";

	/**
	 * Signature data use private key with RSA
	 * 
	 * @param data
	 *            The common data
	 * @param privateKey
	 * @return The base64 encode signature data
	 * @throws Exception
	 */
	public static String sign(byte[] data, String privateKey) throws Exception {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		PrivateKey privateKey2 = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(privateKey2);
		signature.update(data);

		return Base64.encodeBase64String(signature.sign());
	}

	/**
	 * Signature data use private key with RSA
	 * 
	 * @param data
	 *            The common data
	 * @param privateKey
	 * @return The base64 encode signature data
	 * @throws Exception
	 */
	public static String sign(String data, String privateKey) throws Exception {
		return sign(data.getBytes(CHARSET), privateKey);
	}

	/**
	 * Verify data use public key with RSA
	 * 
	 * @param data
	 *            The common data
	 * @param publicKey
	 * @param sign
	 *            Verify data, base64 encode too.
	 * @return
	 * @throws Exception
	 */
	public static boolean verify(byte[] data, String sign, String publicKey) throws Exception {
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		PublicKey publicKey2 = keyFactory.generatePublic(x509EncodedKeySpec);

		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initVerify(publicKey2);
		signature.update(data);
		return signature.verify(Base64.decodeBase64(sign));
	}

	/**
	 * Verify data use public key with RSA
	 * 
	 * @param data
	 *            The common data
	 * @param publicKey
	 * @param sign
	 *            Verify data, base64 encode too.
	 * @return
	 * @throws Exception
	 */
	public static boolean verify(String data, String sign, String publicKey) throws Exception {
		return verify(data.getBytes(CHARSET), sign, publicKey);
	}

	/**
	 * Decrypt data use public key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param publicKey
	 * @return Decrypt data
	 * @throws Exception
	 */
	public static byte[] decryptByPublicKey(byte[] data, String publicKey) throws Exception {
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		Key key = keyFactory.generatePublic(x509EncodedKeySpec);

		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	/**
	 * Decrypt data use public key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param publicKey
	 * @return Decrypt data
	 * @throws Exception
	 */
	public static String decryptByPublicKey(String data, String publicKey) throws Exception {
		return new String(decryptByPublicKey(Base64.decodeBase64(data), publicKey), CHARSET);
	}

	/**
	 * Encrypt data use public key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param publicKey
	 * @return The encrypt data
	 * @throws Exception
	 */
	public static byte[] encryptByPublicKey(byte[] data, String publicKey) throws Exception {
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		Key key = keyFactory.generatePublic(x509EncodedKeySpec);

		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(Cipher.ENCRYPT_MODE, key);

		return cipher.doFinal(data);
	}

	/**
	 * Encrypt data use public key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param publicKey
	 * @return The encrypt data
	 * @throws Exception
	 */
	public static String encryptByPublicKey(String data, String publicKey) throws Exception {
		return Base64.encodeBase64String(encryptByPublicKey(data.getBytes(CHARSET), publicKey));
	}

	/**
	 * Decrypt data use private key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param privateKey
	 * @return Decrypt data
	 * @throws Exception
	 */
	public static byte[] decryptByPrivateKey(byte[] data, String privateKey) throws Exception {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		Key key = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(Cipher.DECRYPT_MODE, key);

		return cipher.doFinal(data);
	}

	/**
	 * Decrypt data use private key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param privateKey
	 * @return Decrypt data
	 * @throws Exception
	 */
	public static String decryptByPrivateKey(String data, String privateKey) throws Exception {
		return new String(decryptByPrivateKey(Base64.decodeBase64(data), privateKey), CHARSET);
	}

	/**
	 * Encrypt data use private key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param privateKey
	 * @return Encrypt data
	 * @throws Exception
	 */
	public static byte[] encryptByPrivateKey(byte[] data, String privateKey) throws Exception {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		Key key = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(Cipher.ENCRYPT_MODE, key);

		return cipher.doFinal(data);
	}

	/**
	 * Encrypt data use private key with RSA
	 * 
	 * @param data
	 *            The base64 encode data
	 * @param privateKey
	 * @return Encrypt data
	 * @throws Exception
	 */
	public static String encryptByPrivateKey(String data, String privateKey) throws Exception {
		return Base64.encodeBase64String(encryptByPrivateKey(data.getBytes(CHARSET), privateKey));
	}
}
