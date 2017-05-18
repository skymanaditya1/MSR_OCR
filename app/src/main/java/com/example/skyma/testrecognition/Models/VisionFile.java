package com.example.skyma.testrecognition.Models;

import java.io.Serializable;
import java.util.List;

/**
 * Created by skyma on 5/16/2017.
 */

public class VisionFile implements Serializable{
    public String language;
    public int textAngle;
    public String orientation;
    public List<Region> regions;
}
