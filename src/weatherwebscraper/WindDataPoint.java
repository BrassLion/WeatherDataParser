/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherwebscraper;

/**
 *
 * @author 422
 */
class WindDataPoint implements Comparable<WindDataPoint> {

    public long time;
    public String direction;
    public String speedLo;
    public String speedHi;
    public String gust;

    public WindDataPoint(long t, String dir, String velRange, String gust) {

        this.time = t;
        this.direction = ConvertBearingToDegrees(dir);
        this.gust = gust;
        
        //parse velocity range into separate variables
        String[] velArray = velRange.replaceAll("[a-zA-z]", "").split("-");
        
        if(velArray.length >= 2) {
            this.speedLo = velArray[0].trim();
            this.speedHi = velArray[1].trim();
        }
        else {
            this.speedLo = null;
            this.speedHi = null;
        }
    }

    private String ConvertBearingToDegrees(String direction) {

        switch (direction) {
            case "N":
                return "180";
            case "NE":
                return "225";
            case "E":
                return "270";
            case "SE":
                return "315";
            case "S":
                return "0";
            case "SW":
                return "45";
            case "W":
                return "90";
            case "NW":
                return "135";
            default:
                return direction;
        }
    }

    @Override
    public int compareTo(WindDataPoint p) {
        return Long.compare(this.time, p.time);
    }
}
