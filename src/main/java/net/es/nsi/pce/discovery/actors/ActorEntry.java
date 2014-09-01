/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

/**
 *
 * @author hacksaw
 */
public class ActorEntry {
    private String actor;
    private boolean start;

    public ActorEntry(String actor, boolean start) {
        this.actor = actor;
        this.start = start;
    }
    
    public ActorEntry() {
    }
    
    /**
     * @return the actor
     */
    public String getActor() {
        return actor;
    }

    /**
     * @param actor the actor to set
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * @return the start
     */
    public boolean isStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(boolean start) {
        this.start = start;
    }
}
