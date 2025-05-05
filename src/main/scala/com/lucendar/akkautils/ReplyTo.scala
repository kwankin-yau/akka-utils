package com.lucendar.akkautils

import org.apache.pekko.actor.{ActorContext, ActorRef}

/**
 * A message contains `replyTo` property.
 */
trait ReplyTo {

  val replyTo: ActorRef

  /**
   * Send message to replyTo if it is not null, otherwise send to sender().
   *
   * @param m   message to send.
   * @param ctx the actor context.
   */
  def reply(m: Any)(implicit ctx: ActorContext): Unit = if (replyTo != null) replyTo ! m else ctx.sender() ! m

  /**
   * Send message to replyTo if it is not null.
   *
   * @param m message to send
   */
  def replyStrictly(m: Any): Unit = if (replyTo != null) replyTo ! m

}
