/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.worldwindearth.geocode;

import gov.nasa.worldwind.geom.Position;
import java.util.ArrayList;
import org.tinyrcp.TinyPlugin;

/**
 * Geocode plugin
 * 
 * @author sbodmer
 */
public interface WWEGeocodePlugin extends TinyPlugin {
    
    /**
     * Return the list of location for the passed address
     * 
     * @param pos
     * @return 
     */
    
    public ArrayList<Result> geocode(String house, String street, String zip, String city, String country);
    
    public ArrayList<Result> reverse(Position pos);
}
