/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.spring;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.context.ApplicationContext;

/**
 * An actor producer that lets Spring create the Actor instances.
 */
public class SpringActorProducer implements IndirectActorProducer {
  final ApplicationContext applicationContext;
  final String actorBeanName;

  public SpringActorProducer(ApplicationContext applicationContext, String actorBeanName) {
    this.applicationContext = applicationContext;
    this.actorBeanName = actorBeanName;
  }

  @Override
  public Actor produce() {
    return (Actor) applicationContext.getBean(actorBeanName);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends Actor> actorClass() {
    return (Class<? extends Actor>) applicationContext.getType(actorBeanName);
  }
}
