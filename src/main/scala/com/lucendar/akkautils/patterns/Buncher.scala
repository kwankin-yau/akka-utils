package com.lucendar.akkautils.patterns


import org.apache.pekko.actor.{Actor, ActorRef, Props, Timers}

import scala.concurrent.duration.FiniteDuration


object Buncher {
  private case object Timeout
  private case object TimerKey

  /**
   * Create Buncher Props.
   *
   * @param target The target actor finally receives and handles the `BuncherReqBatch` message.
   * @param interval The interval of collecting `BunchReq` messages and merge them into a single `BuncherReqBatch` message.
   * @param maxSize the maximum batch size. The Buncher will build a `BuncherReqBatch` message and send it to the `target`
   *                when the count of cached messages reaches `maxSize`.
   * @return The `Props` for the Buncher.
   */
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
        target ! BunchReqBatch(newBuffer)
        context.become(idle)
      } else
        context.become(active(newBuffer))
  }
}

