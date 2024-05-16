package com.fsz._3Dcostestimator;

 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.charset.StandardCharsets;
 import java.util.List;
 import java.util.ArrayList;
 import java.io.*;
 import java.util.Scanner;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;

 class surfaceThicknessException extends Exception{
     public surfaceThicknessException(String message){
         super(message);
     }
 }

class Triangle {
    private final float[] vertex1;
    private final float[] vertex2;
    private final float[] vertex3;

    public Triangle(float[] v1, float[] v2, float[] v3) {
        this.vertex1 = v1;
        this.vertex2 = v2;
        this.vertex3 = v3;
    }

    public float[] getVertex1() {
        return vertex1;
    }

    public float[] getVertex2() {
        return vertex2;
    }

    public float[] getVertex3() {
        return vertex3;
    }

    public double calculateArea() {
        float ax = vertex2[0] - vertex1[0], ay = vertex2[1] - vertex1[1], az = vertex2[2] - vertex1[2];
        float bx = vertex3[0] - vertex1[0], by = vertex3[1] - vertex1[1], bz = vertex3[2] - vertex1[2];

        float cx = ay * bz - az * by;
        float cy = az * bx - ax * bz;
        float cz = ax * by - ay * bx;

        double area = 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
        return area;
    }

    public double calculateVolume(){
        double v321 = vertex3[0] * vertex2[1] * vertex1[2];
        double v231 = vertex2[0] * vertex3[1] * vertex1[2];
        double v312 = vertex3[0] * vertex1[1] * vertex2[2];
        double v132 = vertex1[0] * vertex3[1] * vertex2[2];
        double v213 = vertex2[0] * vertex1[1] * vertex3[2];
        double v123 = vertex1[0] * vertex2[1] * vertex3[2];
        return (1.0 / 6.0) * (-v321 + v231 + v312 - v132 - v213 + v123);

    }
}

public class STLReader {
    private InputStream fileStream;
    private boolean isBinary;
    private ArrayList<Triangle> triangles = new ArrayList<>();
    private double surfaceArea;


    public STLReader(File file) throws IOException{
        try {
            this.fileStream = new BufferedInputStream(new FileInputStream(file));
            this.isBinary = isBinary();
            System.out.println("The file is " + (isBinary ? "binary." : "not binary."));
            countTriangles();
            surfArea();
            double[] volumeAndWeight = this.calculateVolume("cm", 1.24, 5, 0.087);
            System.out.println("Total volume is: "+volumeAndWeight[0]+", and weight: "+volumeAndWeight[1]);

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

    private float[] unpackFloats(int numberOfFloats) throws IOException {
        byte[] buffer = new byte[4*numberOfFloats];
        fileStream.read(buffer);
        float[] result = new float[numberOfFloats];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < numberOfFloats; i++) {
            result[i] = byteBuffer.getFloat(i * 4);
        }
        return result;
    }

    private void unpackSkip(int numberOfBytes) throws IOException{
        byte[] buffer = new byte[numberOfBytes];
        fileStream.read(buffer);
    }
    public Triangle readTriangle() throws IOException {
        unpackSkip(12);
        float[] p1 = unpackFloats(3);
        float[] p2 = unpackFloats(3);
        float[] p3 = unpackFloats(3);
        unpackSkip(2);

        return new Triangle(p1, p2, p3);
    }

    private Triangle readAsciiTriangle(List<String> lines, int index) {
        Pattern p = Pattern.compile("[-+]?\\d*\\.\\d+|\\d+");
        Matcher matcher;
        float[] p1 = new float[3], p2 = new float[3], p3 = new float[3];

        for (int j = 0; j < 3; j++) {
            matcher = p.matcher(lines.get(index + 1 + j));
            for (int k = 0; matcher.find() && k < 3; k++) {
                if (j == 0) p1[k] = Float.parseFloat(matcher.group());
                if (j == 1) p2[k] = Float.parseFloat(matcher.group());
                if (j == 2) p3[k] = Float.parseFloat(matcher.group());
            }
        }

        return new Triangle(p1, p2, p3);
    }

    private double surfArea(){
        double area =0;
        for(Triangle triangle : triangles){
            area += triangle.calculateArea();
        }
        area = area / 100;
        System.out.println("Total area: " + area + " cm^2");
        this.surfaceArea=area;
        return area;
    }



    public double[] calculateVolume(String unit, double materialMass, double infillPercentage, double surfaceThickness){
        //Because of the infill pattern, mass can vary across different ranges of patterns.
        double totalVolume = 0;
        try {
            double[] returnedValue = new double[2];

            for (Triangle triangle : triangles) {
                totalVolume += triangle.calculateVolume();
            }
            totalVolume = totalVolume / 1000;

            double shellVolume = this.surfaceArea * surfaceThickness;
            if (shellVolume > totalVolume) {
                throw new surfaceThicknessException("Surface thickness too thick. Reduce surface thickness.");
            }
            double interiorVolume = totalVolume - shellVolume;

            double effectiveVolume = interiorVolume * (infillPercentage / 100);
            effectiveVolume += shellVolume;

            returnedValue[0] = totalVolume;
            returnedValue[1] = effectiveVolume * materialMass;
            return returnedValue;
        }catch(surfaceThicknessException e){
            System.err.println("Error while calculating volume (calculateVolume) "+e.getMessage());
            return new double[]{totalVolume, 0};  // Return totalVolume converted and mass as 0

        }
    }

    private void countTriangles(){
        try {
            if (this.isBinary) {
                readHeader();
                int l = readLength();
                for(int i=0;i<l;i++){
                    triangles.add(readTriangle());
                }
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream));
                List<String> lines = new ArrayList<>();
                String line;

                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                reader.close();

                int i = 0;
                while (i < lines.size()) {
                    if (lines.get(i).trim().startsWith("facet")) {
                        triangles.add(readAsciiTriangle(lines, i));
                        i += 7;
                    } else {
                        i++;
                    }
                }
            }
            System.out.println("Total number of triangles processed: " + triangles.size());

        }catch(IOException e){
            System.err.println("Error while counting triangles (countTriangles)"+e.getMessage());
        }
    }
    public static void main(String[] args) throws IOException {
        String filename = "src/main/resources/astronaut.stl";
        //String filename = "src/main/resources/VICTORY.stl";
        File file = new File(filename);
        STLReader stlReader = new STLReader(file);
    }


}
