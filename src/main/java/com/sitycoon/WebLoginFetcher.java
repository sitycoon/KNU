package com.sitycoon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

public class WebLoginFetcher {
	public static boolean isValidDateRange(String startDt, String endDt) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate startDate = LocalDate.parse(startDt, formatter);
		LocalDate endDate = LocalDate.parse(endDt, formatter);
		LocalDate today = LocalDate.now();
		return startDate.isBefore(endDate) && endDate.isAfter(today);
	}

	public static void main(String[] args) throws Exception {
		/*
		 * CookieStore cookieStore = new BasicCookieStore(); try (CloseableHttpClient
		 * client = HttpClients.custom().setDefaultCookieStore(cookieStore).build()) {
		 */
		CookieStore cookieStore = new BasicCookieStore();
		SSLContext sslContext = SSLContextBuilder.create()
				.loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true).build();
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext,
				(hostname, session) -> true);

		int count = 0;

		try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore)
				.setSSLSocketFactory(sslSocketFactory) // SSL 우회 추가
				.build()) {
			// 로그인 POST 요청
			HttpPost loginPost = new HttpPost("https://human.knu.ac.kr/login/loginProc.do");
			List<NameValuePair> loginParams = new ArrayList<>();
			loginParams.add(new BasicNameValuePair("mberId", "sitycoon"));
			loginParams.add(new BasicNameValuePair("password", "Kosaf123"));
			loginPost.setEntity(new UrlEncodedFormEntity(loginParams));

			HttpResponse loginResponse = client.execute(loginPost);
			System.out.println("로그인 응답 코드: " + loginResponse.getStatusLine().getStatusCode());

			Map<Integer, Integer> result = null;

			Properties props = new Properties();

			String startDt = null;
			String endDt = null;
			try (InputStream input = WebLoginFetcher.class.getClassLoader().getResourceAsStream("config.properties")) {
				if (input == null) {
					System.out.println("설정 파일을 찾을 수 없습니다.");
					return;
				}
				props.load(input);
				startDt = props.getProperty("startDt");
				endDt = props.getProperty("endDt");
				System.out.println("startDt: " + startDt);
				System.out.println("endDt: " + endDt);
			} catch (IOException e) {
				e.printStackTrace();
			}

			while (true && isValidDateRange(startDt, endDt)) {
				HttpPost checkPost = new HttpPost("https://human.knu.ac.kr/fcReservation/selectResProductList.do");
				List<NameValuePair> checkParams = new ArrayList<>();

				checkParams.add(new BasicNameValuePair("startDt", startDt));
				checkParams.add(new BasicNameValuePair("endDt", endDt));
				checkPost.setEntity(new UrlEncodedFormEntity(checkParams));

				HttpResponse checkResponse = client.execute(checkPost);
				System.out.println("체크 응답 코드: " + checkResponse.getStatusLine().getStatusCode());
				// String html = EntityUtils.toString(checkResponse.getEntity(), "UTF-8");
				String jsonData = EntityUtils.toString(checkResponse.getEntity(), "UTF-8");
				result = RoomParser.countRoomsByCapacity(jsonData);
				if (!result.isEmpty()) {
					break;
				}
				System.out.println("(" + ++count + ") 방없음");
				Thread.sleep(60_000); // 60,000 밀리초 = 60초 = 1분
			}

			String returnString = "";
			if (result.isEmpty()) {
				returnString += "빈방 없음";
			} else {
				for (Map.Entry<Integer, Integer> entry : result.entrySet()) {
					returnString += entry.getKey() + "인실: " + entry.getValue() + "개\n";
				}
			}

			System.out.println("페이지 내용: " + returnString);

			// -------------------------------------
			String token = "7796634359:AAF0BQkzkqEqniALCbybyokxMPgHPyZ1sGc";
			String chatId = "-1002831819674";
			String messageText = returnString;

			String urlStr = "https://api.telegram.org/bot" + token + "/sendMessage?";
			String urlParameters = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") + "&text="
					+ URLEncoder.encode(messageText, "UTF-8");

			boolean isProxy = false;

			String proxyHost = "123.140.146.7";
			int proxyPort = 3128;
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

			URL url = new URL(urlStr + urlParameters);

			HttpURLConnection conn = (isProxy) ? (HttpURLConnection) url.openConnection(proxy)
					: (HttpURLConnection) url.openConnection();

			// — 타임아웃 설정 —
			conn.setConnectTimeout(10000); // 연결 최대 10초
			conn.setReadTimeout(15000); // 응답 최대 15초

			// — 헤더 설정 (User-Agent 포함) —
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

			// — 연결 및 응답 수신 —
			int responseCode = conn.getResponseCode();
			System.out.println("Response Code: " + responseCode);

			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				String line;
				while ((line = in.readLine()) != null) {
					System.out.println(line);
				}
				in.close();
			} else {
				System.out.println("Unexpected HTTP status: " + responseCode);
			}
		}
	}
}