import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * ClassNV.java Purpose: generic Class that you can modify and adapt easily for any application that
 * need data visualization.
 *
 * @author: Jeffrey Pallarés Núñez.
 * @version: 1.0 23/07/19
 */
public class CellularAutomata2D implements Runnable {

  private static int[][] actualPh;
  private static int[][] nextPh;
  private static int[][] actualGen, nextGen;
  private static int[] initialPopulation;
  public static AtomicIntegerArray population_counter;
  private int[] local_population_counter;
  private static LinkedList<Double>[] population;
  public static MainCanvas canvasTemplateRef;
  public static AnalyticsMultiChart population_chart_ref;

  public int[][] getData() {
    return actualGen;
  }

  public void plug(MainCanvas ref) {
    canvasTemplateRef = ref;
  }

  public void plugPopulationChart(AnalyticsMultiChart ref) {
    population_chart_ref = ref;
  }

  public static void changeRefs() {
    actualPh = nextPh;
    nextPh = new int[height][width];
    actualGen = nextGen;
    nextGen = new int[height][width];
  }

  public static void stop() {
    abort = true;
  }

  private static int width, height;

  public static int states_number = 1;
  private static int cfrontier = 0;
  private static int seed;
  private static int cells_number;
  public static int generations;
  private static String initializerMode;
  private static Random randomGenerator;

  private static double ps = 1; // Probability of cell survival.
  private static double pp = 0.25; // Probability of cell proliferation.
  private static double pm = 0.2; // Probability of cell migration.
  private static double np = 1; // Total PH needed to proliferate.
  private static double scaleImage = 1;
  private static double pd ; // Probability of cell death.
  private static double pq; // Probability of cell quiescence.
  private double rr = 1; // Random value to determine survival.
  private double rrm = 1; // Random value to determine migration.
  private double rrp = 1; // Random value to determine proliferation.

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

