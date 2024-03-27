package pcd.ass01.simtrafficbase;

import java.util.ArrayList;
import java.util.List;

import pcd.ass01.simengineseq.AbstractSimulation;

public class SimThreadsSupervisor {
    // Supervisore per i thread sia delle macchinine che dei semafori.
    private final int nThreadsForCars;
    private final int nThreadsForTrafficLights;
    private RoadsEnv env;
    private final List<CarsThread> carsThreads;

    private final List<TrafficLightsThread> trafficLightsThreads;
    /**
     * Barrier for coordinating threads on when to act.
     */
    private final Barrier actBarrier;
    /**
     * Barrier for coordinating with the threads on when they can perform a step.
     */
    private final Barrier stepBarrier;
    private AbstractSimulation simulation;
    private int nStepsPerSec = 1;
    private int nSteps = 0;
    private int dt = Integer.MAX_VALUE;

    private int t0;
    
    /* for time statistics*/
	private long currentWallTime;

    public SimThreadsSupervisor(int nThreadsForCars, int nThreadsForLights, AbstractSimulation simulation) {
        this.nThreadsForCars = nThreadsForCars;
        this.nThreadsForTrafficLights = nThreadsForLights;
        this.actBarrier = new BarrierImpl(nThreadsForCars + nThreadsForLights);
        this.stepBarrier = new BarrierImpl(nThreadsForCars + nThreadsForLights + 1);  // +1 per il supervisor, bisogna aspettare anche lui.
        this.carsThreads = new ArrayList<>(nThreadsForCars);
        this.trafficLightsThreads = new ArrayList<>(nThreadsForLights);
        this.simulation = simulation;
    }

    public void setEnvironment(RoadsEnv env) {
        this.env = env;
    }

    public void setTimings(int t0, int dt) {
        this.t0 = t0;
        this.dt = dt;
    }

    public void setSteps(int nSteps) {
        this.nSteps = nSteps;
    }

    public void setStepsPerSec(int nStepsPerSec) {
        this.nStepsPerSec = nStepsPerSec;
    }

    public void createCars(List<CarAgent> cars) {
        var iter = cars.iterator();
        int carsPerThread = cars.size() / nThreadsForCars;
        int remainingCars = cars.size() % nThreadsForCars;
        for (int i = 0; i < nThreadsForCars; i++) {
            CarsThread th = new CarsThread(actBarrier, stepBarrier, dt);
            carsThreads.add(th);
            for (int j = 0; j < carsPerThread; j++) {
                th.addCar(iter.next());
            }
        }
        CarsThread th = carsThreads.getLast();
        for (int j = 0; j < remainingCars; j++) {
            th.addCar(iter.next());
        }
    }

    public void createTrafficLights(List<TrafficLight> trafficLights) {
        var iter = trafficLights.iterator();
        int lightsPerThread = trafficLights.size() / this.nThreadsForTrafficLights;
        int remainingLights = trafficLights.size() % this.nThreadsForTrafficLights;
        for (int i = 0; i < nThreadsForTrafficLights; i++) {
            TrafficLightsThread th = new TrafficLightsThread(actBarrier, stepBarrier, dt);
            this.trafficLightsThreads.add(th);
            for (int j = 0; j < lightsPerThread; j++) {
                th.addTrafficLight(iter.next());
            }
        }
        TrafficLightsThread th = trafficLightsThreads.getLast();
        for (int j = 0; j < remainingLights; j++) {
            th.addTrafficLight(iter.next());
        }
    }

    public void runAllThreads() {
		carsThreads.forEach(th -> th.initCars(this.env));
		carsThreads.forEach(th -> th.start());

        trafficLightsThreads.forEach(th -> th.initTrafficLight(this.env));
        trafficLightsThreads.forEach(th -> th.start());



        new Thread(() -> {
            int stepsDone = 0;
            int t = this.t0;
            while (stepsDone < this.nSteps && !this.simulation.isStopped()) {

                this.stepBarrier.waitBeforeActing();    // Avvia l'inizio del passo.
                t += this.dt;
                currentWallTime = System.currentTimeMillis();
                this.simulation.notifySimulationStep(t);

                syncWithWallTime();
                //try {
                    //Thread.sleep(500);
                //} catch (InterruptedException e) {
                //throw new RuntimeException(e);
                //}
                stepsDone++;
            }
            this.stopAllThreads();
            this.stepBarrier.waitBeforeActing();        // Avvia il passo di terminazione.
        }).start();


    }

    public void stopAllThreads() {
        this.carsThreads.forEach(th -> th.requestInterrupt());
        this.trafficLightsThreads.forEach(th -> th.requestInterrupt());
    }

    private void syncWithWallTime() {
		try {
			long newWallTime = System.currentTimeMillis();
			long delay = 1000 / this.nStepsPerSec;
			long wallTimeDT = newWallTime - this.currentWallTime;
			if (wallTimeDT < delay) {
				Thread.sleep(delay - wallTimeDT);
			}
		} catch (Exception ex) {}		
	}
}