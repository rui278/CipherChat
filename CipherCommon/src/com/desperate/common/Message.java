package com.desperate.common;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public abstract class Message implements Serializable {

	private static final long serialVersionUID = -7388046942771669624L;

	public Long timestamp;

	public String remoteHMAC;

	/**
	 * Gets a string representative of the whole message, to be hashed with {@link #computeHMAC(String)}. The hash will assure the integrity of this
	 * info, and nothing else.
	 * <p>
	 * This should include the timestamp. It's up to the subclass to implement this correctly.
	 * 
	 * @return String with all info that needs integrity check
	 */
	public abstract String getStringToHMAC();

	/**
	 * Computes the HMAC of this message, as it is. This is implemented in {@link #Message} and is final: subclasses cannot change it. To set what
	 * information should enter the digest, override the {@link #getStringToHMAC()} method.
	 * <p>
	 * To verify integrity, the resulting HMAC should be checked against the {@link #remoteHMAC} received.
	 * 
	 * @param secret
	 *            Secret to be used in the HMAC
	 * @return The HMAC of the message
	 */
	public final String computeHMAC(String secret) {

		String type = "HmacSHA256";

		try {
			SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(Utilities.charset), type);
			Mac mac = Mac.getInstance(type);
			mac.init(secretKey);

			String plainString = getStringToHMAC();

			byte[] bytes = mac.doFinal(plainString.getBytes(Utilities.charset));

			return new String(bytes, Utilities.charset);

		} catch (Exception e) {

			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public boolean verifyTimestamp(Long tolerance) {

		boolean isValid = timestamp > System.currentTimeMillis() - tolerance;

		return isValid;
	}

	/**
	 * Sends the message through the specified stream. The stream is assumed to have been connected to a socket.
	 * 
	 * @param socketStream
	 */
	public void send(ObjectOutputStream socketStream) {

		try {
			socketStream.writeObject(this);

			System.out.println(this.toString());
			
			/*
			if (this.remoteHMAC == null)
				System.out.println("Warning: message sent without HMAC.");*/

		} catch (IOException e) {

			System.out.println("Could not send message.");
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String newLine = System.getProperty("line.separator");

		result.append(this.getClass().getSimpleName());
		result.append(" object {");
		result.append(newLine);

		// determine fields declared in this class only (no fields of superclass)
		Field[] fields = this.getClass().getFields();

		// print field names paired with their values
		for (Field field : fields) {

			if (field.getName().contains("key") || field.getName().contains("password") || field.getName().contains("HMAC"))
				continue;

			result.append("  ");
			try {
				result.append(field.getName());
				result.append(": ");
				// requires access to private field:
				result.append(field.get(this));
			} catch (IllegalAccessException ex) {
			}
			result.append(newLine);
		}
		result.append("}\n");

		return result.toString();
	}

}
