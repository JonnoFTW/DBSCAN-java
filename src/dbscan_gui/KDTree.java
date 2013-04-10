package dbscan_gui;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;


public class KDTree {
    KDTreeNode root;
    PointComparator[] comps;
    public KDTree(ArrayList<Point> list) {
        int axes = list.get(0).points.length;
        
        comps = new PointComparator[axes];
        for (int i = 0; i < axes; i++) {
            comps[i] = new PointComparator(i);
        }
        root = new KDTreeNode(list,0);
    }
    private class PointComparator implements Comparator<Point> {
        private int axis;
        public PointComparator(int axis) {
            this.axis = axis;
        }
        @Override
        public int compare(Point p1, Point p2) {
            return p1.points[axis] - p2.points[axis];
        }
        
    }
    /**
     * Adapted from https://code.google.com/p/python-kdtree/
     * Stores points in a tree, sorted by axis
     * @author Jonathan
     *
     */
    public ArrayList<Point> getNodes() {
        return root.allNodes();
    }
    public class KDTreeNode {
        KDTreeNode leftChild = null;
        KDTreeNode rightChild = null;
        Point location;
        
        /**
         * @param list the list to make the tree from
         */
        public KDTreeNode(ArrayList<Point> list, int depth) {
            if(list.isEmpty())
                return;
            final int axis = depth % (list.get(0).points.length);
            
            Collections.sort(list, comps[axis] );
            int median = list.size()/2;
            location = list.get(median);
            List<Point> leftPoints = list.subList(0, median);
            List<Point> rightPoints = list.subList(median+1, list.size());
            if(!leftPoints.isEmpty())
                leftChild  = new KDTreeNode(new ArrayList<Point>(leftPoints), depth+1);
            if(!rightPoints.isEmpty())
                rightChild = new KDTreeNode(new ArrayList<Point>(rightPoints),depth+1);
        }

        /**
         * 
         * @param p
         */
        public void add(Point p) {
            throw new UnsupportedOperationException();
        }
        /**
         * @return true if this node has no children
         */
        public boolean isLeaf() {
            return leftChild == null && rightChild == null;
        }
        /**
         * @return all points in the kdtree in no particular order.
         */
        public ArrayList<Point> allNodes() { 
            ArrayList<Point> points = new ArrayList<Point>();
            points.add(location);
            if(leftChild != null)
                points.addAll(leftChild.allNodes());
            if (rightChild != null)
                points.addAll(rightChild.allNodes());
            return points;
        }

    }
    
    
    /**
     * Finds the nearest n neighbours to the given point
     * @param queryPoint the point you want the neighbours of
     * @param count the amount of neighbours wanted
     * @return
     */
    public ArrayList<Point> nearestNNeighbours(Point queryPoint, int count) {
        KDNeighbours neighbours = new KDNeighbours(queryPoint,count);
        nearestNeighbours_(root, queryPoint, 0, neighbours);
        return neighbours.getNBest(count);
    } 
    /**
     * Finds the nearest neighbours of a point that fall within a given distance
     * @param queryPoint the point to find the neighbours of
     * @param epsilon the distance threshold
     * @return the list of points
     */
    public ArrayList<Point> nearestNeighbours(Point queryPoint, int epsilon) {
        KDNeighbours neighbours = new KDNeighbours(queryPoint, 1);
        nearestNeighbours_(root, queryPoint, 0, neighbours);
        return neighbours.getBest(epsilon);
    }
    /**
     * @param node
     * @param queryPoint
     * @param depth
     * @param bestNeighbours
     */
    private void nearestNeighbours_(KDTreeNode node, Point queryPoint, int depth,KDNeighbours bestNeighbours) {
        if(node == null)
            return;
        if(node.isLeaf() && queryPoint != node.location) {
            bestNeighbours.add(node.location);
            return;
        }
        int axis = depth % (queryPoint.points.length);
        KDTreeNode nearSubtree = node.rightChild;
        KDTreeNode farSubtree  = node.leftChild;
        if(queryPoint.points[axis] < node.location.points[axis]) {
            nearSubtree = node.leftChild;
            farSubtree = node.rightChild;
        }
        nearestNeighbours_(nearSubtree, queryPoint,  depth+1, bestNeighbours);
        if(node.location != queryPoint)
            bestNeighbours.add(node.location);       
        if(Math.pow(node.location.points[axis] - queryPoint.points[axis],2) <= bestNeighbours.largestDistance)
            nearestNeighbours_(farSubtree, queryPoint, depth+1,bestNeighbours);
        return;
    }
    /**
     * Private datastructure for holding the neighbours of a point
     * @author Jonathan
     *
     */
    private class KDNeighbours {
        Point queryPoint;
        double largestDistance = 0;
        int t;
        LinkedList<Tuple> currentBest = new LinkedList<Tuple>();
        /**
         * @param queryPoint
         * @param t
         */
        KDNeighbours(Point queryPoint, int t) {
            this.queryPoint = queryPoint;
            this.t = t;
        }
        /**
         * @param t
         * @return
         */
        public ArrayList<Point> getNBest(int t) {
            //System.out.println(currentBest);
            ArrayList<Point> best = new ArrayList<Point>();
            for (Tuple tp : currentBest.subList(0, t)) {
                best.add(tp.x);
            }
            return best;
        }
        
