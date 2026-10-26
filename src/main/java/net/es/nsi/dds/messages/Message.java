package net.es.nsi.dds.messages;

import org.apache.pekko.actor.ActorPath;
import lombok.Data;

/**
 * This is the superclass of all Pekko messages sent internally.
 */
@Data
public class Message {
  private String initiator = "unspecified"; // Who initiated the message.
  private ActorPath path; // The path of the Agent initiating the message.

  /**
   * Empty constructor.
   */
  public Message() {
  }

  /**
   * Message constructor for a non-Pekko thread to initiate a message.
   *
   * @param initiator A string identifying the class initiating the message.
   */
  public Message(String initiator) {
    this.initiator = initiator;
  }

  /**
   * Message constructor for an Pekko thread to initiate a message.
   *
   * @param initiator A string identifying the class initiating the message.
   * @param path The Pekko agent path name.
   */
  public Message(String initiator, ActorPath path) {
    this.initiator = initiator;
    this.path = path;
  }

  /**
   * Generate a debug string for use in logging.
   *
   * @param msg The message object to extract context into a string.
   * @return A string containing the message context.
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
