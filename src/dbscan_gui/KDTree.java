package dbscan_gui;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;


public class KDTree {
    KDTreeNode root;
    PointComparator[] comps;
    public KDTree(ArrayList<Point> list) {
        int axes = list.get(0).coordinates.length;
        
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
            return p1.coordinates[axis] - p2.coordinates[axis];
        }
        
    }


    /**
     * 
     * @return an arraylist of all points in the tree
     */
    public ArrayList<Point> getNodes() {
        return root.allNodes();
    }
    /**
     * Adapted from https://code.google.com/p/python-kdtree/
     * Stores points in a tree, sorted by axis
     * @author Jonathan
     *
     */
    public class KDTreeNode {
        KDTreeNode leftChild = null;
        KDTreeNode rightChild = null;
        Point location;
        boolean visited = false;
        
        /**
         * @param list the list to make the tree from
         */
        public KDTreeNode(ArrayList<Point> list, int depth) {
            if(list.isEmpty())
                return;
            final int axis = depth % (list.get(0).coordinates.length);
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
    private void nearestNeighbours_(KDTreeNode node, Point queryPoint, int depth, KDNeighbours bestNeighbours) {
        if(node == null)
            return;
        if(node.isLeaf()) {
            bestNeighbours.add(node.location);
            return;
        }
        int axis = depth % (queryPoint.coordinates.length);
        KDTreeNode nearSubtree = node.rightChild;
        KDTreeNode farSubtree  = node.leftChild;
        if(queryPoint.coordinates[axis] <= node.location.coordinates[axis]) {
            nearSubtree = node.leftChild;
            farSubtree = node.rightChild;
        }
        nearestNeighbours_(nearSubtree, queryPoint,  depth+1, bestNeighbours);
        if(node.location != queryPoint) 
            bestNeighbours.add(node.location);       
     //   if(Math.pow(node.location.points[axis] - queryPoint.points[axis],2) <= bestNeighbours.largestDistance) 
            nearestNeighbours_(farSubtree, queryPoint, depth+1,bestNeighbours);
        
        return;
    }
    public ArrayList<Point> rangeSearch(Point queryPoint, int epsilon) {
        ArrayList<Point> neighbours = new ArrayList<Point>();
        rangeSearch(root, queryPoint, epsilon, neighbours, 0);
        return neighbours;
    }
    private void rangeSearch(KDTreeNode node,Point queryPoint, int epsilon, ArrayList<Point> neighbours,int depth) {
        if(queryPoint == null || node == null)
            return;
        node.visited = true;
        if(node.location.distance(queryPoint) <= epsilon && queryPoint != node.location)  {
            neighbours.add(node.location);
        }
        int axis =  depth % (queryPoint.coordinates.length);
        // check dim
        KDTreeNode nearSubtree = node.rightChild;
        KDTreeNode farSubtree  = node.leftChild;
        if(queryPoint.coordinates[axis] < node.location.coordinates[axis]) {
            nearSubtree = node.leftChild;
            farSubtree = node.rightChild;
        }
        rangeSearch(nearSubtree, queryPoint,  epsilon, neighbours,depth+1);
               
        if(Math.pow(node.location.coordinates[axis] - queryPoint.coordinates[axis],2) <= Math.sqrt(epsilon)) 
            rangeSearch(farSubtree, queryPoint, epsilon,neighbours,depth+1);
        
      
    }
    private int countVisited(KDTreeNode node) {
        if(node == null)
            return 0;
        if(node.visited)
            return countVisited(node.leftChild) + countVisited(node.rightChild) + 1;
        else
            return countVisited(node.leftChild) + countVisited(node.rightChild);
    }

    /**
     * Private data structure for holding the neighbours of a point
     * @author Jonathan
     *
     */
    private class KDNeighbours {
        Point queryPoint;
        double largestDistance = 0;
        int t;
        TreeSet<Tuple> currentBest = new TreeSet<Tuple>(new Comparator<Tuple>() {
            @Override
            public int compare(Tuple o1, Tuple o2) {
                return (int) (o1.y-o2.y);
            }
        });
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
         * @return the t nearest neighbours to the query point
         */
        public ArrayList<Point> getNBest(int t) {
            //System.out.println(currentBest);
            ArrayList<Point> best = new ArrayList<Point>();
            Iterator<Tuple> it = currentBest.iterator();
            int count = 0;
            while(it.hasNext()){
                if(count==t)
                    break;
                best.add(it.next().x);
                count++;
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
            Iterator<Tuple> it = currentBest.iterator();
            while(it.hasNext()) {
                Tuple tu =it.next();
                if(tu.y > epsilon)
                    break;
                else if(tu.x != queryPoint)
                    best.add(tu.x);
            }
            return best;
        }
        
        public void add(Point p) {
            double dist = p.distance(queryPoint);
            currentBest.add(new Tuple(p, dist));
            calculateLargest();
        }
        private void calculateLargest() {
            largestDistance = currentBest.last().y;
           // System.out.println("Largest candidate distance for"+queryPoint+" ->"+currentBest.last());
            /*
            if(t >= currentBest.size())
                largestDistance = currentBest.last().y;
            else {
                Iterator<Tuple> it = currentBest.iterator();
                int c = 0;
                while(it.hasNext()) {
                    Tuple tu = it.next();
                    if(c==t){
                        largestDistance = tu.y;
                        break;
                    }
                }
            }*/
        }
        private class Tuple  {
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
        int epsilon = 3;
        
        System.out.println("Epsilon: "+epsilon);
        ArrayList<Point> points = new ArrayList<Point>();
        Random r = new Random();
        for (int i = 0; i < 21; i++) {
            points.add(new Point(r.nextInt(10),r.nextInt(10)));
        }
        
        System.out.println("Points "+points );
        System.out.println("----------------");
        System.out.println("Neighbouring Kd");
        KDTree tree = new KDTree(points);
        for (Point p : points) {
            
            ArrayList<Point> neighbours = tree.rangeSearch(p,epsilon);
           // for (Point q : neighbours) { q.addNeighbour(p);}
            p.addNeighbours(neighbours);
            p.printNeighbours();
            p.clearNeighbours();
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
            point.printNeighbours();
        }

    }
}