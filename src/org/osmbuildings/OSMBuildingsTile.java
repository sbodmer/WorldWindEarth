/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.osmbuildings;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.formats.geojson.GeoJSONDoc;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.retrieve.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Thread to download the gson data
 *
 * @author sbodmer
 */
public class OSMBuildingsTile {

    /**
     * Special key for ww osm
     */
    static final String OSMBUILDINGS_URL = "http://[abcd].data.osmbuildings.org/0.2/sx3pxpz6/tile";
    static final String NASA_BINGMAPS_URL = "https://worldwind27.arc.nasa.gov/wms/virtualearth";

    /**
     * Fetch the root texture directly for virtualearth
     */
    static final String BING_URL = "http://a[123].ortho.tiles.virtualearth.net/tiles";

    // 15/16942/11632.json"
    /**
     * The counter for the different servers
     */
    static int current = 0;

    int x = 0;
    int y = 0;
    int level = 15;
    double defaultHeight = 10;
    boolean applyRoofTextures = false;
    ShapeAttributes defaultAttrs = null;

    Position center = null;
    FileStore store = null;
    boolean retrieveRemoteData = true;
    long expireDate = 0;
    String cachePath = "";

    /**
     * The loading timestamp
     */
    long ts = 0;

    OSMBuildingsTileListener listener = null;

    /**
     * The loaded buildings
     */
    OSMBuildingsRenderable renderable = null;

    /**
     * The tile bounding box
     */
    ExtrudedPolygon tile = null;

    LatLon bl = null;   //--- Bottom left
    LatLon tl = null;   //--- Top left
    LatLon tr = null;   //--- Top right
    LatLon br = null;   //--- Bottom right

