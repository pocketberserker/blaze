package org.http4s.blaze
package examples

import java.nio.ByteBuffer

import scala.concurrent.Future
import org.http4s.blaze.pipeline.stages.http.{SimpleHttpResponse, HttpServerStage}
import org.http4s.blaze.http.http_parser.BaseExceptions.BadRequest

/**
 * @author Bryce Anderson
 *         Created on 1/5/14
 */
class ExampleHttpServerStage(maxRequestLength: Int) extends HttpServerStage(maxRequestLength) {
  def handleRequest(method: String, uri: String, headers: Seq[(String, String)], body: ByteBuffer): Future[SimpleHttpResponse] = {

    if (uri.endsWith("error")) Future.failed(new BadRequest("You requested an error!"))
    else {
      val body = ByteBuffer.wrap(s"Hello world!\nRequest URI: $uri".getBytes())
      Future.successful(SimpleHttpResponse("OK", 200, Nil, body))
    }
  }
}
