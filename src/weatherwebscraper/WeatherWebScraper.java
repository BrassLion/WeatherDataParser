/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherwebscraper;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author 422
 */
public class WeatherWebScraper {

    private static WeatherWebScraper window = null;
    static final String dataURLBase = "http://www.weatheronline.co.uk/weather/maps/current?LANG=en&DATE=%d&CONT=ukuk&LAND=UK&KEY=UK&SORT=2&UD=0&INT=06&TYP=windrichtung&ART=tabelle&RUBRIK=akt&R=310&CEL=C&SI=%s";
    static final String geoDataURLBase = "http://www.weatheronline.co.uk/weather/maps/current?LANG=en&CEL=C&SI=mph&MAPS=over&CONT=ukuk&LAND=UK&REGION=0003&WMO=%s&UP=0&R=310&LEVEL=180&NOREGION=0";
    static final String dataUnits = "mph";
    static final boolean useGoogleLocationData = true;
    //use Google geolocation coordinates if returned results are within this distance from target.
    static final double distanceThreshold = 100000;

    static final Pattern pLong = Pattern.compile("Longitude,(\\w.*?),");
    static final Pattern pLat = Pattern.compile("Latitude,(\\w.*?),");

    static Map<String, WindDataCollection> data = Collections.synchronizedMap(new HashMap<String, WindDataCollection>());
    static Map<String, Vector> geoData = Collections.synchronizedMap(new HashMap<String, Vector>());

    final static int iterations = 48;

    /**
     * @param args the command line arguments
     * @throws java.net.MalformedURLException
     */
    public static void main(String[] args) throws MalformedURLException, IOException {

        window = new WeatherWebScraper();
        CalendarFrame calendarFrame = new CalendarFrame(window);
    }

