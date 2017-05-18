package com.example.skyma.testrecognition.Models;

import java.io.Serializable;
import java.util.List;

/**
 * Created by skyma on 5/16/2017.
 */

public class Line implements Serializable{
    public String boundingBox;
    public List<Word> words;
}
