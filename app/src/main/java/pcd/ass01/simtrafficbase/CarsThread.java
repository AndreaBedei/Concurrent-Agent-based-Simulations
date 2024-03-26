package pcd.ass01.simtrafficbase;

import java.util.ArrayList;
import java.util.List;

import pcd.ass01.simengineseq.AbstractEnvironment;

public class CarsThread extends Thread {

    private final Barrier barrier;
    private final Barrier stepBarrier;
    private final List<CarAgent> carAgents;
    private boolean interrupted = false;
    private final int dt;

    public CarsThread(Barrier barrier, Barrier eventBarrier, int dt){
        super();
        this.barrier = barrier;
        this.stepBarrier = eventBarrier;
        this.carAgents = new ArrayList<>();
        this.dt = dt;
    }

    public void requestInterrupt() {
        this.interrupted = true;
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
        this.carAgents.forEach(car -> car.senseAndDecide(this.dt));
        System.out.println("sense");
        barrier.waitBeforeActing();             // Attende che tutti i thread abbiano preso le decisioni.
        System.out.println("act");

        this.carAgents.forEach(car -> car.act());
    }

    public void run() {
        while(!interrupted) {
            stepBarrier.waitBeforeActing();     // Attende l'ok per eseguire il passo.
            if (interrupted) {
                return;
            }
            this.step();
        }
    }
}