    for (int i = 0; i < generations - 1; i++) {
      if (abort) break;
      nextGen(i);

      try {
        int l = barrier.await();
        for (int j = 0; j < states_number; j++) {
          population_counter.getAndAdd(j, this.local_population_counter[j]);
        }

        if (barrier.getParties() == 0) barrier.reset();

        l = barrier.await();

        if (this.task_number == 1) {
          canvasTemplateRef.revalidate();
          canvasTemplateRef.repaint();
          Thread.sleep(0, 10);

          for (int j = 0; j < states_number; j++) {
            population[j].add((double) population_counter.get(j));
          }
          population_counter = new AtomicIntegerArray(states_number);

          if (CellularAutomata2D.population_chart_ref != null)
            CellularAutomata2D.population_chart_ref.plot();
          changeRefs();
        }

        if (barrier.getParties() == 0) barrier.reset();

        l = barrier.await();

        if (barrier.getParties() == 0) barrier.reset();
      } catch (Exception e) {
        System.out.println(e.toString());
      }
    }
  }


  public CellularAutomata2D() {}

  public CellularAutomata2D(int i) {
    task_number = i;

    int paso = cells_number / total_tasks;

    fn = paso * task_number;
    in = fn - paso;

    if (total_tasks == task_number) fn = cells_number;
  }

  public int[] getInitialPopulation() {
    return initialPopulation;
  }

  public LinkedList<Double>[] getPopulation() {
    return population;
  }

  private static void customInitializer() {
    actualGen[height / 2][width / 2] = 1;
  }

  private static void caseAInitializer() {
    actualGen[height / 2][width / 2] = 1;
    np = 1;
    pm = 0.2;
  }

  private static void caseBInitializer() {
    actualGen[height / 2][width / 2] = 1;
    pm = 0.8;
    np = 1;
  }

  private static void caseCInitializer() {
    actualGen[height / 2][width / 2] = 1;
    pm = 0.2;
    np = 2;

  }

  private static void caseDInitializer() {
    actualGen[height / 2][width / 2] = 1;
    pm = 0.8;
    np = 2;
  }

  private static void initializeState(String initializerMode) {
    switch (initializerMode) {
      case "A":
        caseAInitializer();
        break;
      case "B":
        caseBInitializer();
        break;
      case "C":
        caseCInitializer();
        break;
      case "D":
        caseDInitializer();
        break;
      default:
        customInitializer();
    }
  }

  public void initializer(
      int cells_number,
      int generations,
      int cfrontier,
      String initializerMode,
      double ps,
      double pp,
      double pm,
      double np) {

    randomGenerator = new Random();

    width = cells_number;
    height = cells_number;

    actualGen = new int[height][width];
    nextGen = new int[height][width];
    actualPh = new int[height][width];
    nextPh = new int[height][width];


    population_counter = new AtomicIntegerArray(states_number);

    CellularAutomata2D.cells_number = cells_number;
    CellularAutomata2D.generations = generations;
    CellularAutomata2D.cfrontier = cfrontier;
    CellularAutomata2D.initializerMode = initializerMode;

    CellularAutomata2D.ps = ps;
    CellularAutomata2D.pp = pp;
    CellularAutomata2D.pm = pm;
    CellularAutomata2D.np = np;
    CellularAutomata2D.pd = 1 - ps;
    CellularAutomata2D.pq = 1 - pm - pp;

    population = new LinkedList[states_number];
    initialPopulation = new int[states_number];

    CellularAutomata2D.initializeState(initializerMode);
    for (int i = 0; i < states_number; i++) {
      population[i] = new LinkedList<Double>();
    }

    for (int j = 0; j < states_number; j++) {
      population[j].add((double) initialPopulation[j]);
    }
    if (CellularAutomata2D.population_chart_ref != null)
      CellularAutomata2D.population_chart_ref.plot();
  }


  private int computeVonNeumannNeighborhood(int i, int j) {
    int cellsAlive = 0;

    cellsAlive = actualGen[(i + 1 + height) % height][j]
            + actualGen[(i - 1 + height) % height][j]
            + actualGen[i][(j - 1 + width) % width]
            + actualGen[i][(j + 1 + width) % width];

    return cellsAlive;
  }

  private double P1(int i, int j) {
    return getProbability(i, j, -1, 0);
  }

  private double P2(int i, int j) {
    return getProbability(i, j, 1, 0);
  }

  private double P3(int i, int j) {
    return getProbability(i, j, 0, -1);
  }

  private double P4(int i, int j) {
    return getProbability(i, j, 0, 1);
  }

  private double getProbability(int i, int j, int posI, int posJ) {
    return (1 - actualGen[(i + posI + height) % height][(j + posJ + width) % width])
        / (double) probabilityDenominator(i, j);
  }

  private int probabilityDenominator(int i, int j) {
    return 4 - computeVonNeumannNeighborhood(i,j);
  }

  private boolean cellSurvives(int i, int j) {
    rr = Math.random();
    return (rr < ps && actualGen[i][j] == 1);
  }

  public boolean cellProliferates(int i, int j) {
    rrp = Math.random();

    if(rrp < pp)
      actualPh[i][j]++;
    return actualPh[i][j] >= np;

  }

  public boolean cellMigrates() {
    rrm = Math.random();
    return 	rrm < pm;
  }

  public static void next_gen_concurrent(int nt, int g) {
    gens = g;

    size_pool = nt;

    barrier = new CyclicBarrier(size_pool);
    total_tasks = size_pool;

    myPool =
            new ThreadPoolExecutor(
                    size_pool,
                    size_pool,
                    60000L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());
    CellularAutomata2D[] tareas = new CellularAutomata2D[nt];

    for (int t = 0; t < nt; t++) {
      tareas[t] = new CellularAutomata2D(t + 1);
      myPool.execute(tareas[t]);
    }

    myPool.shutdown();
    try {
      myPool.awaitTermination(10, TimeUnit.HOURS);
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  public static LinkedList<Double>[] caComputation(int nGen) {
    abort = false;
    generations = nGen;
    next_gen_concurrent(1, nGen);

    return population;
  }

  public int getDirection(double probability, int i, int j) {

   if (0 < probability && probability <= P1(i,j))
     return 1;
   else if( probability <= P1(i,j)+ P2(i,j))
     return 2;
   else if(probability <= P1(i,j) + P2(i,j) + P3(i,j))
     return 3;
   else if(probability <=1 )
     return 4;
   return 0;
  }

  public void proliferates(int i, int j, int position) {
    updateDirection(i,j,position,false);
  }

  public void migrates(int i, int j, int position) {
    updateDirection(i, j, position, true);
  }

  private void updateDirection(int i, int j, int position, boolean migration) {
    int posI =0, posJ =0;
    switch (position) {
      case 1: {
        posI = -1;
        posJ = 0;
        break;
      }
      case 2: {
        posI = 1;
        posJ = 0;
        break;
      }
      case 3: {
        posI = 0;
        posJ = -1;
        break;
      }
      case 4: {
        posI= 0;
        posJ = 1;
        break;
      }
    }

    if(position != 0){
      if(migration) {
        updatePosition(i,j,posI ,posJ,nextPh, actualPh[i][j]);
        updatePosition(i,j, 0, 0, nextGen, 0);
        local_population_counter[0]++;
      }
      else {
        updatePosition(i,j,posI ,posJ,nextPh, 0);
        updatePosition(i,j, 0, 0, nextGen, 1);

      }
      updatePosition(i,j,0 ,0,nextPh, 0);
      updatePosition(i,j, posI , posJ, nextGen, 1);
      local_population_counter[nextGen[i][j]]++;
    }


  private static int  getPositionValue(int i, int j, int posI, int posJ, int[][] matrix) {
    return matrix[(i + posI + height) % height][(j + posJ + width) % width];

  }


    private static void updatePosition(int i, int j, int posI, int posJ, int[][] matrix, int value) {
    matrix[(i + posI + height) % height][(j + posJ + width) % width] = value;
  }


  public LinkedList<Double>[] nextGen(int actual_gen) {

    local_population_counter = new int[states_number];

    for (int i = 0; i < states_number; i++) {
      this.local_population_counter[i] = 0;
    }

    for (int i = 0; i < width; i++) {
      for (int j = in; j < fn; j++) {
        if (abort) break;
        if (cellSurvives(i, j)) {
          nextGen[i][j] = 1;
          if (cellProliferates(i, j)) {
            rr = Math.random();
            int direction = getDirection(rr, i, j);
            proliferates(i, j, direction);
          } else if (cellMigrates()) {
            rrm = Math.random();
            int direction = getDirection(rrm, i, j);
            migrates(i, j, direction);
          }
        } else {
          nextPh[i][j] = 0;
          nextGen[i][j] = 0;
        }
      }
    }
    return population;
  }
}
