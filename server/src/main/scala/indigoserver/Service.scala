package indigoserver

import java.io.File

import cats.effect._
import cats.implicits._
import fs2._
import fs2.StreamApp.ExitCode
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.websocket._
import org.http4s.websocket.WebsocketBits._
import _root_.io.circe.syntax._
import _root_.io.circe.generic.auto._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.language.higherKinds

object MyService extends Service[IO]

abstract class Service[F[_]](implicit F: Effect[F]) extends StreamApp[F] with Http4sDsl[F] {

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference", "org.wartremover.warts.Nothing"))
  def route(scheduler: Scheduler): HttpService[F] = HttpService[F] {
    case GET -> Root / "hello" =>
      Ok("Hello world.")

    case GET -> Root / "ping" =>
      Ok("pong").map(_.replaceAllHeaders(Header("Access-Control-Allow-Origin", "*"), Header("Content-Type", "text/plain")))

    case GET -> Root / "game" / "id" / "definition" =>
      Ok(GameDetails.definition.asJson)

    case GET -> Root / "game" / "id" / "config" =>
      Ok(GameDetails.config.asJson)

    case GET -> Root / "game" / "id" / "assets" =>
      Ok(GameDetails.assets.asJson)

    case request @ GET -> Root / "game" / "id" / "assets" / path =>
      StaticFile
        .fromFile(new File("./server/assets/" + path), Some(request))
        .getOrElseF(NotFound())

    // ---------------------------------------
    // WebSockets

    case GET -> Root / "ws" =>
      val toClient: Stream[F, WebSocketFrame] =
        scheduler.awakeEvery[F](1.seconds).map(d => Text(s"Ping! $d"))

      val fromClient: Sink[F, WebSocketFrame] = _.evalMap { (ws: WebSocketFrame) =>
        ws match {
          case Text(t, _) => F.delay(println(t))
          case f          => F.delay(println(s"Unknown type: $f"))
        }
      }

      WebSocketBuilder[F].build(toClient, fromClient)

    case GET -> Root / "wsecho" =>
      val queue = async.unboundedQueue[F, WebSocketFrame]
      val echoReply: Pipe[F, WebSocketFrame, WebSocketFrame] = _.collect {
        case Text(msg, _) => Text("You sent the server: " + msg)
        case _            => Text("Something new")
      }

      queue.flatMap { q =>
        val d = q.dequeue.through(echoReply)
        val e = q.enqueue
        WebSocketBuilder[F].build(d, e)
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] =
    for {
      scheduler <- Scheduler[F](corePoolSize = 2)
      exitCode <- BlazeBuilder[F]
        .bindHttp(8080)
        .withWebSockets(true)
        .mountService(route(scheduler), "/")
        .serve
    } yield exitCode

}