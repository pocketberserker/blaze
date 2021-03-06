package org.http4s.blaze.pipeline.stages

import org.specs2.mutable._

import org.http4s.blaze.pipeline.Command._
import scala.concurrent.Future
import org.http4s.blaze.pipeline._
import org.http4s.blaze.util.Execution
import scala.util.Failure
import scala.Some
import scala.util.Success


/**
 * @author Bryce Anderson
 *         Created on 1/26/14
 *
 *         What a mess. Its almost a full blown implementation of something to test this
 */
class HubStageSpec extends Specification {

  implicit val ec = Execution.directec

  case class Msg(k: Int, msg: String)

  val msgs = Msg(1, "one")::Msg(2, "two")::Nil

  class TestHubStage(builder: () => LeafBuilder[Msg]) extends HubStage[Msg, Msg, Int](builder) {

    override protected def stageStartup(): Unit = {
      super.stageStartup()
      reqLoop()
    }

    private def reqLoop(): Unit = channelRead().onComplete {
      case Success(msg) =>
        val k = msg.k
        getNode(k) match {
          case Some(node) => node.sendMsg(msg)
          case None =>
            val n = makeAndInitNode(k)
            n.sendMsg(msg)
        } 
      
        reqLoop()

      case Failure(EOF) => 
        logger.trace("Finished.")
        closeAllNodes()

      case Failure(t)   => throw t
    }

    protected def nodeReadRequest(key: Int, size: Int): Unit = {}

    protected def onNodeWrite(key: Int, data: Msg): Future[Unit] = channelWrite(data)

    protected def onNodeWrite(key: Int, data: Seq[Msg]): Future[Unit] = channelWrite(data)

    protected def onNodeCommand(key: Int, cmd: OutboundCommand): Unit = {
      logger.trace(s"Received command $cmd")
      cmd match {
        case Disconnect => removeNode(key)
        case _ => sendOutboundCommand(cmd)
      }
    }
  }

  class Echo extends TailStage[Msg] {
    def name: String = "EchoTest"

    override protected def stageStartup(): Unit = {
      readLoop()
    }

    private def readLoop(): Unit = channelRead().onComplete {
      case Success(msg) =>
        channelWrite(Msg(msg.k, "Echoing: " + msg.msg))
          .onSuccess{ case _ => readLoop() }

      case Failure(EOF) => logger.debug("Received EOF")
      case _ => sys.error("Shouldn't get here!")
    }
  }

  def nodeBuilder(): LeafBuilder[Msg] = LeafBuilder(new Echo)

  def rootBuilder(): LeafBuilder[Msg] = LeafBuilder(new TestHubStage(nodeBuilder))

  "HubStage" should {

    "Initialize" in {
      val h = new SeqHead(msgs)

      rootBuilder().base(h)

      h.inboundCommand(Connected)
      h.inboundCommand(Disconnected)
      // All the business should have finished because it was done using the directec

      h.results should_== Vector(Msg(1, "Echoing: one"), Msg(2, "Echoing: two"))
    }
  }

}
