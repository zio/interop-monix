package zio.interop

import _root_.monix.eval
import _root_.monix.execution.Scheduler
import zio.interop.monix._
import zio.test.Assertion._
import zio.test._
import zio.{ IO, Runtime }

import scala.util.{ Success, Try }

object MonixSpec
    extends DefaultRunnableSpec({
      val runtime = Runtime((), DefaultTestRunner.platform)

      suite("MonixSpec2")(
        suite("IO.fromTask")(
          testM("return an `IO` that fails if `Task` failed.") {
            implicit val scheduler: Scheduler                 = Scheduler(runtime.platform.executor.asEC)
            val error                                         = new Exception
            val task                                          = eval.Task.raiseError[Int](error)
            val io                                            = IO.fromTask(task).unit
            val assertion: Assertion[Either[Throwable, Unit]] = equalTo(Left(error))
            assertM(io.either)(assertion)
          },
          testM("return an `IO` that produces the value from `Task`.") {
            implicit val scheduler: Scheduler                = Scheduler(runtime.platform.executor.asEC)
            val value                                        = 10
            val task                                         = eval.Task(value)
            val io                                           = IO.fromTask(task)(scheduler)
            val assertion: Assertion[Either[Throwable, Int]] = equalTo(Right(value))
            assertM(io.either)(assertion)
          }
        ),
        suite("IO.toTask")(
          testM("produce a successful `IO` of `Task`.") {
            val task = IO.fail(new Exception).toTask
            assertM(task.map(_.isInstanceOf[eval.Task[Unit]]))(isTrue)
          },
          testM("returns a `Task` that fails if `IO` fails.") {
            val error                                    = new Exception
            val task                                     = IO.fail(error).toTask
            val assertion: Assertion[eval.Task[Nothing]] = equalTo(eval.Task.raiseError(error))
            assertM(task)(assertion)
          },
          testM("returns a `Task` that produces the value from `IO`.") {
            implicit val scheduler: Scheduler = Scheduler(runtime.platform.executor.asEC)
            val value                         = 10
            val task                          = IO.succeed(value).toTask.map(_.runSyncUnsafe())
            assertM(task)(equalTo(10))
          }
        ),
        suite("IO.fromCoeval")(
          testM("return an `IO` that fails if `Coeval` failed") {
            val error                                         = new Exception
            val coeval                                        = eval.Coeval.raiseError[Int](error)
            val io                                            = IO.fromCoeval(coeval).unit
            val assertion: Assertion[Either[Throwable, Unit]] = equalTo(Left(error))
            assertM(io.either)(assertion)
          },
          testM("return an `IO` that produces the value from `Coeval`.") {
            val value                                        = 10
            val coeval                                       = eval.Coeval(value)
            val io                                           = IO.fromCoeval(coeval)
            val assertion: Assertion[Either[Throwable, Int]] = equalTo(Right(value))
            assertM(io.either)(assertion)
          }
        ),
        suite("IO.toCoeval")(
          testM("produce a successful `IO` of `Coeval`") {
            val task = IO.fail(new Exception).toCoeval
            assertM(task.map(_.isInstanceOf[eval.Coeval[Unit]]))(isTrue)
          },
          testM("returns a `Coeval` that fails if `IO` fails.") {
            val error                                      = new Exception
            val coeval                                     = IO.fail(error).toCoeval
            val assertion: Assertion[eval.Coeval[Nothing]] = equalTo(eval.Coeval.raiseError(error))
            assertM(coeval)(assertion)
          },
          testM("returns a `Coeval` that produces the value from `IO`.") {
            val value                          = 10
            val coeval                         = IO.succeed(value).toCoeval.map(_.runTry)
            val assertion: Assertion[Try[Int]] = equalTo(Success(10))
            assertM(coeval)(assertion)
          }
        )
      )
    })
