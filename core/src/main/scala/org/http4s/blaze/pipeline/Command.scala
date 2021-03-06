package org.http4s.blaze
package pipeline

import scala.util.control.NoStackTrace

/**
 * @author Bryce Anderson
 *         Created on 1/4/14
 */
object Command {

  trait InboundCommand

  trait OutboundCommand

  case object Connect extends OutboundCommand

  case object Connected extends InboundCommand

  case object Disconnect extends OutboundCommand

  case object Disconnected extends InboundCommand

  case object Flush extends OutboundCommand

  case object EOF extends Exception("EOF") with InboundCommand with NoStackTrace {
    override def toString() = getMessage
  }

  case class Error(e: Throwable) extends Exception(e) with InboundCommand with OutboundCommand
}
