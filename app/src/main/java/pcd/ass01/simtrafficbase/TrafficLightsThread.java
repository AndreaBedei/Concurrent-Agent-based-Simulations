package pcd.ass01.simtrafficbase;

import pcd.ass01.simengineseq.AbstractEnvironment;
import pcd.ass01.simengineseq.AbstractSimulation;

import java.util.ArrayList;
import java.util.List;

public class TrafficLightsThread extends Thread{
    private final Barrier barrier;
    private final Barrier stepBarrier;
    private final List<TrafficLight> trafficLights;
    private AbstractSimulation simulation;
    private final int dt;

    public TrafficLightsThread(Barrier barrier, Barrier eventBarrier, int dt, AbstractSimulation simulation){
        super();
        this.barrier = barrier;
        this.stepBarrier = eventBarrier;
        this.trafficLights = new ArrayList<>();
        this.dt = dt;
        this.simulation = simulation;
    }
    
    public void addTrafficLight(TrafficLight tl) {
        this.trafficLights.add(tl);
    }

    public void initTrafficLights(AbstractEnvironment env) {
        for (var a: trafficLights) {
            a.init();
        }
    }

    public void step() {
        this.trafficLights.forEach(tl -> tl.step(this.dt));
        barrier.waitBeforeActing();                         // Attende che le macchinine abbiano fatto fase sense/decide
        barrier.waitBeforeActing();                         // Attende che le macchinine abbiano fatto fase act
    }


    public void run() {
        while(true) {
            stepBarrier.waitBeforeActing();     // Attende l'ok per eseguire il passo.
            if(simulation.isStopped())
                break;
            this.step();
        }
        System.out.println("thread light terminate");

    }
}
