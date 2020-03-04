package zio.interop

import _root_.monix.eval
import _root_.monix.execution.Scheduler
import zio.interop.monix._
import zio.test.Assertion._
import zio.test._
import zio.IO

import scala.util.Success

object MonixSpec extends DefaultRunnableSpec {

  implicit val scheduler: Scheduler = Scheduler(runner.platform.executor.asEC)

  override def spec = suite("MonixSpec")(
    suite("IO.fromTask")(
      testM("return an `IO` that fails if `Task` failed.") {
        val error = new Exception
        val task  = eval.Task.raiseError[Int](error)
        val io    = IO.fromTask(task).unit

        assertM(io.either)(isLeft(equalTo(error)))
      },
      testM("return an `IO` that produces the value from `Task`.") {
        val value = 10
        val task  = eval.Task(value)
        val io    = IO.fromTask(task)(scheduler)

        assertM(io.either)(isRight(equalTo(value)))
      }
    ),
    suite("IO.toTask")(
      testM("produce a successful `IO` of `Task`.") {
        val task = IO.fail(new Exception).toTask
        assertM(task.map(_.isInstanceOf[eval.Task[Unit]]))(isTrue)
      },
      testM("returns a `Task` that fails if `IO` fails.") {
        val error = new Exception
        val task  = IO.fail(error).toTask

        assertM(task)(equalTo(eval.Task.raiseError(error)))
      },
      testM("returns a `Task` that produces the value from `IO`.") {
        val value = 10
        val task  = IO.succeed(value).toTask.map(_.runSyncUnsafe())

        assertM(task)(equalTo(10))
      }
    ),
    suite("IO.fromCoeval")(
      testM("return an `IO` that fails if `Coeval` failed") {
        val error  = new Exception
        val coeval = eval.Coeval.raiseError[Int](error)
        val io     = IO.fromCoeval(coeval).unit

        assertM(io.either)(isLeft(equalTo(error)))
      },
      testM("return an `IO` that produces the value from `Coeval`.") {
        val value  = 10
        val coeval = eval.Coeval(value)
        val io     = IO.fromCoeval(coeval)

        assertM(io.either)(isRight(equalTo(value)))
      }
    ),
    suite("IO.toCoeval")(
      testM("produce a successful `IO` of `Coeval`") {
        val task = IO.fail(new Exception).toCoeval
        assertM(task.map(_.isInstanceOf[eval.Coeval[Unit]]))(isTrue)
      },
      testM("returns a `Coeval` that fails if `IO` fails.") {
        val error  = new Exception
        val coeval = IO.fail(error).toCoeval

        assertM(coeval)(equalTo(eval.Coeval.raiseError(error)))
      },
      testM("returns a `Coeval` that produces the value from `IO`.") {
        val value  = 10
        val coeval = IO.succeed(value).toCoeval.map(_.runTry)

        assertM(coeval)(equalTo(Success(10)))
      }
    )
  )
}
