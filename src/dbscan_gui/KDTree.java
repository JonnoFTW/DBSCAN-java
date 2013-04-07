package dbscan_gui;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;


/**
 * Adapted from https://code.google.com/p/python-kdtree/
 * Stores points in a tree, sorted by axis
 * @author Jonathan
 *
 */
public class KDTree {
    KDTree leftChild = null;
    KDTree rightChild = null;
    Point location;
    
    /**
     * @param list the list to make the tree from
     */
    public KDTree(ArrayList<Point> list) {
        initialise(list,0);
    }
    /**
     * @param arrayList
     * @param depth
     */
    private KDTree(ArrayList<Point> arrayList, int depth) {
        initialise(arrayList, depth);
    }
    /**
     * Initialise the kd-tree with the given point at depth
     * @param list
     * @param depth
     */
    private void initialise(ArrayList<Point> list, int depth) {
        if(list.isEmpty())
            return;
        final int axis = depth % (list.get(0).points.length);
        
        Collections.sort(list, new Comparator<Point>() {

            @Override
            public int compare(Point o1, Point o2) {                
                int x = o1.points[axis];
                int y = o2.points[axis];
                return x-y;
            }
        });
        int median = list.size()/2;
        location = list.get(median);
        List<Point> leftPoints = list.subList(0, median);
        List<Point> rightPoints = list.subList(median+1, list.size());
        if(!leftPoints.isEmpty())
            leftChild  = new KDTree(new ArrayList<Point>(leftPoints), depth+1);
        if(!rightPoints.isEmpty())
            rightChild = new KDTree(new ArrayList<Point>(rightPoints),depth+1);
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
    /**
     * Finds the nearest n neighbours to the given point
     * @param queryPoint the point you want the neighbours of
     * @param count the amount of neighbours wanted
     * @return
     */
    public ArrayList<Point> nearestNNeighbours(Point queryPoint, int count) {
        KDNeighbours neighbours = new KDNeighbours(queryPoint,count);
        nearestNeighbours_(this, this.location, 0, neighbours);
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
        nearestNeighbours_(this, this.location, 0, neighbours);
        return neighbours.getBest(epsilon);
    }
    /**
     * @param node
     * @param queryPoint
     * @param depth
     * @param bestNeighbours
     */
    private static void nearestNeighbours_(KDTree node, Point queryPoint, int depth,KDNeighbours bestNeighbours) {
        if(node == null)
            return;
        if(node.isLeaf() && queryPoint != node.location) {
            bestNeighbours.add(node.location);
            return;
        }
        int axis = depth % (queryPoint.points.length);
        KDTree nearSubtree = node.rightChild;
        KDTree farSubtree  = node.leftChild;
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
        double t, largestDistance = 0;
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
                System.out.println(tu.x+"->"+queryPoint+" "+tu.y);
                if(tu.y <= epsilon && tu.x != queryPoint)
                    best.add(tu.x);
            }
            return best;
        }
        /**
         * 
         */
        public void calculateLargest() {
            int cbsize = currentBest.size();
            if(t >= cbsize)
                largestDistance = currentBest.getLast().y; 
            else
                largestDistance = currentBest.get((int) (t-1)).y;
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
        for (int i = 0; i < 5; i++) {
            points.add(new Point(r.nextInt(10), r.nextInt(10)));
        }
        System.out.println("Points "+points );
        System.out.println("Neighbour Kd");
        KDTree tree = new KDTree(points);
        KDTree.findNeighboursKD(points, epsilon);
        for (Point point : points) {
            System.out.println("Neighbours of "+point+" are: "+point.neighbours);
        }
        System.out.println("Neighbouring O(n^2)");
        KDTree.findNeighbours(points, epsilon);
        for (Point point : points) {
            System.out.println("Neighbours of "+point+" are: "+point.neighbours);
            point.neighbours.clear();
        }

    }
    private static void findNeighbours(ArrayList<Point> points,int epsilon) {
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                setNeighbours(points.get(i), points.get(j),epsilon);
            }
        }

    }
    private static void setNeighbours(Point p, Point q, int epsilon) {
        if (p.distance(q) <= epsilon) {
            p.addNeighbour(q);
            q.addNeighbour(p);
        }
    }
    private static void findNeighboursKD(ArrayList<Point> points,int epsilon) {
        KDTree tree = new KDTree(points);
        for (Point p : points) {
            p.addNeighbours(tree.nearestNeighbours(p, epsilon));
        }
    }
}
