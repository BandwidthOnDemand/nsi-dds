package net.es.nsi.dds.messages;

import akka.actor.ActorPath;
import lombok.Data;

/**
 * This is the superclass of all AKKA messages sent internally.
 */
@Data
public class Message {
  private String initiator = "unspecified"; // Who initiated the message.
  private ActorPath path; // The path of the Agent initiating the message.

  public Message() {
  }

  public Message(String initiator) {
    this.initiator = initiator;
  }

  public Message(String initiator, ActorPath path) {
    this.initiator = initiator;
    this.path = path;
  }

  /**
   * Generate a debug string for use in logging.
   *
   * @param msg
   * @return
   */
  public static String getDebug(Object msg) {
    StringBuilder result = new StringBuilder(msg.getClass().getCanonicalName());
    if (msg instanceof Message) {
      Message message = (Message) msg;
      result.append(" : ");
      result.append(message.getInitiator());

      if (message.getPath() != null) {
        result.append(" : ");
        result.append(message.getPath());
      }
    }
    return result.toString();
  }
}
