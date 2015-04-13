/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherwebscraper;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author Craig, Sam
 */
public class GoogleMapsQuery {
    
    public final static Point2d boundingBoxSWCorner = new Point2d(49.69016250118635, -11.646131347656251);
    public final static Point2d boundingBoxNECorner = new Point2d(63.049187392023704, 2.9025200195312664);

    /**
     * @param args the command line arguments
     *
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Gets geolocation data from Google Maps API and returns the coordinate closest to the target coordinate supplied.
     * 
     * @param address A String of the name of the place supplied to the Google search.
     * @param targetLat Latitude of the target coordinate in decimal notation
     * @param targetLng Longitude of the target coordinate in decimal notation
     * @param distanceThreshold if distance between target coordinate and closest Google coordinate is less than threshold, return Google coordinate. Else return target coordinate.
     * @return an array containing [closest coordinate latitude, closest coordinate longitude, distance between target and closest coordinate, number of locations returned by Google].
     */
    public static double[] getClosestLatLong(String address, double targetLat, double targetLng, double distanceThreshold) {

        System.out.println("\tGetting Google Location Data for " + address + "...");
        
        URL url = null;
        try {
            String bounds = String.format( "&bounds=%f,%f|%f,%f",
                    boundingBoxSWCorner.x,
                    boundingBoxSWCorner.y,
                    boundingBoxNECorner.x,
                    boundingBoxNECorner.y
            );
            
            url = new URL( "http://maps.googleapis.com/maps/api/geocode/json?address=" + address.replaceAll("[ ']", "%20") + bounds + "&sensor=false" );
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(GoogleMapsQuery.class.getName()).log(Level.SEVERE, null, ex);
        }

        BufferedReader theHTML;
        String jsonText = null;
        try {
            theHTML = new BufferedReader(new InputStreamReader(url.openStream()));
            jsonText = readAll(theHTML);
        } catch (IOException ex) {
            Logger.getLogger(GoogleMapsQuery.class.getName()).log(Level.SEVERE, null, ex);
        }

        JSONObject json = (JSONObject) JSONValue.parse(jsonText);
        JSONArray jResults = (JSONArray) JSONValue.parse(json.get("results").toString());
        
        if(jResults.size() == 0) {
            System.out.println("\t\tGoogle location search returned error code:\n\t\t" + json.get("status").toString());
            return null;
        }
        
        double closestLat = 0;
        double closestLng = 0;
        double closestDist = 0;
        
        for(int i = 0;i < jResults.size();i++) {
            JSONObject jAddress = (JSONObject) jResults.get(i);
            JSONObject jGeometry = (JSONObject) jAddress.get("geometry");
            JSONObject jLocation = (JSONObject) jGeometry.get("location");

            double lat = (double) jLocation.get("lat");
            double lng = (double) jLocation.get("lng");
            double distance = distanceBetweenLatLongs(lat, lng, targetLat, targetLng);
            
            if(distance < closestDist || i == 0) {
                closestLat = lat;
                closestLng = lng;
                closestDist = distance;
            }
        }
        
        try {
            Thread.sleep(200);

        } catch (InterruptedException ie) {
            System.out.println("Slight Pause");
        }

        if(closestDist < distanceThreshold) {
            System.out.println("\t\tSearch returned " + jResults.size() + " results. Closest coordinate was " + closestDist + "m from target coordinate");
            return new double[]{closestLat, closestLng, closestDist, jResults.size()};
        }
        else {
            System.out.println("\t\tSearch returned no coordinates within " + distanceThreshold + "m threshold, returning target coordinates.");
            return new double[]{targetLat, targetLng, 0, 0};
        }
    }
    
    /**
     * Parses strings containing a target position latitude and longitude in DMS notation and returns the closest coordinate found by Google.
     * 
     * @param address A String of the name of the place supplied to the Google search.
     * @param targetLat Latitude of the target coordinate in DMS notation
     * @param targetLng Longitude of the target coordinate in DMS notation
     * @param distanceThreshold if distance between target coordinate and closest Google coordinate is less than threshold, return Google coordinate. Else return target coordinate.
     * @return an array containing [closest coordinate latitude, closest coordinate longitude, distance between target and closest coordinate, number of locations returned by Google].
     */
    public static double[] getClosestLatLong(String address, String targetLat, String targetLng, double distanceThreshold) {
        double latDecimal = latLngDMSToDeg( targetLat );
        double lngDecimal = latLngDMSToDeg( targetLng );
        
        return getClosestLatLong( address, latDecimal, lngDecimal, distanceThreshold );
    }
    
    /**
     * Parses a string containing a latLong coordinate in DMS notation to decimal notation
     * 
     * @param input DMS latitude or longitude coordinate
     * 
     * @return Decimal coordinate
     */
    private static double latLngDMSToDeg( String input ) {
        
        String[] coordinateString = input.split("[^\\d\\w^\\.]+");
        double[] coordinate = new double[coordinateString.length];
        
        if(coordinateString.length > 4) 
            throw new IllegalArgumentException("LatLong coordinate parsing incorrectly");
        
        for(int i = 0;i < coordinateString.length - 1;i++) {
            try {
                coordinate[i] = Double.parseDouble( coordinateString[i] );
            } catch(NumberFormatException e) {
                throw e;
            }
        }
        
        switch(coordinateString[coordinateString.length - 1]) {
            case "N":
                coordinate[coordinateString.length - 1] = 1;
                break;
            case "S":
                coordinate[coordinateString.length - 1] = -1;
                break;
            case "W":
                coordinate[coordinateString.length - 1] = -1;
                break;
            case "E":
                coordinate[coordinateString.length - 1] = 1;
                break;
            default:
                throw new IllegalArgumentException("LatLong compass direction invalid");
        }
        
        if(coordinate.length == 3)
            return (coordinate[0] + coordinate[1] / 60.0) * coordinate[2];
        else if(coordinate.length == 4) 
            return (coordinate[0] + coordinate[1] / 60.0 + coordinate[2] / 3600.0) * coordinate[3];
        else
            throw new IllegalArgumentException("Invalid latLong coordinate");
    }
    
    /**
     * Uses Haversine Formula to return distance between two points on the globe as the crow flies.
     * 
     * @param lat1 
     * @param lng1
     * @param lat2
     * @param lng2
     * @return 
     */
    private static double distanceBetweenLatLongs(double lat1, double lng1, double lat2, double lng2) {
        double rlat1 = degToRad(lat1);
        double rlat2 = degToRad(lat2);
        double drlat = degToRad(lat2 - lat1);
        double drlng = degToRad(lng2 - lng1);
        double R = 6371000.0;                           //Earth's radius
        
        double a = Math.sin(drlat / 2.0) * Math.sin(drlat / 2.0) + Math.cos( rlat1 ) * Math.cos( rlat2 ) * Math.sin(drlng / 2.0) * Math.sin(drlng / 2.0);
        double c = 2 * Math.atan2( Math.sqrt(a), Math.sqrt(1.0 - a) );
        double d = R * c;
        
        return d;
    }
    
    private static double degToRad(double value) {
        return value * Math.PI / 180.0;
    }
}
