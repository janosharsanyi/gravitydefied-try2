package org.happysanta.gd.API;

/**
 * Replacement for {@code org.apache.http.NameValuePair} /
 * {@code BasicNameValuePair}, which were removed from Android in API 28
 * (org.apache.http legacy library). Used as the param type for form-encoded
 * POSTs in {@link Request}.
 */
public class NameValuePair {

	private final String name;
	private final String value;

	public NameValuePair(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

}
