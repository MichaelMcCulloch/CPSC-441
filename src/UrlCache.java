
/**
 * UrlCache Class
 * 
 *
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlCache {

	HashMap<String, Long> catalog;
	
	PrintWriter outputStream;
	Scanner inputStream;
	
	/**
	 * Break the URL into Host, Port, and Path
	 * @author micha
	 *
	 */
	private class MyURL {
		private String host;
		private int port;
		private String path;
		public MyURL (String url) {
			String withPortRE = "(.*):([\\d]*)(/.*)";
			Pattern withPort = Pattern.compile(withPortRE, Pattern.CASE_INSENSITIVE);
			String woPortRE = "([^/]+)(.*)";
			Pattern woPort = Pattern.compile(woPortRE, Pattern.CASE_INSENSITIVE);
			
			
			Matcher withPortMatcher = withPort.matcher(url);
			Matcher woPortMatcher = woPort.matcher(url);
			if (withPortMatcher.find()){
				this.host = withPortMatcher.group(1);
				this.port = Integer.parseInt(withPortMatcher.group(2));
				this.path = withPortMatcher.group(3);
			} else if (woPortMatcher.find()){
				this.host = woPortMatcher.group(1);
				this.port = 80;
				this.path = woPortMatcher.group(2);
				
			}
			
		}
		public String getHost() {
			return host;
		}
		
		public int getPort() {
			return port;
		}
		
		public String getPath() {
			return path;
		}
	
	}
    
	
	
	/**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	public UrlCache() throws IOException {
		//TODO: Check if catalog.txt already exists
		catalog = new HashMap<String, Long>();
		
	}
	

	
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException {
		MyURL u = new MyURL(url);
		try {
			//1. Open a TCP Connection to the server;
			Socket sock = new Socket(u.getHost(), u.getPort());
			outputStream = new PrintWriter(new DataOutputStream(sock.getOutputStream()));
			inputStream = new Scanner(new InputStreamReader(sock.getInputStream()));
			
			//2. Format the get Request and send it to the server
			String req = httpRequest(u.getPath(), u.getHost());
			outputStream.print(req);
			outputStream.flush();
			
			ArrayList<String> headers = new ArrayList<>();
			do {
				//4.
				String next = inputStream.nextLine() + "\n";
				if (next.toCharArray()[0] == '\n'){
					break;
				} else {
					headers.add(next);
				}
			} while (inputStream.hasNextLine());
			
			HTTPHeaderParser parser = new HTTPHeaderParser(headers);
			if (parser.status == 200){
				catalog.put(url, parser.lastModified);
				//6
				String body ="";
				do {
					body+=inputStream.nextLine()+ '\n';
				} while (inputStream.hasNextLine());
			}
			
			
			
			
			//7
			inputStream.close();
			outputStream.close();
			sock.close();
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private class HTTPHeaderParser {
		private int status;
		private String type;
		private String subtype;
		private long lastModified;
		
		String statusRE = "HTTP.* ([0-9]*) .*"; 
		Pattern statusPattern = Pattern.compile(statusRE, Pattern.CASE_INSENSITIVE);
		
		String lastModifiedRE = "Last-Modified:[ ]*(.*)";
		Pattern lastModifiedPattern = Pattern.compile(lastModifiedRE, Pattern.CASE_INSENSITIVE);
		
		String typeRE = "Content-Type:[ ]*(([\\w]*)/([\\w]*))";
		Pattern typePattern = Pattern.compile(typeRE, Pattern.CASE_INSENSITIVE);
		
		public HTTPHeaderParser(ArrayList<String> headers) {
			
			Matcher httpStatusMatcher = statusPattern.matcher(headers.get(0));
			if (httpStatusMatcher.find()){
				this.status = Integer.parseInt(httpStatusMatcher.group(1));
			}
			if (this.status != 200){
				return;
			}
			
			
			for (int i = 1; i < headers.size(); i++) {
				if (headers.get(i) == null) continue;
				if (headers.get(i).contains("Last-Modified:")){
					Matcher lmMatcher = lastModifiedPattern.matcher(headers.get(i));
					if (lmMatcher.find()){
						this.lastModified = dateToMillis(lmMatcher.group(1));
					}
				} else if (headers.get(i).contains("Content-Type:")){
					Matcher typeMatcher = typePattern.matcher(headers.get(i));
					if (typeMatcher.find()){
						this.type = typeMatcher.group(2);
						this.subtype = typeMatcher.group(3);
					}
				}
				
			}
		}

		public int getStatus() {
			return status;
		}

		public String getSubtype() {
			return subtype;
		}

		public long getLastModified() {
			return lastModified;
		}
	}
	
	
	
	/**
	 * Embed the server and path into a HTTP Request
	 * @param path
	 * @param host
	 * @return
	 */
	public String httpRequest(String path, String host){
		String request = "GET " + path + " HTTP/1.1\r\n";
		request+= "Host: " + host + "\r\n\r\n";
		return request;
	}
	
	public void saveObject(String body, String type){
		
	}
	
	public long dateToMillis(String modified){
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		Date date = null;
		date = format.parse(modified, new ParsePosition(0));
		long millis = date.getTime();
		return millis;
	}
	
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     */
	public long getLastModified(String url) {
		long millis = 0;
		return millis;
	}
	
	public void conditionalGet(String url){
		
		
	}

}
