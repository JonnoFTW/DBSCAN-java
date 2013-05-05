package dbscan_gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * Adapted from http://algs4.cs.princeton.edu/92search/QuadTree.java.html
 * Please ignore the high degree of coupling with the Point class.
 * Points with the same x,y location are stored in the same QuadTreeNode.
 * Could be extended to map points to objects.
 * @author Jonathan
 *
 */
public class QuadTree implements Iterable<Point> {

    QuadTreeNode root;
    private int size = 0;
    /**
     * Constructs an empty QuadTree
     */
    public QuadTree() {
    }
    /**
     * Constructs a Quadtree from the points in the collection
     * Should construct the tree in a balanced fashion
     * @param points the collection of points to add
     */
    public QuadTree(ArrayList<Point> points) {
        if(points == null || points.isEmpty())
            return;
        Collections.sort(points,new Comparator<Point>() {

            @Override
            public int compare(Point p, Point q) {
                if(p.coordinates[0] == q.coordinates[0])
                    return p.coordinates[1] - q.coordinates[1];
                else
                    return p.coordinates[0] - q.coordinates[0];
            }
        });   
        int median = points.size()/2 -1;
        add(points.get(median));
        for (int i = 1; i <= median ; i++) {
            add(points.get(median+i));
            add(points.get(median-i));
        }
        add(points.get(points.size()-1));
    }
    public QuadTree(ArrayList<Point> points, Comparator<Point> cmp) {
        if(points == null || points.isEmpty())
            return;
        Collections.sort(points,cmp);
        
        for (Point point : points) {
            add(point);
        }
    }
    /**
     * Inserts the point p into the Quadtree.
     * The very first inserted node becomes the root node
     * because no balancing is performed.
     * @param p the point to insert
     */
    public void add(Point p) {
        size++;
        root = insert(root,p);
    }
    /**
     * Recursively inserts to the point p into the current QuadTreeNode, if the node is 
     * null, the point is inserted and becomes a new leaf node, otherwise the point is inserted
     * into a child quadrant of the current node.
     * @param h
     * @param p
     * @return
     */
    private QuadTreeNode insert(QuadTreeNode h, Point p) {
        if(h== null) return new QuadTreeNode(p);
        else if ( lessX(p,h) &&  lessY(p,h)) h.SW = insert(h.SW, p);
        else if ( lessX(p,h) && !lessY(p,h)) h.NW = insert(h.NW, p);
        else if (!lessX(p,h) &&  lessY(p,h)) h.SE = insert(h.SE, p);
        else if (!lessX(p,h) && !lessY(p,h)) h.NE = insert(h.NE, p);
        return h;
    }
    /**
     * @param p
     * @param q
     * @return
     */
    private boolean lessX(Point p, QuadTreeNode h) {
        return p.coordinates[0] < h.x;
    }
    /**
     * @param p
     * @param q
     * @return
     */
    private boolean lessY(Point p, QuadTreeNode h) {
        return p.coordinates[1] < h.y;
    }
    private boolean eq(Point p, QuadTreeNode h) {
        return p.coordinates[0] == h.x && p.coordinates[1] == h.y;
    }
    /**
     * 
     * @param p
     * @return
     */
    public boolean contains(Point p) {
        return contains(root, p);
    }
    private boolean contains(QuadTreeNode h, Point p) {
        if(h==null) {
            return false;
        } else if(eq(p,h)) {
            return true;
        } else {
            if      ( lessX(p,h) &&  lessY(p,h)) return contains(h.SW, p);
            else if ( lessX(p,h) && !lessY(p,h)) return contains(h.NW, p);
            else if (!lessX(p,h) &&  lessY(p,h)) return contains(h.SE, p);
            else if (!lessX(p,h) && !lessY(p,h)) return contains(h.NE, p);
        }
        return false;
    }
    /**
     * Adds all neighours of p from the tree to the point
     * @param epsilon
     * @param p
     */
    public void queryCircle(int epsilon, Point p) {
        query2D(root, p, epsilon);
    }
    /**
     * @param h
     * @param p
     * @param r
     */
    private void query2D(QuadTreeNode h, Point p, int r) {
        if(h==null) return;
        int px = p.coordinates[0];
        int py = p.coordinates[1];
        int xmin = px - r;
        int xmax = px + r;
        int ymin = py - r;
        int ymax = py + r;
        if(p.distance(h.value.get(0)) <= r) {
            p.addNeighbours(h.value);
        }
        if ( (xmin < h.x) &&  (ymin < h.y)) query2D(h.SW, p, r);
        if ( (xmin < h.x) && !(ymax < h.y)) query2D(h.NW, p, r);
        if (!(xmax < h.x) &&  (ymin < h.y)) query2D(h.SE, p, r);
        if (!(xmax < h.x) && !(ymax < h.y)) query2D(h.NE, p, r);
    }
  
