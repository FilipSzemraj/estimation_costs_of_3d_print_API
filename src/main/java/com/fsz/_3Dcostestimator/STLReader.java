package com.fsz._3Dcostestimator;

 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.charset.StandardCharsets;
 import java.util.List;
 import java.util.ArrayList;
 import java.io.*;
 import java.util.Scanner;

class Triangle {
    private final float[] vertex1;
    private final float[] vertex2;
    private final float[] vertex3;

    // Constructor that takes the coordinates for three vertices
    public Triangle(float[] v1, float[] v2, float[] v3) {
        this.vertex1 = v1;
        this.vertex2 = v2;
        this.vertex3 = v3;
    }

    // Getter methods for each vertex
    public float[] getVertex1() {
        return vertex1;
    }

    public float[] getVertex2() {
        return vertex2;
    }

    public float[] getVertex3() {
        return vertex3;
    }

    // You might add methods here to calculate the area, perimeter, or other properties of the triangle
    // Example method to calculate the area of the triangle
    public double calculateArea() {
        double area = Math.abs(vertex1[0]*(vertex2[1]-vertex3[1]) + vertex2[0]*(vertex3[1]-vertex1[1]) + vertex3[0]*(vertex1[1]-vertex2[1])) / 2.0;
        return area;
    }
}

public class STLReader {
    private InputStream fileStream;
    private boolean isBinary;
    private ArrayList<Triangle> triangles = new ArrayList<>();


    public STLReader(File file) throws IOException{
        try {
            this.fileStream = new BufferedInputStream(new FileInputStream(file));
            this.isBinary = isBinary();
            System.out.println("The file is " + (isBinary ? "binary." : "not binary."));
        }catch (IOException e) {
            System.err.println("Failed to open or read from the file (constructor): " + e.getMessage());
            this.fileStream = null;
            this.isBinary = false;
        }

    }
    public boolean isBinary(){ //Direct buffer if the read operation will be a performance bottleneck
        if(fileStream == null){
            return false;
        }
        try {
            fileStream.mark(80);
            byte[] headerBytes = new byte[80];
            int readBytes = fileStream.read(headerBytes);
            fileStream.reset();

            if (readBytes == -1) {
                return true;
            }
            String header = new String(headerBytes, StandardCharsets.ISO_8859_1);
            return !header.startsWith("solid");
        }catch(IOException e){
            System.err.println("Error while reading/checking type of the file (isBinary): " + e.getMessage());
            return false;
        }
    }

    private void readHeader(){
        try {
            this.fileStream.skip(80);
        }catch(IOException e){
            System.err.println("Error while skipping the binary header (readHeader)"+e.getMessage());
        }
    }

    private int readLength() throws IOException {
        byte[] buffer = new byte[4];
        this.fileStream.read(buffer);
        return java.nio.ByteBuffer.wrap(buffer).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private void countTriangles(String filename){
        if(this.isBinary){

        }else{

        }
    }
    public static void main(String[] args) throws IOException {
        //String filename = "src/main/resources/astronaut.stl"; // Adjust the file path as necessary
        String filename = "src/main/resources/VICTORY.stl"; // Adjust the file path as necessary
        File file = new File(filename);
        STLReader stlReader = new STLReader(file);
    }


}
