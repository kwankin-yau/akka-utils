/*
 * Copyright (c) 2024 lucendar.com.
 * All rights reserved.
 */
package com.lucendar.akkautils.msg

trait ActorMsg {
  def msgId: Int
}

object ActorMsgs {
  final val ActorMsgId_Start = 1
  final val ActorMsgId_Stop = 2

  case object ActorMsgs_Start extends ActorMsg {
    override def msgId: Int = ActorMsgId_Start
  }

  case object ActorMsgs_Stop extends ActorMsg {
    override def msgId: Int = ActorMsgId_Stop
  }
}
