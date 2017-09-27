
/**
 * UrlCache Class
 * 
 *
 */

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;

public class UrlCache {

	HashMap<String, WebObject> catalog;
	
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	public UrlCache() throws IOException {
		catalog = new HashMap<String, WebObject>();
		
	}
	
	private class WebObject {
		String theObject;
		public WebObject(String theObject){
			this.theObject = theObject;
		}
		public long lastModified(){
			//TODO: return the last modified time of this object
			return 0;
		}
	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException {
		try {
			WebObject obj = catalog.get(url);
			long lastModifiedFromWeb = getLastModified(url);
			long lastModifiedCatalog = obj.lastModified();
			if (lastModifiedFromWeb > lastModifiedCatalog) {
				
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	private WebObject fetchObject(String url) {
		return null;
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
