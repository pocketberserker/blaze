package org.http4s.blaze.channel.nio1

import java.nio.channels._
import scala.annotation.tailrec
import java.net.SocketAddress
import org.http4s.blaze.channel._
import scala.util.control.NonFatal
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * @author Bryce Anderson
 *         Created on 1/19/14
 */
abstract class NIOServerChannelFactory[Channel <: NetworkChannel](pool: SelectorLoopPool)
                extends ServerChannelFactory[Channel] with LazyLogging {

  def this(fixedPoolSize: Int, bufferSize: Int = 8*1024) = this(new FixedArraySelectorPool(fixedPoolSize, bufferSize))

  protected def doBind(address: SocketAddress): Channel

  protected def acceptConnection(ch: Channel, loop: SelectorLoop): Boolean

  protected def makeSelector: Selector = Selector.open()

  protected def createServerChannel(channel: Channel): ServerChannel =
    new NIO1ServerChannel(channel, pool)

  def bind(localAddress: SocketAddress = null): ServerChannel = createServerChannel(doBind(localAddress))


  /** This class can be extended to change the way selector loops are provided */
  protected class NIO1ServerChannel(val channel: Channel, pool: SelectorLoopPool) extends ServerChannel {

    type C = Channel

    @volatile private var closed = false

    override def close(): Unit = {
      closed = true
      pool.shutdown()
      channel.close()
    }

    // The accept thread just accepts connections and pawns them off on the SelectorLoop pool
    final def run(): Unit = {
      while (channel.isOpen && !closed) {
        try {
          val p = pool.nextLoop()
          acceptConnection(channel, p)
        } catch {
          case NonFatal(t) => logger.error("Error accepting connection", t)
        }
      }
    }

  }
}
