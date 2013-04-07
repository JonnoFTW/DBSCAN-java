package dbscan_gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Point {
    
    final HashSet<Point> neighbours = new HashSet<Point>();
    int[] points;
    boolean visited = false;
    public Point(ArrayList<Integer> ps) {
        points = new int[ps.size()];
        for (int i = 0; i < points.length; i++) {
            points[i] = ps.get(i);
        }
    }
    public Point(int... is) {
        this.points = is;
    }
    public String toString() {
        return Arrays.toString(points);
    }

    public double distance(Point p) {
        double sum = 0;
        for (int i = 0;i < points.length;i++) {
            sum += Math.pow(points[i] - p.points[i],2);
        }
        return Math.sqrt(sum);
    }
    public void addNeighbours(ArrayList<Point> ps) {
        neighbours.addAll(ps);
    }
    public void addNeighbour(Point p) {
        if (p != this)
            neighbours.add(p);
    }
    public int neighbourCount() {
        return neighbours.size();
    }
    public static void main(String[] args) {
        Point p = new Point(2,2);
        
        Point p1 = new Point(2,2);
        System.out.print(p+"->"+p1+" ");
        System.out.println(p.distance(p1));
    }
}
