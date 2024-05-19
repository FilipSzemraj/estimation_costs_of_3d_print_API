package com.fsz._3Dcostestimator;

 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.charset.StandardCharsets;
 import java.util.*;
 import java.io.*;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;

 class surfaceThicknessException extends Exception{
     public surfaceThicknessException(String message){
         super(message);
     }
 }

 class materialException extends Exception{
     public materialException(String message){
         super(message);
     }
 }

 class Materials{
    private final Map<Integer, Material> materialsDict;
    public Materials(){
        this.materialsDict = new HashMap<>();
        this.materialsDict.put(1, new Material("ABS", 1.04));
        this.materialsDict.put(2, new Material("PLA", 1.24));
    }
    public double getMaterialMass(int number) throws materialException {
        if(materialsDict.containsKey(number)){
            return materialsDict.get(number).mass();
        }else{
            throw new materialException("Given key of material doesn't exist, so the returned mass is equal to 1.0");
        }
    }

    public Map<Integer, Map<String, Double>> getListOfMaterials(){
        Map<Integer, Map<String, Double>> transformedMaterials = new HashMap<>();
        for(Map.Entry<Integer, Material> entry : materialsDict.entrySet()){
            int materialId = entry.getKey();
            Material material = entry.getValue();
            Map<String, Double> materialDetails = new HashMap<>();
            materialDetails.put(material.name(), material.mass());
            transformedMaterials.put(materialId, materialDetails);
        }
        return transformedMaterials;
    }

     private record Material(String name, double mass) {
     }
 }

class Triangle {
    private final float[] vertex1;
    private final float[] vertex2;
    private final float[] vertex3;
    private static float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
    private static float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
    private static float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

    public static float getMinX(){
        return minX;
    }
    public static float getMaxX(){
        return maxX;
    }
    public static float getMinY(){
        return minY;
    }
    public static float getMaxY(){
        return maxY;
    }
    public static float getMinZ(){
        return minZ;
    }
    public static float getMaxZ(){
        return maxZ;
    }

    public Triangle(float[] v1, float[] v2, float[] v3) {
        this.vertex1 = v1;
        this.vertex2 = v2;
        this.vertex3 = v3;

        updateBounds(v1);
        updateBounds(v2);
        updateBounds(v3);
    }
    private void updateBounds(float[] vertex) {
        if (vertex[0] < minX) minX = vertex[0];
        if (vertex[0] > maxX) maxX = vertex[0];
        if (vertex[1] < minY) minY = vertex[1];
        if (vertex[1] > maxY) maxY = vertex[1];
        if (vertex[2] < minZ) minZ = vertex[2];
        if (vertex[2] > maxZ) maxZ = vertex[2];
    }


    public double calculateArea() {
        float ax = vertex2[0] - vertex1[0], ay = vertex2[1] - vertex1[1], az = vertex2[2] - vertex1[2];
        float bx = vertex3[0] - vertex1[0], by = vertex3[1] - vertex1[1], bz = vertex3[2] - vertex1[2];

        float cx = ay * bz - az * by;
        float cy = az * bx - ax * bz;
        float cz = ax * by - ay * bx;

        return 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
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
    private final ArrayList<Triangle> triangles = new ArrayList<>();
    private double surfaceArea;
    private double[] volumeAndWeight;
    private double[] dimensions;


    public STLReader(InputStream inputStream, String unit, int materialId, int infillPercentage, double surfaceThickness){
        try {
            this.fileStream = new BufferedInputStream(inputStream);
            this.isBinary = isBinary();
            //System.out.println("The file is " + (isBinary ? "binary." : "not binary."));
            countTriangles();
            processSurfArea(unit);
            Materials materials = new Materials();
            volumeAndWeight = this.calculateVolume(unit, materials.getMaterialMass(materialId), infillPercentage, surfaceThickness);
            //System.out.println("Total volume is: "+volumeAndWeight[0]+", and weight: "+volumeAndWeight[1]);

            double xDimension = Math.round((Triangle.getMaxX() - Triangle.getMinX())*100)/100;
            double yDimension = Math.round((Triangle.getMaxY() - Triangle.getMinY())*100)/100;
            double zDimension = Math.round((Triangle.getMaxZ() - Triangle.getMinZ())*100)/100;

            dimensions = new double[]{xDimension, yDimension, zDimension};

            //System.out.println("Dimesions: "+xDimension+"x"+yDimension+"x"+zDimension);

        }catch (materialException e) {
            System.err.println("Error occured in STLReader construtor: "+e.getMessage());
            surfaceArea=0;
            volumeAndWeight = new double[]{0,0};
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

    private double processSurfArea(String unit){
        double area =0;
        for(Triangle triangle : triangles){
            area += triangle.calculateArea();
        }

        switch (unit.toLowerCase()) {
            case "cm":
                break;
            case "mm":
                area /= 100;
                break;
            case "m":
                area /= 1000;
                break;
            case "in":
                area *= 6.4516;
                break;
            default:
                System.err.println("Unsupported unit: " + unit);
                break;
        }

        //System.out.println("Total area: " + area + " cm^2");
        this.surfaceArea=area;
        return area;
    }
    public double getSurfaceArea(){
        return surfaceArea;
    }
    public double[] getDimensions(){
        return dimensions;
    }

    public double getVolume(){
        return volumeAndWeight[0];
    }

    public double getWeight(){
        return volumeAndWeight[1];
    }
    public double[] calculateVolume(String unit, double materialMass, double infillPercentage, double surfaceThickness){
        //System.out.println("Mass "+materialMass+", thickness "+surfaceThickness);
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
            return new double[]{totalVolume, 0};

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
            //System.out.println("Total number of triangles processed: " + triangles.size());

        }catch(IOException e){
            System.err.println("Error while counting triangles (countTriangles)"+e.getMessage());
        }
    }
    public static void main(String[] args) {
        //String filename = "src/main/resources/astronaut.stl";
        //String filename = "src/main/resources/VICTORY.stl";
        //File file = new File(filename);
        //STLReader stlReader = new STLReader(file);
        Materials m = new Materials();
        //m.printListOfMaterials();
    }


}
