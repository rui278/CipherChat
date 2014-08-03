package com.desperate.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utilities {

	public static final long tolerance = 10 * 60 * 1000; // 10 minutes DEBUG
	public static final String charset = "ISO-8859-1";
	public static final String cipherAlgorithm = "AES";
	public static final String cipherMode = "AES/CBC/PKCS5Padding";
	public static final int keySize = 128;

	/**
	 * This class cannot be instantiated.
	 */
	private Utilities() {
	}

	/**
	 * Ciphers a serializable object using {@value #cipherMode}.
	 * 
	 * @param obj
	 *            Object to be serialized
	 * @param key
	 *            Key to be used
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IOException
	 * 
	 * @see #decipherObject(Serializable, SecretKeySpec)
	 */
	public static byte[] cipherObject(Serializable obj, SecretKeySpec key) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException, IOException {

		// Prepare AES cipher
		Cipher aes = Cipher.getInstance(Utilities.cipherMode);
		aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[aes.getBlockSize()]));

		// Prepare streams
		ObjectOutputStream oos;
		ByteArrayOutputStream plainBaos;
		CipherOutputStream cos;
		ByteArrayOutputStream cipherBaos;

		// Write object to the oos -> to the plainBaos
		plainBaos = new ByteArrayOutputStream();
		oos = new ObjectOutputStream(plainBaos);

		oos.writeObject(obj);
		oos.flush();
		oos.close();

		byte[] plainText = plainBaos.toByteArray();

		// Write it to cipherStream, which ciphers it
		cipherBaos = new ByteArrayOutputStream();
		cos = new CipherOutputStream(cipherBaos, aes);

		cos.write(plainText);
		cos.flush();
		cos.close();

		// Get ciphered output
		byte[] cipherText = cipherBaos.toByteArray();

		cipherBaos.flush();
		cipherBaos.close();

		return cipherText;
	}

	/**
	 * Deciphers an array of bytes into an object.
	 * 
	 * @param cipherText
	 * @param key
	 *            Key to be used.
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * 
	 * @see #cipherObject(Serializable, SecretKeySpec)
	 */
	public static Serializable decipherObject(byte[] cipherText, SecretKeySpec key) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException, IOException, ClassNotFoundException {

		// Prepare AES cipher
		Cipher aes = Cipher.getInstance(Utilities.cipherMode);
		aes.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[aes.getBlockSize()]));

		// Prepare streams
		CipherOutputStream cos;
		ByteArrayOutputStream decipherBaos;
		ObjectInputStream ois;
		ByteArrayInputStream plainBais;

		// Write it to cipherStream, which ciphers it
		decipherBaos = new ByteArrayOutputStream();
		cos = new CipherOutputStream(decipherBaos, aes);

		cos.write(cipherText);
		cos.flush();
		decipherBaos.flush();
		cos.close();

		// Get deciphered output
		byte[] plainText = decipherBaos.toByteArray();
		decipherBaos.close();

		// Write bytes to the plainBais -> to the object deserialize stream
		plainBais = new ByteArrayInputStream(plainText);
		ois = new ObjectInputStream(plainBais);

		Serializable obj = (Serializable) ois.readObject();

		return obj;
	}

	public static String keyToString(SecretKeySpec key) {
		try {
			return new String(key.getEncoded(), Utilities.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