    public void ParseDateToCSV(Date date) {

        System.out.println("Starting data scrape for " + getDayFromDate(date) + "...");

        ParsingDataThread[] threadList = new ParsingDataThread[iterations];

        long UNIXTime = (date.getTime() / 1000);

        //start timer
        Timer timer = new Timer();
        //read data from site
       
        for (int i = 0; i < iterations; i++) {

            threadList[i] = new ParsingDataThread(UNIXTime);
            threadList[i].start();

            //add 1/2 hour to UNIX time
            UNIXTime += 60 * 30;
        }

        //check all threads are finished
        waitForThreadPoolCompletion(threadList);

        //measure time elapsed
        timer.printTimeElapsedMessage("Data parsing");

        //start new thread list and get coarse geo data
        ParsingGeoDataThread[] geoThreadList = new ParsingGeoDataThread[data.values().size()];

        int j = 0;

        for (WindDataCollection windCollection : data.values()) {
            geoThreadList[j] = new ParsingGeoDataThread(windCollection.owner, windCollection.ownerID);
            geoThreadList[j].start();
            j++;
        }

        //check all threads are finished
        waitForThreadPoolCompletion(geoThreadList);

        timer.printTimeElapsedMessage("Coarse geo data parsing");

        //parse data to CSV
        try {
            String filePath = System.getProperty("user.dir") + "\\" + getDayFromDate(date) + ".csv";
            File f = new File(filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));

            //write column titles
            writer.append(String.format(
                    "Station Name, Latitude, Longitude%s, Speed Units,\t UNIX Time, Wind Direction, Wind Speed Lo (%s), Wind Speed Hi (%s), Gust Speed (%s)", 
                    useGoogleLocationData ? ", number of locations returned by Google, distance between coarse and Google coordinates (m)" : "",
                    dataUnits, 
                    dataUnits, 
                    dataUnits)
            );
            writer.newLine();

            ArrayList<WindDataCollection> dataList = new ArrayList(data.values());

            //sort data collections by name
            Collections.sort(dataList);

            for (WindDataCollection collection : dataList) {
                writer.append(collection.owner + ",");

                if (useGoogleLocationData) {
                    String trimmedOwner = collection.owner.substring(0, collection.owner.lastIndexOf("(") - 1);
                    
                    //get fine geo data from Google
                    double[] latLong = GoogleMapsQuery.getClosestLatLong(trimmedOwner, geoData.get(collection.owner).get(0).toString(), geoData.get(collection.owner).get(1).toString(), distanceThreshold);
                    
                    if(latLong != null) {
                        writer.append(latLong[0] + ",");
                        writer.append(latLong[1] + ",");
                        writer.append(latLong[3] + ",");
                        writer.append(latLong[2] + ",");
                    }
                    else {
                        writer.append(",,,,,");
                    }
                } else {
                    writer.append(geoData.get(collection.owner).get(0) + ",");
                    writer.append(geoData.get(collection.owner).get(1) + ",");
                }
                
                writer.append( dataUnits );

                Collections.sort(collection.points);
                writer.append("\t");
                
                for (WindDataPoint point : collection.points) {
                    writer.append(point.time + ",");
                    writer.append(point.direction + ",");
                    writer.append(point.speedLo + ",");
                    writer.append(point.speedHi + ",");
                    writer.append(point.gust + "\t");
                }
                writer.newLine();
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        timer.printTimeElapsedMessage("Data writing");
        timer.printTotalTimeElapsed();
        System.out.println("Done.");
    }

    private String getDayFromDate(Date date) {
        String dateString = date.toString();
        String day = dateString.substring(0, 10) + dateString.substring(dateString.length() - 5);

        return day;
    }

    private String getTimeFromUNIX(long UNIX) {
        Date date = new Date(UNIX * 1000);
        String dateString = date.toString();
        String time = dateString.substring(11, 19);

        return time;
    }

    private void waitForThreadPoolCompletion(Thread[] pool) {
        for (Thread thread : pool) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(WeatherWebScraper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ParsingDataThread extends Thread {

        long UNIXTime;

        ParsingDataThread(long time) {
            UNIXTime = time;
        }

        @Override
        public void run() {
            System.out.println(this.getName() + " parsing data from " + getTimeFromUNIX(UNIXTime) + "...");
            String[] rawData = HTMLParser.parseTableAtURL(String.format(dataURLBase, UNIXTime, dataUnits)).split("\n");

            for (int j = 1; j < rawData.length; j++) {

                String[] dataPiece = rawData[j].split(",");
                String gust = null;

                if (dataPiece.length < 4) {
                    System.out.println("Data Missing from " + this.getName() + "!");
                    continue;
                }

                if (dataPiece[3].contains("Gusts")) {
                    gust = dataPiece[3].substring(dataPiece[3].indexOf("Gusts") + 6, dataPiece[3].lastIndexOf(")"));
                    dataPiece[3] = dataPiece[3].replaceAll("\\((.*?)\\)", "");
                }

                //if station exists
                synchronized (data) {
                    if (data.containsKey(dataPiece[1])) {
                        data.get(dataPiece[1]).add(new WindDataPoint(UNIXTime, dataPiece[2], dataPiece[3], gust));
                    } else {
                        WindDataCollection collection = new WindDataCollection(dataPiece[1], dataPiece[0]);
                        collection.add(new WindDataPoint(UNIXTime, dataPiece[2], dataPiece[3], gust));

                        data.put(dataPiece[1], collection);
                    }
                }
            }
        }
    }

    class ParsingGeoDataThread extends Thread {

        String owner;
        String ownerID;

        ParsingGeoDataThread(String own, String ownID) {
            this.owner = own;
            this.ownerID = ownID;
        }

        @Override
        public void run() {

            System.out.println("\t" + this.getName() + " getting coarse geo data for " + owner + "...");

            String stationGeoData = HTMLParser.parseTableAtURL(String.format(geoDataURLBase, ownerID));

            Vector latLong = new Vector(2);

            Matcher mLat = pLat.matcher(stationGeoData);
            if (mLat.find()) {
                latLong.add(StringEscapeUtils.unescapeHtml4(mLat.group(1)));
            } else {
                System.out.println("No Latitude data for " + owner);
            }

            Matcher mLong = pLong.matcher(stationGeoData);
            if (mLong.find()) {
                latLong.add(StringEscapeUtils.unescapeHtml4(mLong.group(1)));
            } else {
                System.out.println("No Longitude data for " + owner);
            }

            synchronized (geoData) {
                geoData.put(owner, latLong);
            }
        }
    }
}
