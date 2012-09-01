package servlets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import classes.DataStore;
import classes.EmailActivation;
import classes.MemCache;

@SuppressWarnings("serial")
public class GoogleAppEngineServlet extends HttpServlet {
	String spaceDelimiter = "%20";
	boolean isFirstTerm = false;
	String imageDelimiter = "\"";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello, world");
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String searchText = req.getParameter("searchText");
		String url = "http://search.twitter.com/search.json?q=";
		String[] inputParameters = searchText.split(" ");
		for (String string : inputParameters) {
			url = url.concat(string);
			if (inputParameters.length != 1 && !isFirstTerm) {
				url = url.concat(spaceDelimiter);
				isFirstTerm = true;
			}
		}
		System.out.println(url);
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			String jsonText = sb.toString();
			JSONObject json;

			try {
				json = new JSONObject(jsonText);
				List<DataStore> listOfTweets = new ArrayList<DataStore>();
				DataStore tweet ;
				JSONArray array = json.getJSONArray("results");
				for (int i = 0; i < array.length(); i++) {
					tweet = new DataStore();
					tweet.setTweetId(array.getJSONObject(i).getLong("id"));
					tweet.setCreatedTime(array.getJSONObject(i).getString(
							"created_at"));
					String imageUrl = imageDelimiter
							+ array.getJSONObject(i).getString(
									"profile_image_url_https") + imageDelimiter;
					tweet.setUserImage(imageUrl);
					tweet.setUserName(array.getJSONObject(i).getString(
							"from_user_name"));
					MemCache memCache = new MemCache();
					
					MemCache.putInCache(Long.toString(tweet.getTweetId()), array.getJSONObject(i).getString("text"));
					listOfTweets.add(tweet);
					if (listOfTweets.size() == 10)
						break;
				}
				String messageBody = "";
				for(DataStore cacheTweet: listOfTweets)
				{
					messageBody=messageBody.concat("____________"+MemCache.getFromCache(Long.toString(cacheTweet.getTweetId())));
					cacheTweet.setTweetText(MemCache.getFromCache(Long.toString(cacheTweet.getTweetId())));
				}
				HttpSession session = req.getSession(true);
				session.setAttribute("listOfTweets", listOfTweets);
				session.setAttribute("isResultSet", true);
				RequestDispatcher r1 = req.getRequestDispatcher("/Search.jsp");
				EmailActivation.sendEmail(messageBody);
				try {
					r1.forward(req, resp);
				} catch (ServletException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} finally {
			is.close();
		}
	}
}
