package zio
package interop.monix

import duration._

import test._
import Assertion._
import TestAspect._

import _root_.monix.eval.{ Task => MTask }

object MonixTaskSpec extends DefaultRunnableSpec {

  override def spec =
    suite("MonixTaskSpec")(
      suite("ZIO.fromMonixTask")(
        testM("return a ZIO that fails if Monix task failed") {
          val error     = new Exception("monix task failed")
          val monixTask = MTask.raiseError(error)
          val io        = ZIO.fromMonixTask(monixTask)

          assertM(io.either)(isLeft(equalTo(error)))
        },
        testM("return a ZIO that succeeds if Monix task succeeded") {
          val result    = "monix task result"
          val monixTask = MTask.now(result)
          val io        = ZIO.fromMonixTask(monixTask)

          assertM(io)(equalTo(result))
        },
        testM("converts a synchronous Monix task to a ZIO task") {
          @volatile var testVar = 0
          val monixTask         = MTask.eval {
            val old = testVar
            testVar = old + 1
            old
          }
          val io                = for {
            orig    <- ZIO.fromMonixTask(monixTask)
            current <- UIO(testVar)
          } yield ((orig, current))
          assertM(io)(equalTo((0, 1)))
        },
        testM("converts an asynchronous Monix task to a ZIO task") {
          @volatile var testVar = 0
          val monixTask         = MTask.async[Int] { cb =>
            val old = testVar
            testVar = old + 1
            cb.onSuccess(old)
          }
          val io                = for {
            orig    <- ZIO.fromMonixTask(monixTask)
            current <- ZIO.succeed(testVar)
          } yield ((orig, current))
          assertM(io)(equalTo((0, 1)))
        },
        testM("the ZIO task fails if a Monix async task fails") {
          val error     = new Exception("async monix failure")
          val monixTask = MTask.async[Int] { cb =>
            cb.onError(error)
          }
          val io        = ZIO.fromMonixTask(monixTask)
          assertM(io.either)(isLeft(equalTo(error)))
        },
        testM("lazily executes a converted Monix task") {
          @volatile var executed = false
          val monixTask          = MTask.eval {
            executed = true
          }
          val test               = ZIO.succeed {
            executed = false
          } *> {
            ZIO.fromMonixTask(monixTask)
            ZIO.succeed(executed)
          }
          assertM(test)(isFalse)
        },
        testM("propagates cancellation from ZIO to Monix") {
          @volatile var cancelled = false
          @volatile var running   = false
          val monixTask           = MTask.eval {
            running = true
          } >> MTask.never.doOnCancel {
            MTask.eval {
              cancelled = true
            }
          }
          for {
            _            <- ZIO.succeed {
                              cancelled = false
                              running = false
                            }
            fiber        <- ZIO.fromMonixTask(monixTask).fork
            // wait until the monix task is running before interrupting the fiber
            _            <- ZIO.succeed(running).repeatUntil(Predef.identity)
            exit         <- fiber.interrupt
            wasCancelled <- ZIO.succeed(cancelled)
          } yield assertTrue(wasCancelled) && assertTrue(exit.interrupted)
        } @@ nonFlaky
      ),
      suite("ZIO#toMonixTask")(
        testM("converts a successful ZIO task to a Monix task") {
          @volatile var testVar = 0
          val io                = ZIO.succeed {
            val old = testVar
            testVar = old + 1
            old
          }
          for {
            monixTask <- io.toMonixTask
            result  <- ZIO.fromMonixTask(monixTask)
            current <- ZIO.succeed(testVar)
          } yield assertTrue(result == 0) && assertTrue(current == result + 1)
        },
        testM("converts a failed ZIO task to a failed Monix task") {
          val error = new Exception("ZIO operation failed")
          val io    = ZIO.fail(error)
          val test  = io.toMonixTask.flatMap[Any, Throwable, Nothing](ZIO.fromMonixTask(_)).either
          assertM(test)(isLeft(equalTo(error)))
        },
        testM("propagates cancellation from Monix to ZIO") {
          @volatile var cancelled = false
          @volatile var running   = false
          val io                  = ZIO.succeed {
            running = true
          } *> ZIO.never.onInterrupt {
            ZIO.succeed {
              cancelled = true
            }
          }
          val test                = for {
            monixTask    <- io.toMonixTask
            _            <- ZIO.fromMonixTask {
                              for {
                                fiber <- monixTask.start
                                _     <- MTask.eval(running).restartUntil(Predef.identity)
                                _     <- fiber.cancel
                              } yield ()
                            }
            // Monix `fiber.cancel` doesn't wait for all the cancellation effects
            // to complete, so `cancelled` might not be true at first
            wasCancelled <- ZIO.succeed(cancelled).repeatUntil(Predef.identity)
          } yield wasCancelled
          assertM(test)(isTrue)
        } @@ jvmOnly @@ timeout(5.seconds),
        testM("only executes the ZIO effect if the Monix task is executed") {
          @volatile var executed: Boolean = false
          val io                          = ZIO.succeed {
            executed = true
          }
          val test                        = for {
            _      <- ZIO.succeed {
                        executed = false
                      }
            _      <- io.toMonixTask
            result <- ZIO.succeed(executed)
          } yield result
          assertM(test)(isFalse)
        }
      )
    )
}
