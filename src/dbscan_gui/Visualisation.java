package dbscan_gui;



import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferStrategy;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JCheckBox;
import javax.swing.SpinnerNumberModel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import javax.swing.JButton;

/**
 * A visualisation for the DBSCAN algorithm
 * @author Jonathan
 *
 */
public class Visualisation extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1672939579062189545L;
    private JSpinner spinner_epsilon;
    private JSpinner spinner_minpts;
    private QuadTree points = new QuadTree();
    private ArrayList<HashSet<Point>> clusters = new ArrayList<HashSet<Point>>();
    private HashSet<Point> noise = new HashSet<Point>();
    private int epsilon = 5000;
    private int minpts = 100;
    private JTextArea log;
    private boolean points_loaded = false;
    private ClusterRunner clusterRunner = new ClusterRunner();
    private NeighbourRunner neighbourRunner = new NeighbourRunner();
    private JToggleButton tglbtnStart;
    private Display visualisation;
    private JButton btnRecalculateClusters;
    private JFileChooser fc;
    private JProgressBar progressBar;
    private int maxX, maxY;

    /**
     * Most of this is generated by a GUI Builder (window builder pro)
     */
    public Visualisation(final JFrame window) {
        JPanel contents = new JPanel();
        window.setMinimumSize(new Dimension(1392,1000));
        window.setMaximumSize(new Dimension(1492, 1052));
        visualisation = new Display();
        contents.add(visualisation, BorderLayout.CENTER);
        window.getContentPane().add(contents);
        contents.setLayout(new BorderLayout(0, 0));
        
        JPanel display = new JPanel();
        contents.add(display, BorderLayout.CENTER);
        display.setLayout(new BorderLayout());
        display.add(visualisation);
        JPanel inputs = new JPanel();
        contents.add(inputs, BorderLayout.EAST);
        inputs.setLayout(new MigLayout("", "[31px,grow][grow]", "[14px][][][][][][][][][][][grow]"));
        inputs.setMinimumSize(new Dimension(200,1000));
        
        JLabel lblInputs = new JLabel("Inputs");
        inputs.add(lblInputs, "cell 0 0,alignx left,aligny top");

        JLabel lblEpsilon = new JLabel("Epsilon");
        inputs.add(lblEpsilon, "cell 0 1");

        spinner_epsilon = new JSpinner();
        final SpinnerNumberModel epsilonNumberModel = new SpinnerNumberModel(
                new Integer(epsilon), new Integer(0), null, new Integer(30));
        spinner_epsilon.setModel(epsilonNumberModel);
        spinner_epsilon.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                epsilon = (Integer) epsilonNumberModel.getNumber();
                btnRecalculateClusters.setEnabled(false);
            }
        });
        inputs.add(spinner_epsilon, "cell 1 1,growx");

        JLabel lblMinPoints = new JLabel("Min Points");
        inputs.add(lblMinPoints, "cell 0 2");

        final SpinnerNumberModel minptsNumberModel = new SpinnerNumberModel(
                new Integer(minpts), new Integer(1), null, new Integer(1));
        spinner_minpts = new JSpinner();
        spinner_minpts.setModel(minptsNumberModel);
        inputs.add(spinner_minpts, "cell 1 2,growx");
        spinner_minpts.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                minpts = (Integer) minptsNumberModel.getNumber();
                if(points_loaded) {
                    resetClusters();
                    btnRecalculateClusters.setEnabled(true);
                }
            }
        });
        
        JCheckBox chckbxDisplayNeighbourhood = new JCheckBox(
                "Display Neighbourhood");
        inputs.add(chckbxDisplayNeighbourhood, "cell 0 3 2 1");

        JButton btnReloadFile = new JButton("Load File");
        inputs.add(btnReloadFile, "cell 0 4 2 1,growx");
        btnReloadFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                loadPoints();
            }
        });
        JSpinner spinner = new JSpinner();
        final SpinnerNumberModel randomPointCountModel = new SpinnerNumberModel(new Integer(10), new Integer(1), null, new Integer(1));
        
        JButton btnExportClusters = new JButton("Export Clusters");
        btnExportClusters.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                exportClusters();
            }
        });
        inputs.add(btnExportClusters, "cell 0 5 2 1,growx");
        spinner.setModel(randomPointCountModel);
        inputs.add(spinner, "cell 1 6,growx");
        JButton btnLoadRandomPoints = new JButton("Load Random Points");
        btnLoadRandomPoints.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                loadRandomPoints((Integer) randomPointCountModel.getNumber());
            }
        });
        inputs.add(btnLoadRandomPoints, "cell 0 6,growx");

        btnRecalculateClusters = new JButton("Recalculate Clusters");
        btnRecalculateClusters.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                runClustering();
            }
        });
        
        JButton parameterButton = new JButton("Calculate Parameters");
        parameterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                BufferedWriter csv;
                try {
                    csv = new BufferedWriter(new FileWriter("test.csv"));
                    csv.write("epsilon,minpts,clusters\n");
                    for (epsilon = 0; epsilon < 5000; epsilon+=50) {
                        runNeighbouring();
                        for (minpts = 10; minpts < 200; minpts+=5) {
                            runClustering();
                            csv.write(epsilon+","+minpts+","+clusters.size()+"\n");
                        }
                    }
                    csv.close();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        inputs.add(parameterButton, "cell 0 7 2 1,growx");
        inputs.add(btnRecalculateClusters, "cell 0 8 2 1,growx");

        tglbtnStart = new JToggleButton("Start");
        inputs.add(tglbtnStart, "cell 0 9 2 1,growx");

        tglbtnStart.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (tglbtnStart.isSelected()) {
                    if (!points_loaded) {
                        loadPoints();
                    }
                    if(points_loaded) {
                        tglbtnStart.setText("Stop");
                        // Start the simulation
                        runNeighbouring();
                    } else {
                        tglbtnStart.setEnabled(false);
                    }
                } else {
                    tglbtnStart.setText("Start");
                    // Start the timer
                    stopNeighbouring();
                }
                
            }
        });

        progressBar = new JProgressBar(0,100);
        progressBar.setStringPainted(true);
        inputs.add(progressBar, "cell 0 10 2 1,growx");
        log = new JTextArea();
        log.setColumns(48);
      //  log.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(log);
        scrollPane.setViewportView(log);
        DefaultCaret caret = (DefaultCaret) log.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        inputs.add(scrollPane, "cell 0 11 2 1,grow");
        
        contents.revalidate();
        clearCollections();
        fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
    }
    
    
    /**
     * Exports clusters to a file of the user's choosing. clusters are denoted with a \#n where n
     * is the number of the cluster. It is then followed by all points in that cluster
     */
    protected void exportClusters() {
        String fname = fc.getSelectedFile().getAbsolutePath();
        File newFile = new File(fname+".cluster");
        fc.setSelectedFile(newFile);
        int retval = fc.showSaveDialog(this);
            
            if(retval == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                BufferedWriter out = new BufferedWriter(new FileWriter(f));
                int i = 0;
                for (HashSet<Point> c : clusters) {
                    out.write("#"+i+"\n");
                    for (Point p : c) {
                        out.write(p+"\n");
                    }
                    i++;
                }
                out.write("#noise\n");
                for(Point p: noise) {
                    out.write(p+"\n");
                }
                log.append("Exported to file "+f.getName()+"\n");
                out.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } 
        
    }

    /**
     * 
     */
    protected void resetNeighbours() {
        resetClusters();
        for(Point p : points) {
            p.clearNeighbours();
        }
    }

    /**
     * 
     */
    private void resetClusters() {
        for (Point p : points) {
            p.visited = false;
        }
        clusters.clear();
        noise.clear();
    }
    /**
     * 
     */
    private void stopNeighbouring() {
        neighbourRunner.cancel(true);
        enableInputs();
    }

    /**
     * 
     */
    private void runNeighbouring() {
        disableInputs();
        resetNeighbours();
        if (neighbourRunner.isDone())
            neighbourRunner = new NeighbourRunner();
        neighbourRunner.execute();
    }
    /**
     * 
     */
    private void stopClustering() {
        clusterRunner.cancel(true);
        enableInputs();
    }

    /**
     * 
     */
    private void runClustering() {
        disableInputs();
        resetClusters();
        if (clusterRunner.isDone())
            clusterRunner = new ClusterRunner();
        clusterRunner.execute();
    }

    private class NeighbourRunner extends SwingWorker<String, Object> {

        NeighbourRunner() {
            this.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    if ("progress".equals(e.getPropertyName())) {
                        progressBar.setValue((Integer) e.getNewValue());
                    }

                }
            });
        }
        @Override
        protected String doInBackground() throws Exception {
            progressBar.setValue(0);
            log.append("Finding neighbours\n");
            double start = System.nanoTime();
            findNeighboursQuadTree();
            log.append("Found neighbours in "
                    + ((System.nanoTime() - start) / 1000000000.0) + "s\n");
            return null;
        }

        private void findNeighboursQuadTree() {
            int i = 0;
            for (Point p : points) {
                i++;
                if(isCancelled())
                    return;
                points.queryCircle(epsilon, p);
                setProgress(100*i/points.size());
            }
        }

        @Override
        protected void done() { 
            if(!isCancelled()) {
                runClustering();
                btnRecalculateClusters.setEnabled(true);
            } else {
                System.out.println("Neighbouring was cancelled");
            }
        }
        
        /*
         * Old code
        private void findNeighbours() {
            for (int i = 0; i < points.size(); i++) {
                for (int j = i + 1; j < points.size(); j++) {
                    if(isCancelled())
                        return;
                    setNeighbours(points.get(i), points.get(j));
                }
            }
        }

        private class NeighbourSetter implements Runnable {
            private final int i,j;
            public NeighbourSetter(int i, int j) {
                this.i = i;
                this.j = j;
            }
            @Override
            public void run() {
                setNeighbours(points.get(i), points.get(j));
            }
        }
        private void findNeighboursParallel() {
            ExecutorService tpe =  Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (int i = 0; i < points.size(); i++) {
                for (int j = i + 1; j < points.size(); j++) {
                    tpe.execute(new NeighbourSetter(i, j));
                }
            }
        }
        
        private void setNeighbours(Point p, Point q) {
            if (p.distance(q) <= epsilon) {
                p.addNeighbour(q);
                q.addNeighbour(p);
            }
        }
        */
    }
    /**
     * 
     * @author Jonathan
     *
     */
    private class ClusterRunner extends SwingWorker<String, Object> {

        private int pointsClustered;
        ClusterRunner() {
            pointsClustered = 0;
            this.addPropertyChangeListener(new PropertyChangeListener() {
                
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                   if("progress".equals(e.getPropertyName())) {
                       progressBar.setValue((Integer)e.getNewValue());
                   }
                }
            });
        }
        @Override
        protected String doInBackground() throws Exception {
            if(!neighbourRunner.isDone())
                return null;
            progressBar.setValue(0);
            log.append("Clustering\n");
            double start = System.nanoTime();
            dbscan();
            log.append(String.format("Found %d clusters from %d points in %fs.%n%d noise%n",
                    clusters.size(), points.size(),
                    (System.nanoTime() - start) / 1000000000.0,
                    noise.size()));
            visualisation.repaint();
            return null;
        }

        @Override
        protected void done() {
            enableInputs();
            tglbtnStart.setSelected(false);
            tglbtnStart.setText("Start");
            visualisation.repaint();
            visualisation.validate();
        }
        /**
         * Helper method for DBSCAN
         * @param p
         * @param cluster
         */
        private void expandCluster(Point p, HashSet<Point> cluster) {
            cluster.add(p);
            for (Point i : p.getNeighbours()) {
                if (!i.visited) {
                    i.visited = true;
                    pointsClustered++;
                    setProgress(100*pointsClustered/points.size());
                    if (i.neighbourCount() >= minpts) {
                        i.addNeighbours(p.getNeighbours());
                    }
                }
                boolean iInCluster = false;
                for (HashSet<Point> x : clusters) {
                    if(x.contains(i)) {
                        iInCluster = true;
                        break;
                    }
                }
                if(!iInCluster)
                    cluster.add(i);
            }
        }
        /**
         * Implements DBSCAN.
         * Adapted from pseudocode at http://en.wikipedia.org/wiki/DBSCAN
         * 
         */
        private void dbscan() {
            for (Point i : points) {
                if(isCancelled())
                    return;
                if (i.visited) {
                    continue;
                }
                i.visited = true;
                pointsClustered++;
                setProgress(100*pointsClustered/points.size());
                if (i.neighbourCount() < minpts) {
                    noise.add(i);
                } else {
                    HashSet<Point> c = new HashSet<Point>();
                    clusters.add(c);
                    expandCluster(i, c);
                }
            }
            
        }
    }
    /**
     * Clears the collections containing points and clusters
     */
    private void clearCollections() {
        points = new QuadTree();
        clusters = new ArrayList<HashSet<Point>>();
        noise = new HashSet<Point>();
        btnRecalculateClusters.setEnabled(false);
    }
    /**
     * Load some random 2D points 
     * @param count the number of points to add
     */
    private void loadRandomPoints(int count) {
        clearCollections();
        log.append("Loading "+count+" random points\n");
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            points.add(new Point(r.nextInt(10), r.nextInt(10)));
        }
        points_loaded = true;
        tglbtnStart.setEnabled(true);
    }
    /**
     * Load points from a file of line separated points, coordinates are separated by a space.
     * The user is prompted to select a file to use.
     * @return true if the points were loaded successfully
     */
    private boolean loadPoints() {
        clearCollections();
        
        int retval = fc.showOpenDialog(this);
        if(retval == JFileChooser.APPROVE_OPTION) {
            points_loaded = false;
            File file= fc.getSelectedFile();
            String filename = file.getAbsolutePath(); 
            
            log.append("Loading points from " + filename + "\n");
            try {
                Scanner in = new Scanner(new File(filename));  
                ArrayList<Point> pointList = new ArrayList<Point>(3000);
                while (in.hasNext()) {
                    int x = in.nextInt();
                    int y = in.nextInt();
                    if(x > maxX)
                        maxX = x;
                    if(y > maxY)
                        maxY = y;
                    pointList.add(new Point(x,y));
                    /*
                    Scanner scan = new Scanner(in.nextLine()); 
                    ArrayList<Integer> ps = new ArrayList<Integer>(3);
                    while(scan.hasNext())
                        ps.add(scan.nextInt());
                    points.add(new Point(ps));
                    */
                }
                points = new QuadTree(pointList);
                points_loaded = true;
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(null, "Could not load file "
                        + filename+", file not found", "Critical Error", JOptionPane.ERROR_MESSAGE);
            } catch (InputMismatchException e) {
                JOptionPane.showMessageDialog(null, "Could not load file "
                        + filename+". This file does not match input format of n" +
                        		" whitespace separated integers on each line", "Critical Error", JOptionPane.ERROR_MESSAGE);
            }
        } 
        tglbtnStart.setEnabled(true);
        return points_loaded;

    }

    
    /**
     * Disable the input parameters to prevent user from changing parameters while clustering is running
     */
    private void disableInputs() {
        spinner_epsilon.setEnabled(false);
        spinner_minpts.setEnabled(false);
    }

    /**
     * enable the parameter inputs
     */
    private void enableInputs() {
        spinner_epsilon.setEnabled(true);
        spinner_minpts.setEnabled(true);
        
    }

    /**
     * 
     * @author Jonathan
     *
     */
    private class Display extends Canvas {
        /**
         * 
         */
        private static final long serialVersionUID = 3415470189652848972L;
        private BufferStrategy bf;

        Display() {
            setMinimumSize(new Dimension(1000, 1000));
            setMaximumSize(new Dimension(1000, 1000));
            setBackground(Color.black);
        }

        /**
         * Paint the graphics
         */
        public void paint(Graphics g) {
            createBufferStrategy(1);
            // Use a bufferstrategy to remove that annoying flickering of the
            // display when rendering
            bf = getBufferStrategy();
            g = null;
            try {
                g = bf.getDrawGraphics();
                render(g);
            } finally {
                g.dispose();
            }
            bf.show();
            Toolkit.getDefaultToolkit().sync();
        }
        /**
         * Renders the points onto the window
         * @param g
         */
        private void render(Graphics g) {
            int width  = getWidth();
            int height = getHeight();
            
            for (Point p : noise) {
                g.setColor(Color.white);
                drawCircle(g, p.coordinates[0]*width/maxX , p.coordinates[1]*height/maxY , 2,false, Color.white);
            }
            for (HashSet<Point> cluster : clusters) {
                Color c = generateRandomColor(null);
                //g.setColor(c);
                for (Point point : cluster) {
                    drawCircle(g, point.coordinates[0]*width/maxX , point.coordinates[1]*height/maxY , 2,true,c);
                }
            }
        }

        private void drawCircle(Graphics g, int xCenter, int yCenter, int r,boolean filled,Color c) {

            g.setColor(c);
            if(filled) {
                g.fillOval(xCenter - r, yCenter - r, 2 * r, 2 * r);
            }
            g.drawOval(xCenter - r, yCenter - r, 2 * r, 2 * r);
        }
        /**
         * Generate a random colour, mixing in with a provided colour
         * @param mix the colour to mix with
         * @return the random Color object 
         */
        private Color generateRandomColor(Color mix) {
            Random random = new Random();
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);

            // mix the color
            if (mix != null) {
                red = (red + mix.getRed()) / 2;
                green = (green + mix.getGreen()) / 2;
                blue = (blue + mix.getBlue()) / 2;
            }

            Color color = new Color(red, green, blue);
            return color;
        }
    }


}
