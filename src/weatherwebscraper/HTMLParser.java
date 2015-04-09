/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherwebscraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author 422
 */
public class HTMLParser {
    
    public static String parseTableAtURL( String url, int tableNumber ) {
        
        int tableNum = 0;
        
        try {

            Pattern p = Pattern.compile(">(\\w.*?)<|WMO=(.*?)&");
            
            URL dataHTMLLink;
            BufferedReader in = null;
            
            try {
                dataHTMLLink = new URL( url );
                in = new BufferedReader( new InputStreamReader(dataHTMLLink.openStream()) );
                
            } catch (MalformedURLException ex) {
                Logger.getLogger(HTMLParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            String rawData = "";
            String rawDataLine = null;
            boolean insideTable = false;
            int lineNum = 0;
            
            while ((rawDataLine = in.readLine()) != null) {
                lineNum++;
                if( !insideTable && rawDataLine.contains("<table") ) {
                    insideTable = true;
                    tableNum++;
                    //skip fist <tr> tag
                    in.readLine();
                }
                
                else if( insideTable && rawDataLine.contains("</table>") ) {
                    insideTable = false;
                }
                
                else if( insideTable && tableNum == tableNumber ) {
                    
                    if(rawDataLine.contains("<tr")) {
                        rawData += "\n";
                    }
                    
                    if(rawDataLine.contains("<td")) {
                        Matcher m = p.matcher(rawDataLine);
                        int groupNum = m.groupCount();
                        
                        while(m.find()) {
                            
                            for(int i = 1;i <= groupNum;i++) {
                                if(m.group(i) != null) {
                                    rawData += m.group(i);
                                    rawData += ",";
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            in.close();
            
            return rawData;
            
        } catch (IOException ex) {
            Logger.getLogger(HTMLParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public static String parseTableAtURL( String url ) {
        return parseTableAtURL( url, 1 );
    }
}
