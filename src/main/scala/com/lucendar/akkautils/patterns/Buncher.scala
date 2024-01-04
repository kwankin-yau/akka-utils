package com.lucendar.akkautils.patterns


import akka.actor.{Actor, ActorRef, Props, Timers}

import scala.concurrent.duration.FiniteDuration


object Buncher {
  private case object Timeout
  private case object TimerKey



  def props(target: ActorRef, interval: FiniteDuration, maxSize: Int): Props =
    Props(new Buncher(target, interval, maxSize))


  case class ReqAndSender(req: Object, sndr: ActorRef)
  case class BunchReq(req: Object)
  case class BunchReqBatch(requests: Vector[ReqAndSender])
}


class Buncher(target: ActorRef, interval: FiniteDuration, maxSize: Int) extends Actor with Timers {

  import Buncher._

  override def receive: Receive = idle

  private def idle: Receive = {
    case m: BunchReq =>
      timers.startSingleTimer(TimerKey, Timeout, interval)
      context.become(active(Vector(ReqAndSender(m.req, sender()))))
  }

  private def active(buffer: Vector[ReqAndSender]): Receive = {
    case Timeout =>
      if (buffer.nonEmpty)
        target ! BunchReqBatch(buffer)

      context.become(idle)

    case m: BunchReq =>
      val newBuffer = buffer :+ ReqAndSender(m.req, sender())
      if (newBuffer.size == maxSize) {
        timers.cancel(TimerKey)
        target ! BunchReqBatch(buffer)
        context.become(idle)
      } else
        context.become(active(newBuffer))
  }
}

