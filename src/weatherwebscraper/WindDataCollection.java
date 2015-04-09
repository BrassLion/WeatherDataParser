/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherwebscraper;

import java.util.ArrayList;

/**
 *
 * @author 422
 */
class WindDataCollection implements Comparable<WindDataCollection> {

    public String owner;
    public String ownerID;

    public ArrayList<WindDataPoint> points = new ArrayList<>();

    public WindDataCollection(String own, String ownID) {
        this.owner = own;
        this.ownerID = ownID;
    }

    public void add(WindDataPoint point) {
        points.add(point);
    }

    @Override
    public int compareTo(WindDataCollection c) {
        return owner.compareTo( c.owner );
    }
}
