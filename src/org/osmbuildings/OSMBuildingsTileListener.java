/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.osmbuildings;

/**
 *
 * @author sbodmer
 */
public interface OSMBuildingsTileListener {
    public void osmBuildingsLoaded(OSMBuildingsTile btile);
    public void osmBuildingsLoading(OSMBuildingsTile btile);
    public void osmBuildingsLoadingFailed(OSMBuildingsTile btile, String reason);
    
    /**
     * To avoid multiple same rendering, return true if the passed id was
     * already rendered
     * 
     * @param id The GeoJSON string id
     * @return 
     */
    public boolean osmBuildingsProduceRenderableForId(String id);
}
