package pcd.ass01.simtrafficbase;

import java.util.ArrayList;
import java.util.List;

import pcd.ass01.simengineseq.AbstractEnvironment;
import pcd.ass01.simengineseq.AbstractSimulation;

public class CarsThread extends Thread {

    private final Barrier barrier;
    private final Barrier stepBarrier;
    private final List<CarAgent> carAgents;
    private final AbstractSimulation simulation;
    private final int dt;

    public CarsThread(Barrier barrier, Barrier eventBarrier, int dt, AbstractSimulation simulation){
        super();
        this.barrier = barrier;
        this.stepBarrier = eventBarrier;
        this.carAgents = new ArrayList<>();
        this.simulation = simulation;
        this.dt = dt;
    }

    public void addCar(CarAgent carAgent) {
        carAgents.add(carAgent);
    }

    public void initCars(AbstractEnvironment env) {
		for (var a: carAgents) {
			a.init(env);
		}
    }

    public void step() {
        barrier.waitBeforeActing();             // Attende che tutti i thread abbiano preso le decisioni.
        this.carAgents.forEach(car -> car.senseAndDecide(this.dt));
        barrier.waitBeforeActing();             // Attende che tutti i thread abbiano preso le decisioni.
        this.carAgents.forEach(car -> car.act());
        //barrier.waitBeforeActing();             // Attende che i semafori abbiano fatto il loro step prima di proseguire.
    }

    public void run() {
        while(!simulation.isStopped()) {

            stepBarrier.waitBeforeActing();     // Attende l'ok per eseguire il passo.

            this.step();
        }
    }
}

