/*
 * Copyright (c) 2024 lucendar.com.
 * All rights reserved.
 */
package com.lucendar.akkautils.patterns

import akka.actor.{Actor, ActorRef, Props, Timers}

import java.util
import scala.concurrent.duration.FiniteDuration


object Buncher2 {
  private case object Timeout
  private case object TimerKey

  /**
   * Create Buncher2 Props.
   *
   * @param buncherId The ID of the Buncher2.
   * @param target The target actor finally receives and handles the `BunchReqBatch2` message.
   * @param interval The interval of collecting `BunchReq2` messages and merge them into a single `BunchReqBatch2` message.
   * @param maxSize the maximum batch size. The Buncher2 will build a `BunchReqBatch2` message and send it to the `target`
   *                when the count of cached messages reaches `maxSize`.
   * @return The `Props` for the Buncher2.
   */
  def props(buncherId: String, target: ActorRef, interval: FiniteDuration, maxSize: Int): Props =
    Props(new Buncher2(buncherId, target, interval, maxSize))

  case class ReqAndSender2[T](req: T, sndr: ActorRef)
  case class BunchReq2[T](req: T)
  case class BunchReqBatch2[T](buncherId: String, requests: java.util.List[ReqAndSender2[T]])
}


class Buncher2[T](id: String, target: ActorRef, interval: FiniteDuration, maxSize: Int) extends Actor with Timers {

  import Buncher2._

  private final var list: java.util.List[ReqAndSender2[T]] = new util.ArrayList[ReqAndSender2[T]]()

  override def receive: Receive = {
    case m: BunchReq2[T] =>
      list.add(ReqAndSender2[T](m.req, sender()))
      if (list.size == maxSize) {
        timers.cancel(TimerKey)
        target ! BunchReqBatch2(id, list)

        list = new util.ArrayList[ReqAndSender2[T]]()
      } else if (list.size() == 1) {
        timers.startSingleTimer(TimerKey, Timeout, interval)
      }
  }



}

