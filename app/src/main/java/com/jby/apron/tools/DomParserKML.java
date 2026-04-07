package com.jby.apron.tools;

import android.util.Log;

import com.jby.apron.entity.FlightMission;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;


public class DomParserKML {
    private static final String TAG = "DomParserKML";
    private String kmlFilePath = "";
    private String kmlFileName = "";

    private Element docElement;

 
    /**
     * 构造
     *
     * @param filePath
     * @param fileName
     */
    public DomParserKML(String filePath, String fileName) {
        this.kmlFilePath = filePath;
        this.kmlFileName = fileName;
    }
 
    /**
     * 创建kml文件
     */
    public void createKml(FlightMission mission) {

        String fileName = kmlFilePath + kmlFileName;
        Document document = DocumentHelper.createDocument();// 建立document对象，用来操作xml文件
        Element kmlElement = document.addElement("kml", "http://www.opengis.net/kml/2.2");// 建立根节点
        kmlElement.addAttribute("xmlns:wpml", "http://www.dji.com/wpmz/1.0.2");
        kmlElement.addNamespace("wpml", "http://www.dji.com/wpmz/1.0.2");
 
        kmlElement.addElement("Document");// 添加一个Document节点

        try {
            XMLWriter writer = new XMLWriter(new FileWriter(new File(fileName)));
            writer.write(document); //写入
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("createKml写入异常","-------");
        }
    }

}