    public OSMBuildingsTile(int level, int x, int y, OSMBuildingsTileListener listener, Position center, FileStore store, boolean retrieveRemoteData, long expireDate, double defaultHeight, boolean applyRoofTextures, ShapeAttributes defaultAttrs) {
        this.x = x;
        this.y = y;
        this.level = level;
        this.center = center;
        this.listener = listener;
        this.store = store;
        this.retrieveRemoteData = retrieveRemoteData;
        this.defaultHeight = defaultHeight;
        this.expireDate = expireDate;
        this.defaultAttrs = defaultAttrs;
        this.applyRoofTextures = applyRoofTextures;
        
        cachePath = OSMBuildingsLayer.CACHE_FOLDER + File.separatorChar + level + File.separatorChar + x + File.separatorChar + y + ".json";

        BufferedImage tex = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) tex.getGraphics();
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 256, 256);
        g2.setColor(Color.WHITE);
        g2.drawString("" + x + "y" + y, 30, 30);

        //--- Create the surface box for tile information
        tile = new ExtrudedPolygon();
        List<LatLon> list = new ArrayList<LatLon>();
        /*
        double lat = (y*180d/OSMBuildingsLayer.maxY)-90d;
        double dy = 180d/OSMBuildingsLayer.maxX;
        double lon = (x*360d/OSMBuildingsLayer.maxX)-180d;
        double dx = 360d/OSMBuildingsLayer.maxX;
         */
        double lat1 = OSMBuildingsLayer.y2lat(y + 1, OSMBuildingsLayer.ZOOM);
        double lon1 = OSMBuildingsLayer.x2lon(x, OSMBuildingsLayer.ZOOM);
        double lat2 = OSMBuildingsLayer.y2lat(y, OSMBuildingsLayer.ZOOM);
        double lon2 = OSMBuildingsLayer.x2lon(x + 1, OSMBuildingsLayer.ZOOM);
        bl = LatLon.fromDegrees(lat1, lon1);       //--- Bottom left
        tl = LatLon.fromDegrees(lat2, lon1);     //--- Top left
        tr = LatLon.fromDegrees(lat2, lon2);  //--- Top right
        br = LatLon.fromDegrees(lat1, lon2);     //--- Bottom right
        list.add(bl);
        list.add(tl);
        list.add(tr);
        list.add(br);
        list.add(bl);
        tile.setOuterBoundary(list);
        tile.setVisible(true);
        tile.setHeight(100d);
        tile.setAltitudeMode(WorldWind.CONSTANT);
        tile.setEnableCap(true);

        ShapeAttributes att = new BasicShapeAttributes();
        att.setInteriorOpacity(0.1d);
        att.setEnableLighting(false);
        att.setOutlineMaterial(Material.BLACK);
        att.setOutlineWidth(1d);
        att.setInteriorMaterial(Material.GREEN);
        att.setDrawInterior(true);
        att.setDrawOutline(false);
        tile.setSideAttributes(att);

        ShapeAttributes cap = new BasicShapeAttributes();
        cap.setDrawInterior(true);
        cap.setInteriorOpacity(0.1d);
        cap.setEnableLighting(false);
        cap.setOutlineMaterial(Material.BLACK);
        cap.setOutlineWidth(1d);
        cap.setInteriorMaterial(Material.GREEN);
        cap.setDrawInterior(true);
        cap.setDrawOutline(true);
        tile.setCapAttributes(cap);
        
    }

    @Override
    public String toString() {
        return "" + x + "x" + y + "@" + level;
    }

    //**************************************************************************
    //*** API
    //**************************************************************************
    /**
     * Returns the renderable ids
     * 
     * @return 
     */
    public ArrayList<String> getIds() {
        if (renderable == null) return new ArrayList<>();
        return renderable.getIds();
    }
    
    /**
     * Start the data fetch via HTTPRetriever
     */
    public void fetch() {
        LocalBuildingsLoader lbl = new LocalBuildingsLoader(cachePath, this);
        try {
            if (listener != null)
                listener.osmBuildingsLoading(this);

            //------------------------------------------------------------------
            //--- Check in local file store first
            //------------------------------------------------------------------
            URL data = store.findFile(cachePath, false);
            if (data != null) {
                long now = System.currentTimeMillis();
                File f = new File(data.toURI());
                if (f.lastModified() < now - expireDate) {
                    f.delete();

                } else {
                    // System.out.println("FOUND LOCAL json:"+f);
                    WorldWind.getTaskService().addTask(lbl);

                }

            } else {
                //--- Retreive data from remote server
                String s = OSMBUILDINGS_URL;
                //--- Find the current server to use
                int i1 = s.indexOf('[');
                if (i1 != -1) {
                    int i2 = s.indexOf(']');
                    String sub = s.substring(i1 + 1, i2);
                    int l = sub.length();
                    current++;
                    if (current >= sub.length())
                        current = 0;
                    char c = sub.charAt(current);
                    s = s.replaceAll("\\[" + sub + "\\]", "" + c);
                }

                s += "/15/" + x + "/" + y + ".json";
                HTTPRetriever r = new HTTPRetriever(new URL(s), lbl);
                r.setConnectTimeout(10000);
                r.setReadTimeout(20000);
                WorldWind.getRetrievalService().runRetriever(r);
            }

        } catch (MalformedURLException ex) {
            //--- Failed
            if (listener != null)
                listener.osmBuildingsLoadingFailed(this, ".json file could not be found : " + ex.getMessage());

        } catch (URISyntaxException ex) {
            //---
            //--- Failed
            if (listener != null)
                listener.osmBuildingsLoadingFailed(this, ".json file could not be found : " + ex.getMessage());

        }
    }

    public Renderable getRenderable() {
        /*
        ShapeAttributes a4 = new BasicShapeAttributes();
        a4.setInteriorOpacity(1);
        a4.setEnableLighting(true);
        a4.setOutlineMaterial(Material.RED);
        // a4.setOutlineWidth(2d);
        a4.setDrawInterior(true);
        a4.setDrawOutline(false);
        Cylinder c = new Cylinder(center, 100, 100);
        c.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        c.setAttributes(a4);
        c.setVisible(true);
         */
        return renderable;
    }

    /**
     * Return the tile footprint as a surface on the ground
     *
     * @return
     */
    public Renderable getTileSurfaceRenderable() {
        return tile;
    }

    /**
     * Tick tile usage
     */
    public void tick() {
        ts = System.currentTimeMillis();
    }

    public long getLastUsed() {
        return ts;
    }

    //******************************************************************************************************************
    //*** RetrievalPostProcessor
    //******************************************************************************************************************
    /**
     * Dump the data to local file store, create the renderable and call the
     * layer for rendering
     *
     * @param retriever
     *
     * @return
     */
    /*
    @Override
    public ByteBuffer run(Retriever retriever) {
        HTTPRetriever hr = (HTTPRetriever) retriever;

        try {
            if (hr.getResponseCode() == HttpURLConnection.HTTP_OK) {
                byte b[] = hr.getBuffer().array();
                if (b.length == 0) return null;

                //--- Store to cache file
                File f = store.newFile(cachePath);
                FileOutputStream fout = new FileOutputStream(f);
                //--- The buffer contains trailling 0000, so convert to string to remove it
                //--- Why is that, no idea ???
                String tmp = new String(b, "UTF-8").trim();
                fout.write(tmp.getBytes("UTF-8"));
                fout.close();

                //--- Load the data
                GeoJSONDoc doc = new GeoJSONDoc(f.toURI().toURL());
                doc.parse();

                renderable = new OSMBuildingsRenderable(doc, defaultHeight, defaultAttrs);
                if (listener != null) listener.osmBuildingsLoaded(this);

            } else {
                //--- Wrong http response
                if (listener != null)
                    listener.osmBuildingsLoadingFailed(this, ".json file could not be found, wrong http response : " + hr.getResponseCode() + " " + hr.getResponseMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //--- Failed
            if (listener != null)
                listener.osmBuildingsLoadingFailed(this, ".json file could not be found : " + ex.getMessage());
        }
        return null;
    }
     */
    //**************************************************************************
    //*** Runnable
    //**************************************************************************
    /**
     * Load local cached file
     */
    /*
    @Override
    public void run() {
        try {
            URL data = store.findFile(cachePath, false);

            //--- Load the data
            GeoJSONDoc doc = new GeoJSONDoc(data);
            doc.parse();

            renderable = new OSMBuildingsRenderable(doc, defaultHeight, defaultAttrs);
            if (listener != null) listener.osmBuildingsLoaded(this);

        } catch (NullPointerException ex) {
            //--- File is no more in local storage ?
            if (listener != null) listener.osmBuildingsLoadingFailed(this, ".json file could not be found");

        } catch (IOException ex) {
            //--- Failed
            if (listener != null) listener.osmBuildingsLoadingFailed(this, ".json file could not be found");
        }
    }
     */
    /**
     * Local and remove listener for buildings json data loading process<p>
     *
     * When the json has arrived, fetch the textures
     * 
     */
    private class LocalBuildingsLoader implements Runnable, RetrievalPostProcessor {

        String path = null;
        OSMBuildingsTile ti = null;

        private LocalBuildingsLoader(String file, OSMBuildingsTile ti) {
            this.path = file;
            this.ti = ti;

        }

        @Override
        public void run() {
            try {
                URL data = store.findFile(path, false);

                //--- Load the data
                GeoJSONDoc doc = new GeoJSONDoc(data);
                doc.parse();
                
                renderable = new OSMBuildingsRenderable(doc, defaultHeight, defaultAttrs, data.toString(), listener);
                if (listener != null) listener.osmBuildingsLoaded(ti);

                if (applyRoofTextures) fetchRoofTextures();

            } catch (NullPointerException ex) {
                //--- File is no more in local storage ?
                if (listener != null) listener.osmBuildingsLoadingFailed(ti, "Local .json file could not be found : " + cachePath);

            } catch (IOException ex) {
                //--- Failed
                if (listener != null) listener.osmBuildingsLoadingFailed(ti, "Local .json file could not be found : " + cachePath);
            }
        }

        @Override
        public ByteBuffer run(Retriever retriever) {
            HTTPRetriever hr = (HTTPRetriever) retriever;
            try {
                if (hr.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    byte b[] = hr.getBuffer().array();
                    if (b.length == 0) return null;

                    //--- Store to cache file
                    File f = store.newFile(path);
                    FileOutputStream fout = new FileOutputStream(f);
                    //--- The buffer contains trailling 0000, so convert to string to remove it
                    //--- Why is that, no idea ???
                    String tmp = new String(b, "UTF-8").trim();
                    fout.write(tmp.getBytes("UTF-8"));
                    fout.close();

                    //--- Load the data
                    GeoJSONDoc doc = new GeoJSONDoc(f.toURI().toURL());
                    doc.parse();

                    renderable = new OSMBuildingsRenderable(doc, defaultHeight, defaultAttrs, f.toString(), listener);
                    if (listener != null) listener.osmBuildingsLoaded(ti);

                    if (applyRoofTextures) fetchRoofTextures();

                } else {
                    //--- Wrong http response
                    if (listener != null)
                        listener.osmBuildingsLoadingFailed(ti, ".json file could not be found, wrong http response : " + hr.getResponseCode() + " " + hr.getResponseMessage());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                //--- Failed
                if (listener != null)
                    listener.osmBuildingsLoadingFailed(ti, ".json file could not be found : " + ex.getMessage());
            }
            return hr.getBuffer();
        }

        private void fetchRoofTextures() {
            RoofTextureLoader rtl = new RoofTextureLoader(cachePath + "_roof.png", ti);

            try {
                //------------------------------------------------------------------
                //--- Get the roof texture (world wind bing image)
                //------------------------------------------------------------------
                URL roof = store.findFile(cachePath + "_roof.png", false);
                if (roof != null) {
                    long now = System.currentTimeMillis();
                    File f = new File(roof.toURI());
                    if (f.lastModified() < now - expireDate) {
                        f.delete();

                    } else {
                        // System.out.println("FOUND LOCAL json");
                        WorldWind.getTaskService().addTask(rtl);

                    }

                } else {
                    String s = NASA_BINGMAPS_URL + "?";
                    s += "service=WMS";
                    s += "&request=GetMap";
                    s += "&version=1.1.1";
                    s += "&srs=EPSG:4326";
                    s += "&layers=ve";
                    s += "&styles=";
                    s += "&transparent=FALSE";
                    s += "&format=image/png";
                    s += "&width=2048";
                    s += "&height=2048";
                    s += "&bbox=" + bl.longitude.degrees + "," + bl.latitude.degrees + "," + tr.longitude.degrees + "," + tr.latitude.degrees;
                    System.out.println("S:" + s);
                    HTTPRetriever r = new HTTPRetriever(new URL(s), rtl);
                    r.setConnectTimeout(10000);
                    r.setReadTimeout(20000);
                    WorldWind.getRetrievalService().runRetriever(r);

                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private class RoofTextureLoader implements Runnable, RetrievalPostProcessor {

        String path = null;
        OSMBuildingsTile ti = null;

        private RoofTextureLoader(String file, OSMBuildingsTile ti) {
            this.path = file;
            this.ti = ti;

        }

        @Override

        public void run() {
            try {
                URL data = store.findFile(path, false);
                BufferedImage tex = ImageIO.read(data);
                // Graphics g = tex.getGraphics();
                // g.setColor(Color.RED);
                // g.fillRect(0, (tex.getHeight() / 2) - 10, tex.getWidth(), 20);

                float[] texCoords = new float[]{0, 0, 1, 0, 1, 1, 0, 1};
                tile.setCapImageSource(tex, texCoords, 4);

                renderable.applyRoofTexture(tex, tile.getSector());

            } catch (Exception ex) {
                ex.printStackTrace();

            }
        }

        @Override
        public ByteBuffer run(Retriever retriever) {
            HTTPRetriever hr = (HTTPRetriever) retriever;
            try {
                if (hr.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    byte b[] = hr.getBuffer().array();
                    if (b.length == 0) return null;

                    //--- Store to cache file
                    File f = store.newFile(path);
                    FileOutputStream fout = new FileOutputStream(f);
                    fout.write(b);
                    fout.close();

                    URL data = store.findFile(path, false);
                    BufferedImage tex = ImageIO.read(data);
                    // Graphics g = tex.getGraphics();
                    // g.setColor(Color.RED);
                    // g.fillRect(0, (tex.getHeight() / 2) - 10, tex.getWidth(), 20);

                    float[] texCoords = new float[]{0, 0, 1, 0, 1, 1, 0, 1};
                    tile.setCapImageSource(tex, texCoords, 4);

                    renderable.applyRoofTexture(tex, tile.getSector());

                } else {
                    //---

                }

            } catch (Exception ex) {
                ex.printStackTrace();

            }
            return hr.getBuffer();
        }
    }

}
