
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
import java.sql.Date;
import java.util.HashMap;
import java.util.Scanner;

public class UrlCache {

	HashMap<String, Long> catalog;
	
	PrintWriter outputStream;
	Scanner inputStream;
	
	private class MyURL {
		private String host;
		private String port;
		private String path;
		public MyURL (String url) {
			String host = "";
			String port = "";
			String path = "";
			
			for (int h = 0; h < url.length(); h++){
				if (url.charAt(h) == ':'){ //get hostname
					host = url.substring(0, h);
					for (int p = h+1 ; p < url.length(); p++){ //get port
						if (url.charAt(p) == '/'){
							port = url.substring(h+1, p);
							path = url.substring(p, url.length());
							break;
						}
					}
					break;
				} else if (url.charAt(h) == '/'){
					host = url.substring(0, h);
					port = "80";
					path = url.substring(h, url.length());
					break;
				}
			}
			
			this.host = host;
			this.path = path;
			this.port = port;
		}
		public String getHost() {
			return host;
		}
		
		public int getPort() {
			return Integer.parseInt(port);
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
			Socket sock = new Socket(u.getHost(), u.getPort());
			
			outputStream = new PrintWriter(new DataOutputStream(sock.getOutputStream()));
			inputStream = new Scanner(new InputStreamReader(sock.getInputStream()));
			
			String req = httpRequest(u.getPath(), u.getHost());
			outputStream.print(req);
			outputStream.flush();
			String s ="";
			do {
				s+=inputStream.nextLine() + "\n";
			} while (inputStream.hasNextLine());
			
			inputStream.close();
			outputStream.close();
			sock.close();
			
			System.out.print(req + "\n" + s);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public String httpRequest(String path, String host){
		String request = "GET " + path + " HTTP/1.1\r\n";
		request+= "Host: " + host + "\r\n\r\n";
		return request;
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
		try {
			long lastModifiedLocal = catalog.get(url);
			if (lastModifiedLocal < getLastModified(url)){
				// TODO get the thing
			}
		} catch (Exception e) {
			// TODO: Get the thing
		} finally {
			// Save the thing.
		}
		
	}

}
