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
   * @param maxSize The maximum batch size. The Buncher2 will build a `BunchReqBatch2` message and send it to the `target`
   *                when the count of cached messages reaches `maxSize`.
   * @param noSender Whether the item of batch message contains sender reference. If `noSender` is true, then the batch
   *                 message will be `BunchReqBatchNoSender`, otherwise the batch message will be `BunchReqBatch2`.
   * @return The `Props` for the Buncher2.
   */
  def props(buncherId: String, target: ActorRef, interval: FiniteDuration, maxSize: Int, noSender: Boolean): Props =
    Props(new Buncher2(buncherId, target, interval, maxSize, noSender))

  case class ReqAndSender2[T](req: T, sndr: ActorRef)
  case class BunchReq2[T](req: T)
  case class BunchReqBatch2[T](buncherId: String, requests: java.util.List[ReqAndSender2[T]])
  case class BunchReqBatchNoSender[T](buncherId: String, requests: java.util.List[T])
}


class Buncher2[T](
                   id: String,
                   target: ActorRef,
                   interval: FiniteDuration,
                   maxSize: Int,
                   noSender: Boolean
                 ) extends Actor with Timers {

  import Buncher2._

  private final var senderList: java.util.List[ReqAndSender2[T]] = _
  private final var noSenderList: java.util.List[T] = _


  override def preStart(): Unit = {
    if (noSender) {
      noSenderList = new util.ArrayList[T]()
    } else
      senderList = new util.ArrayList[ReqAndSender2[T]]()
  }

  private def senderFeed(): Unit = {
    target ! BunchReqBatch2(id, senderList)

    senderList = new util.ArrayList[ReqAndSender2[T]]()
  }

  private def noSenderFeed(): Unit = {
    target ! BunchReqBatchNoSender(id, noSenderList)

    noSenderList = new util.ArrayList[T]()
  }

  private def senderReceive: Receive = {
    case m: BunchReq2[T] =>
      senderList.add(ReqAndSender2[T](m.req, sender()))
      if (senderList.size == maxSize) {
        timers.cancel(TimerKey)
        senderFeed()
      } else if (senderList.size() == 1) {
        timers.startSingleTimer(TimerKey, Timeout, interval)
      }

    case Timeout =>
      if (!senderList.isEmpty)
        senderFeed()
  }

  private def noSenderReceive: Receive = {
    case m: BunchReq2[T] =>
      noSenderList.add(m.req)
      if (noSenderList.size == maxSize) {
        timers.cancel(TimerKey)
        noSenderFeed()
      } else if (noSenderList.size() == 1) {
        timers.startSingleTimer(TimerKey, Timeout, interval)
      }

    case Timeout =>
      if (!noSenderList.isEmpty)
        senderFeed()
  }

  override def receive: Receive = if (noSender) noSenderReceive else senderReceive

}

