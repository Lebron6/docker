package com.aros.apron.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    public static void zip(String src,String dest) throws IOException {
        //定义压缩输出流
        ZipOutputStream out = null;
        try {
            //传入源文件
            File fileOrDirectory= new File(src);
            File outFile= new File(dest);
            //传入压缩输出流
            //创建文件前几级目录
            if (!outFile.exists()){
                File parentfile=outFile.getParentFile();
                if (!parentfile.exists()){
                    parentfile.mkdirs();
                }
            }
            //可以通过createNewFile()函数这样创建一个空的文件，也可以通过文件流的使用创建
            out = new ZipOutputStream(new FileOutputStream(outFile));
            //判断是否是一个文件或目录
            //如果是文件则压缩
            if (fileOrDirectory.isFile()){
                zipFileOrDirectory(out,fileOrDirectory, "");
            } else {
                //否则列出目录中的所有文件递归进行压缩

                File[]entries = fileOrDirectory.listFiles();
                for (int i= 0; i < entries.length;i++) {
                    zipFileOrDirectory(out,entries[i],fileOrDirectory.getName()+"/");//传入最外层目录名

                }
            }
        }catch(IOException ex) {
            ex.printStackTrace();
        }finally{
            if (out!= null){
                try {
                    out.close();
                }catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void zipFileOrDirectory(ZipOutputStream out, File fileOrDirectory, String curPath)throws IOException {
        FileInputStream in = null;
        try {
            //判断是否为目录
            if (!fileOrDirectory.isDirectory()){
                byte[] buffer= new byte[4096];
                int bytes_read;
                in= new FileInputStream(fileOrDirectory);//读目录中的子项
                //归档压缩目录
                ZipEntry entry = new ZipEntry(curPath + fileOrDirectory.getName());//压缩到压缩目录中的文件名字
                //getName() 方法返回的路径名的名称序列的最后一个名字，这意味着表示此抽象路径名的文件或目录的名称被返回。
                //将压缩目录写到输出流中
                out.putNextEntry(entry);//out是带有最初传进的文件信息，一直添加子项归档目录信息
                while ((bytes_read= in.read(buffer))!= -1) {
                    out.write(buffer,0, bytes_read);
                }
                out.closeEntry();
            } else {
                //列出目录中的所有文件
                File[]entries = fileOrDirectory.listFiles();
                for (int i= 0; i < entries.length;i++) {
                    //递归压缩
                    zipFileOrDirectory(out,entries[i],curPath + fileOrDirectory.getName()+ "/");//第一次传入的curPath是空字符串
                }//目录没有后缀所以直接可以加"/"
            }
        }catch(IOException ex) {
            ex.printStackTrace();
        }finally{
            if (in!= null){
                try {
                    in.close();
                }catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}