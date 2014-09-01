/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 *
 * @author hacksaw
 */
public class Test {
    public static void main(String[] args) {
        
        ActorSystem actorSystem = ActorSystem.create("NSI-DISCOVERY");
                
        ClassLoader myClassLoader = ClassLoader.getSystemClassLoader();

        try {
            Class<?> myClass = myClassLoader.loadClass("net.es.nsi.pce.discovery.actors.NotificationRouter");
            System.out.println(myClass.getName());
            ActorRef configurationActor = actorSystem.actorOf(Props.create(myClass, 2), "discovery-configuration-actor");
            System.out.println("Go!");
        } catch (ClassNotFoundException ex) {
            System.err.println("Failed ");
            ex.printStackTrace();
        }
    }
}