    /**
     * A node in the QuadTree, has 4 subregions that are axis aligned.
     * 
     * @author Jonathan
     *
     */
    private class QuadTreeNode {
        ArrayList<Point> value = new ArrayList<Point>(3);
        QuadTreeNode NW,NE,SW,SE;
        int x,y;
        public QuadTreeNode(Point p) {
            value.add(p);
            x = p.coordinates[0];
            y = p.coordinates[1];
        }
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        int epsilon = 2000;
        int runs = 10;
        System.out.println("Epsilon: "+epsilon);
        for (String string : new String[]{"a1.txt","a2.txt","a3.txt","pathological"}) {
            System.out.println(string+"------------------");
            HashMap<String, Double> stats = new HashMap<String, Double>();
            stats.put("arbitrary", 0.0);
            stats.put("sorted", 0.0);
            stats.put("zorder", 0.0);
            
            for (int i = 0; i < runs; i++) {
                    ArrayList<Point> points = new ArrayList<Point>();
                    
                    try {
                        Scanner in = new Scanner(new File("data/"+string));
                        while(in.hasNext()) {
                            points.add(new Point(in.nextInt(),in.nextInt()));
                        }
                    } catch (FileNotFoundException e) {
                     // Generate a pathological dataset (1,1),(2,2),(3,3)
                        for(int j=0;j<3000;j++) {
                            points.add(new Point(j,j));
                            
                        }
                    }
                    QuadTree tree = new QuadTree();
                    for (Point point : points) {
                        tree.add(point);
                    }
                    stats.put("arbitrary",new Double(stats.get("arbitrary")+(timeNeighbouring(tree, epsilon))));
                    tree = new QuadTree(points);
                    stats.put("sorted",new Double(stats.get("sorted")+(timeNeighbouring(tree, epsilon))));
                    
                    tree = new QuadTree(points,new Comparator<Point>() {
        
                        @Override
                        public int compare(Point p, Point q) {
                            int j = 0, k = 0, x =0;
                            for (int i = 0; i < p.coordinates.length; i++) {
                                int y = p.coordinates[k] ^ q.coordinates[k];
                                if(x < y && x <(x^y)) {
                                    j = k;
                                    x = y;
                                }
                            }
                            return p.coordinates[j] - q.coordinates[j];
                        }
                    });
                    stats.put("zorder",new Double(stats.get("zorder")+(timeNeighbouring(tree, epsilon))));
                    
            }
            
            
            for (Entry<String, Double> entry : stats.entrySet()) {
                System.out.println(entry.getKey()+": "+ (entry.getValue()/runs));
            }
        }
            
    }
    public static double timeNeighbouring(QuadTree tree,int epsilon) {
        double start = System.nanoTime();
        for (Point p : tree) {
            tree.queryCircle(epsilon,p);
            p.clearNeighbours();
        }
        return (System.nanoTime()-start) /1000000000.0;
    }
    /**
     * @return
     */
    public int size() {
        return size;
    }
    public int maxDepth()  {
        return maxDepth(root,0);
    }
    private int maxDepth(QuadTreeNode h, int depth) {
        if(h == null)
            return depth;
        else
            return Math.max(
                    Math.max(maxDepth(h.NE, depth+1),maxDepth(h.NW, depth+1)),
                    Math.max(maxDepth(h.SE, depth+1),maxDepth(h.SW, depth+1))
                   );
    }
    public ArrayList<Point> allPoints() {
        ArrayList<Point> points = new ArrayList<Point>(size);
        allPoints_(root,points);
        return points;
    }
    private void allPoints_(QuadTreeNode node, ArrayList<Point> points) {
        if(node != null) {
            points.addAll(node.value);
            allPoints_(node.NE, points);
            allPoints_(node.NW, points);
            allPoints_(node.SE, points);
            allPoints_(node.SW, points);
        } else {
            return;
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     * Uses O(n) space because I can't figure out how to write an iterator.
     */
    @Override
    public Iterator<Point> iterator() {
        return allPoints().iterator();
        //return new QuadTreeIterator();
    }
    /**
     * Adapted from http://www.merl.com/reports/docs/TR2002-41.pdf
     * @author Jonathan
     *
     
    private class QuadTreeIterator implements Iterator<Point> {

        private QuadTreeNode next;
        public QuadTreeIterator() {
            next = root;
        }
        /** {@inheritDoc} 
        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Point next() {
            int 
            Point p = next.location;
            next = next.NE;
            return p;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("The remove() operations is not supported by this Iterator.");
        }
        
    }
    */
    
}
