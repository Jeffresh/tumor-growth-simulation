import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * ClassNV.java
 * Purpose: generic Class that you can modify and adapt easily for any application
 * that need data visualization.
 * @author: Jeffrey Pallarés Núñez.
 * @version: 1.0 23/07/19
 */

public class BenchmarkLife implements Runnable
{

    private static int[][] matrix;
    private static  int[][] actualGen, nextGen;


    private static int width, height;

    private static int cfrontier = 0;
    private static int cells_number;
    public static int generations;
    private static Random randomGenerator;

    private int task_number;
    private static int total_tasks;
    private static CyclicBarrier barrier = null;
    private int in;
    private int fn;
    public static Boolean abort = false;
    private static int gens;
    private static int size_pool;
    private static ThreadPoolExecutor myPool;


    public void run() {

        for (int i = 0; i < generations-1 ; i++) {

            nextGen(i);

            try
            {
                int l = barrier.await();
                if(barrier.getParties() == 0)
                    barrier.reset();


                if(this.task_number==1) {
                    changeRefs();
                }

                l = barrier.await();
                if(barrier.getParties() == 0)
                    barrier.reset();

            }catch(Exception e){}
        }

    }


    public BenchmarkLife() {}

    public BenchmarkLife(int i) {
        task_number = i;

        int paso = cells_number /total_tasks;


        fn = paso * task_number;
        in = fn - paso;

        if( total_tasks == task_number)
            fn =cells_number;
    }

    public static void next_gen_concurrent(int nt, int gens) {
        size_pool =nt;
        generations = gens;

        barrier = new CyclicBarrier (size_pool);
        total_tasks = size_pool;

        myPool = new ThreadPoolExecutor(
                size_pool, size_pool, 60000L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        BenchmarkLife[] tareas = new BenchmarkLife[nt];

        for(int t = 0; t < nt; t++)
        {
            tareas[t] = new BenchmarkLife(t+1);
            myPool.execute(tareas[t]);

        }

        myPool.shutdown();
        try{
            myPool.awaitTermination(10, TimeUnit.HOURS);
        } catch(Exception e){
            System.out.println(e.toString());
        }

    }

    private static void randomInitializer() {
        int nCells = (height*height)/2;
        for(int i=0; i < nCells; i++) {
            actualGen[randomGenerator.nextInt(height)][randomGenerator.nextInt(height)]=1;
        }
    }


    public void initializer (int cells_number, int generations) {
        randomGenerator = new Random();

        width = cells_number;
        height = cells_number;

        actualGen = new int[width][width]; nextGen = new int[width][width];
        matrix = new int[height][width];


        BenchmarkLife.cells_number = cells_number;
        BenchmarkLife.generations = generations;
        BenchmarkLife.randomInitializer();
    }

    public static void changeRefs() {
        int[][] aux = actualGen;
        actualGen = nextGen;
        nextGen = aux;
    }


    public static LinkedList<Long>caComputation(int taskNumber, int nGen) {
        LinkedList<Long> times = new LinkedList<>();
        generations = nGen;



        for (int i = 1; i <= taskNumber; i++) {
            Instant startTime = Instant.now();
            next_gen_concurrent(i,nGen);
            Instant end = Instant.now();
            times.add(Duration.between(startTime,end).getSeconds());
        }

        return times;
    }

    private int computeVonNeumannNeighborhood(int i, int j) {
        int cellsAlive = 0 ;

        if(cfrontier==0) {
            for(int x = i-1; x<=i+1; x++) {
                for(int y = j-1; y<=j+1; y++) {
                    if((x >= 0 && x < width) && (y >= 0 && y < width) && (( x != i) || (y != j)) && (actualGen[x][y] == 1))
                        cellsAlive ++;
                }
            }
        }

        return cellsAlive;
    }

    private int transitionFunction(int cellsAlive, int i, int j) {
        int transitionFunctionValue;

        if(cellsAlive <2 || cellsAlive >3)
            transitionFunctionValue = 0;
        else if( cellsAlive == 2)
            transitionFunctionValue = actualGen[i][j];
        else
            transitionFunctionValue = 1;

        return transitionFunctionValue;
    }

    public int getCellValue(int i, int j){
        int cellsAlive = computeVonNeumannNeighborhood(i,j);
        return transitionFunction(cellsAlive, i, j);
    }

    public void nextGen(int actual_gen) {

        for(int i = 0; i< width; i++)
            for (int j = in; j < fn; j++) {
                nextGen[i][j] = getCellValue(i,j);
            }

    }


    public static void main(String[] args) {
        BenchmarkLife life = new BenchmarkLife();
        life.initializer(1000, 1000);
        LinkedList<Long> times = caComputation(8,1000);
        System.out.print("Medidas de tiempo");
        for (Long time: times )
            System.out.println(time);
        long firstValue =0;

        System.out.print("Medidas de Speed up");
        for (int i = 0; i < times.size() ; i++) {
            if (i==0)
                firstValue= times.get(i);
            System.out.println((firstValue+0.0)/(double)times.get(i));

        }

    }

}