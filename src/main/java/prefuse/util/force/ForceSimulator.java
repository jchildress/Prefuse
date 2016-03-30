package prefuse.util.force;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Manages a simulation of physical forces acting on bodies. To create a
 * custom ForceSimulator, add the desired {@link Force} functions and choose an
 * appropriate {@link Integrator}.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ForceSimulator {

    private ArrayList items;
    private ArrayList springs;
    private Force[] iForces;
    private Force[] sForces;
    private int ifLen, sfLen;
    private Integrator integrator;
    private float speedLimit = 1.0f;
    
    /**
     * Create a new, empty ForceSimulator. A RungeKuttaIntegrator is used
     * by default.
     */
    public ForceSimulator() {
        this(new RungeKuttaIntegrator());
    }

    /**
     * Create a new, empty ForceSimulator.
     * @param integrator the Integrator to use
     */
    public ForceSimulator(Integrator integrator) {
        this.integrator = integrator;
        iForces = new Force[5];
        sForces = new Force[5];
        ifLen = 0;
        sfLen = 0;
        items = new ArrayList();
        springs = new ArrayList();
    }

    /**
     * Get the speed limit, or maximum velocity value allowed by this
     * simulator.
     * @return the "speed limit" maximum velocity value
     */
    public float getSpeedLimit() {
        return speedLimit;
    }
    
    /**
     * Set the speed limit, or maximum velocity value allowed by this
     * simulator.
     * @param limit the "speed limit" maximum velocity value to use
     */
    public void setSpeedLimit(float limit) {
        speedLimit = limit;
    }
    
    /**
     * Get the Integrator used by this simulator.
     * @return the Integrator
     */
    public Integrator getIntegrator() {
        return integrator;
    }
    
    /**
     * Set the Integrator used by this simulator.
     * @param integrator the Integrator to use
     */
    public void setIntegrator(Integrator integrator) {
        this.integrator = integrator;
    }
    
    /**
     * Clear this simulator, removing all ForceItem and Spring instances
     * for the simulator.
     */
    public void clear() {
        items.clear();
        Iterator sIter = springs.iterator();
        Spring.SpringFactory f = Spring.getFactory();
        while ( sIter.hasNext() )
            f.reclaim((Spring)sIter.next());
        springs.clear();
    }
    
    /**
     * Add a new Force function to the simulator.
     * @param f the Force function to add
     */
    public void addForce(Force f) {
        if ( f.isItemForce() ) {
            if ( iForces.length == ifLen) {
                // resize necessary
                Force[] newF = new Force[ifLen +10];
                System.arraycopy(iForces, 0, newF, 0, iForces.length);
                iForces = newF;
            }
            iForces[ifLen++] = f;
        }
        if ( f.isSpringForce() ) {
            if ( sForces.length == sfLen) {
                // resize necessary
                Force[] newF = new Force[sfLen +10];
                System.arraycopy(sForces, 0, newF, 0, sForces.length);
                sForces = newF;
            }
            sForces[sfLen++] = f;
        }
    }
    
    /**
     * Get an array of all the Force functions used in this simulator.
     * @return an array of Force functions
     */
    public Force[] getForces() {
        Force[] rv = new Force[ifLen + sfLen];
        System.arraycopy(iForces, 0, rv, 0, ifLen);
        System.arraycopy(sForces, 0, rv, ifLen, sfLen);
        return rv;
    }
    
    /**
     * Add a ForceItem to the simulation.
     * @param item the ForceItem to add
     */
    public void addItem(ForceItem item) {
        items.add(item);
    }
    
    /**
     * Remove a ForceItem to the simulation.
     * @param item the ForceItem to remove
     */
    public boolean removeItem(ForceItem item) {
        return items.remove(item);
    }

    /**
     * Get an iterator over all registered ForceItems.
     * @return an iterator over the ForceItems.
     */
    public Iterator getItems() {
        return items.iterator();
    }
    
    /**
     * Add a Spring to the simulation.
     * @param item1 the first endpoint of the spring
     * @param item2 the second endpoint of the spring
     * @return the Spring added to the simulation
     */
    public Spring addSpring(ForceItem item1, ForceItem item2) {
        return addSpring(item1, item2, -1.f, -1.f);
    }
    
    /**
     * Add a Spring to the simulation.
     * @param item1 the first endpoint of the spring
     * @param item2 the second endpoint of the spring
     * @param length the spring length
     * @return the Spring added to the simulation
     */
    public Spring addSpring(ForceItem item1, ForceItem item2, float length) {
        return addSpring(item1, item2, -1.f, length);
    }
    
    /**
     * Add a Spring to the simulation.
     * @param item1 the first endpoint of the spring
     * @param item2 the second endpoint of the spring
     * @param coeff the spring coefficient
     * @param length the spring length
     * @return the Spring added to the simulation
     */
    public Spring addSpring(ForceItem item1, ForceItem item2, float coeff, float length) {
        if ( item1 == null || item2 == null )
            throw new IllegalArgumentException("ForceItems must be non-null");
        Spring s = Spring.getFactory().getSpring(item1, item2, coeff, length);
        springs.add(s);
        return s;
    }
    
    /**
     * Get an iterator over all registered Springs.
     * @return an iterator over the Springs.
     */
    public Iterator getSprings() {
        return springs.iterator();
    }
    
    /**
     * Run the simulator for one time step.
     * @param timeStep the span of the time step for which to run the simulator
     */
    public void runSimulator(long timeStep) {
        accumulate();
        integrator.integrate(this, timeStep);
    }
    
    /**
     * Accumulate all forces acting on the items in this simulation
     */
    public void accumulate() {
        for (int i = 0; i < ifLen; i++ )
            iForces[i].init(this);
        for (int i = 0; i < sfLen; i++ )
            sForces[i].init(this);
        Iterator itemIter = items.iterator();
        while ( itemIter.hasNext() ) {
            ForceItem item = (ForceItem)itemIter.next();
            item.force[0] = 0.0f; item.force[1] = 0.0f;
            for (int i = 0; i < ifLen; i++ )
                iForces[i].getForce(item);
        }
        Iterator springIter = springs.iterator();
        while ( springIter.hasNext() ) {
            Spring s = (Spring)springIter.next();
            for (int i = 0; i < sfLen; i++ ) {
                sForces[i].getForce(s);
            }
        }
    }
    
} // end of class ForceSimulator
