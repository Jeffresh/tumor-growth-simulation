import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
/**
 * CanvasClassTemplate.java
 * Purpose: generic Class that represent the canvas where you will show the stuff.
 * @author: Jeffrey Pallarés Núñez.
 * @version: 1.0 23/07/19
 */

class MainCanvas extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Object of the class that Needs Visualization (ONV)  */
    public static CellularAutomata2D task;
    public static BufferedImage image_ref;
    public static double scale_rate =1;
    public static int xMax;
    public static int yMax;

    public void updateCanvas(){
        this.validate();
        this.repaint();
    }
    public static void setScaleRate(double scaleRate){
        scale_rate = scaleRate;
    }

    public BufferedImage scaleImage(double scale_rate){
        BufferedImage mask = new BufferedImage(xMax, yMax, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(scale_rate, scale_rate);
        AffineTransformOp scaleOp =
                new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        image_ref = scaleOp.filter(image_ref, mask);

        return image_ref;

    }



    /** Constructor of the class that works as a link between the classNV and the GUI */
    public MainCanvas(int x_max, int y_max) {

        this.setOpaque(true);

        task = new CellularAutomata2D();
        task.plug(this);
        xMax = x_max;
        yMax = y_max;

        image_ref = new BufferedImage(xMax, yMax,BufferedImage.TYPE_BYTE_INDEXED);

    }
    public static void setDimensions(int x_max, int y_max){
        xMax = x_max;
        yMax = y_max;
        image_ref = new BufferedImage(xMax, yMax,BufferedImage.TYPE_BYTE_INDEXED);
    }

    /**
     * This method process the information of the ONV and draw it in the image.
     * This method is called in {@link #paintComponent(Graphics)} method.
     * Change to represent what you want, in this example we will draw a chessboard.
     * @return A BufferedImage with the information drew on it.
     */

    private  BufferedImage GenerateImage() {
        Color color;

        int[][] matrix = task.getData();

        for(int x = 0; x < yMax; x++)
        {
            for(int y = 0; y < xMax; y++)
            {

                if(matrix[x][y]  == 1)
                    color = Color.GREEN;
                else if(matrix[x][y] == 0)
                    color = Color.BLACK;
                else if(matrix[x][y] == 2)
                    color = Color.BLUE;
                else if(matrix[x][y] == 3)
                    color = Color.YELLOW;
                else if(matrix[x][y] == 4)
                    color = Color.PINK;
                else
                    color = Color.RED;

                image_ref.setRGB(x, y, color.getRGB());
            }
        }

        if(scale_rate>=1)
            return scaleImage(scale_rate);

        return image_ref;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (task.getData() != null)
            g.drawImage(GenerateImage(), 500 - xMax / 2, 500 - yMax / 2, xMax, yMax, null);
    }
}