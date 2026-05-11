package org.happysanta.gd.API;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import static org.happysanta.gd.Helpers.getAppVersion;
import static org.happysanta.gd.Helpers.logDebug;

public class Request {

	// enum Method { GET, POST };

	private List<NameValuePair> params;
	private ResponseHandler handler;
	private AsyncRequestTask task;
	private String apiURL;

	public Request(String method, List<NameValuePair> params, ResponseHandler handler) {
		this.apiURL = API.URL;

		params.add(new NameValuePair("method", method));
		params.add(new NameValuePair("app_version", getAppVersion()));
		params.add(new NameValuePair("app_lang", Locale.getDefault().getDisplayLanguage()));

		this.params = params;
		this.handler = handler;

		go();
	}

	private void go() {
		task = new AsyncRequestTask();
		task.execute(apiURL);
	}

	public void cancel() {
		if (task != null) {
			task.cancel(true);
			task = null;
		}
	}

	private void onDone(String result) {
		Response response;
		logDebug("API.Request.onDone()");

		try {
			response = new Response(result);
		} catch (APIException e) {
			handler.onError(e);
			return;
		} catch (Exception e) {
			// e.printStackTrace();
			handler.onError(new APIException(result == null ? "Network error" : "JSON parsing error"));
			return;
			// exception = new Exception();
		}

		// handler.onResponse(response);

		if (response != null)
			handler.onResponse(response);
		else
			handler.onError(new APIException("JSON parsing error"));
	}

	protected class AsyncRequestTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... objects) {
			String url = objects[0];

			// Build URL-encoded form body. Replaces Apache's UrlEncodedFormEntity.
			byte[] bodyBytes;
			try {
				StringBuilder body = new StringBuilder();
				for (NameValuePair pair : params) {
					if (body.length() > 0) body.append('&');
					body.append(URLEncoder.encode(pair.getName(), "UTF-8"));
					body.append('=');
					body.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
				}
				bodyBytes = body.toString().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}

			HttpURLConnection conn = null;
			InputStream is = null;
			try {
				conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
				conn.setConnectTimeout(15_000);
				conn.setReadTimeout(30_000);

				OutputStream os = conn.getOutputStream();
				try {
					os.write(bodyBytes);
					os.flush();
				} finally {
					os.close();
				}

				is = conn.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					if (isCancelled()) break;
					sb.append(line).append('\n');
				}
				return sb.toString();
			} catch (Exception e) {
				logDebug("API request failed: " + e.getMessage());
				// e.printStackTrace();
				return null;
			} finally {
				try {
					if (is != null) is.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
				if (conn != null) conn.disconnect();
			}
		}

		@Override
		public void onPostExecute(String result) {
			onDone(result);
		}

	}

}
