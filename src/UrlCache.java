
/**
 * UrlCache Class
 * 
 *
 */

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	 * Process HTTP headers.
	 * @author micha
	 *
	 */
	
	private class HTTPHeaderParser {
		private int status;
		private String type;
		private String subtype;
		private long lastModified;
		private int length;

		private String statusRE = "HTTP.* ([0-9]*) .*"; 
		private Pattern statusPattern = Pattern.compile(statusRE, Pattern.CASE_INSENSITIVE);
		
		private String lastModifiedRE = "Last-Modified:[ ]*(.*)";
		private Pattern lastModifiedPattern = Pattern.compile(lastModifiedRE, Pattern.CASE_INSENSITIVE);
		
		private String typeRE = "Content-Type:[ ]*(([\\w]*)/([\\w]*))";
		private Pattern typePattern = Pattern.compile(typeRE, Pattern.CASE_INSENSITIVE);
		
		private String lengthRE = "Content-Length:[ ]*(\\d*)";
		private Pattern lengthPattern = Pattern.compile(lengthRE, Pattern.CASE_INSENSITIVE);
		
		public HTTPHeaderParser(String[] headers) {
			
			Matcher httpStatusMatcher = statusPattern.matcher(headers[0]);
			if (httpStatusMatcher.find()){
				this.status = Integer.parseInt(httpStatusMatcher.group(1));
			}
			if (this.status != 200){
				return;
			}
			
			for (int i = 1; i < headers.length; i++) {
				if (headers[i] == null) continue;
				if (headers[i].contains("Last-Modified:")){
					Matcher lmMatcher = lastModifiedPattern.matcher(headers[i]);
					if (lmMatcher.find()){
						this.lastModified = dateToMillis(lmMatcher.group(1));
					}
				} else if (headers[i].contains("Content-Type:")){
					Matcher typeMatcher = typePattern.matcher(headers[i]);
					if (typeMatcher.find()){
						this.type = typeMatcher.group(2);
						this.subtype = typeMatcher.group(3);
					}
				} else if (headers[i].contains("Content-Length:")){
					Matcher lengthMatcher = lengthPattern.matcher(headers[i]);
					if (lengthMatcher.find()){
						this.length = Integer.parseInt(lengthMatcher.group(1));
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
		public String getType() {
			return type;
		}

		public long getLastModified() {
			return lastModified;
		}
		
		public int getLength() {
			return length;
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
			
			InputStream rawInput = sock.getInputStream();
			
			
			//2. Format the get Request and send it to the server
			String req = httpRequest(u.getPath(), u.getHost());
			outputStream.print(req);
			outputStream.flush();
			
			String header ="";
			
			
			byte[] lastFour = {0,0,0,0};
			int whereTheBodyStarts = 0;
			while (!header.contains("\r\n\r\n")){
				byte next = (byte)rawInput.read();
				header += (char)next +"";
				lastFour[0] = lastFour[1];
				lastFour[1] = lastFour[2];
				lastFour[2] = lastFour[3];
				lastFour[3] = next;
				whereTheBodyStarts+=1;
			}
			
			
			String[] headers = header.split("\r\n");
			
			
			HTTPHeaderParser parser = new HTTPHeaderParser(headers);
			
			if (parser.status == 200){
				catalog.put(url, parser.lastModified); //save the last modified data
				//6
				byte[] body = new byte[parser.getLength()];
				rawInput.read(body);
				
				saveObject(u.getPath(), body, parser.getType(), parser.getSubtype());
			}
			
			
			
			
			//7
			inputStream.close();
			outputStream.close();
			sock.close();
			
		} catch (Exception e) {
			// TODO: handle exception
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
	
	
	
	public void saveObject(String path, byte[] body, String type, String subType) throws IOException{
		
		switch (type) {
		case "text":
			Path out = Paths.get(System.getProperty("user.dir"), path);
			
			File file = new File(out.toString());
			file.getParentFile().mkdirs();
			FileWriter writer = new FileWriter(file);
			
			String content = new String(body);
			
			writer.write(content);
			writer.flush();
			writer.close();
			
			break;

		default:
			Path out1 = Paths.get(System.getProperty("user.dir"), path);
			
			File file1 = new File(out1.toString());
			file1.getParentFile().mkdirs();
			DataOutputStream os = new DataOutputStream(new FileOutputStream(file1));
			os.write(body);
			break;
		}
		
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