        /**
         * @param epsilon
         * @return
         */
        public ArrayList<Point> getBest(int epsilon) {
           // System.out.println(currentBest);
            ArrayList<Point> best = new ArrayList<Point>();
            for (Tuple tu : currentBest) {
               // System.out.println(tu.x+"->"+queryPoint+" "+tu.y);
                if(tu.y <= epsilon && tu.x != queryPoint)
                    best.add(tu.x);
            }
            return best;
        }
        /**
         * 
         */
        public void calculateLargest() {
            if(t >= currentBest.size())
                largestDistance = currentBest.getLast().y; 
            else
                largestDistance = currentBest.get(t-1).y;
        }
        /**
         * @param p
         */
        public void add(Point p) {
            double sd = p.distance(queryPoint);
            ListIterator<Tuple> it = currentBest.listIterator();
            int i = 0;
            while(it.hasNext()) {
                
                if(i == t)
                    return;
                if (it.next().y > sd) {
                    currentBest.add(i, new Tuple(p, sd));
                    calculateLargest();
                    return;
                }
                i++;
            }
            currentBest.offerLast(new Tuple(p,sd));
            calculateLargest();
        }
        /**
         * @author Jonathan
         *
         */
        private class Tuple {
            Point x;
            double y;
            Tuple(Point x, double y) {
                this.x = x;
                this.y = y;
            }
            public String toString() {
                return "["+x+","+y+"]";
            }
        }
    }

    /**
     * @param args
     */
    
    public static void main(String[] args) {
        int minpts = 5,epsilon = 3;
        
        System.out.println("Epsilon: "+epsilon);
        ArrayList<Point> points = new ArrayList<Point>();
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            points.add(new Point(r.nextInt(10), r.nextInt(10)));
        }
        System.out.println("Points "+points );
        System.out.println("Neighbour Kd");
        KDTree tree = new KDTree(points);
        System.out.println("----------------");
        for (Point p : points) {
            ArrayList<Point> neighbours = tree.nearestNeighbours(p, epsilon);
            p.addNeighbours(neighbours);
            
        }
        for (Point point : points) {
            System.out.println("Neighbours of "+point+" are: "+point.neighbours);
            point.neighbours.clear();
        }
        System.out.println("------------------");
        System.out.println("Neighbouring O(n^2)");
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                Point p = points.get(i), q = points.get(j);
                if (p.distance(q) <= epsilon) {
                    p.addNeighbour(q);
                    q.addNeighbour(p);
                }
            }
        }
        for (Point point : points) {
            System.out.println("Neighbours of "+point+" are: "+point.neighbours);
        }

    }
}