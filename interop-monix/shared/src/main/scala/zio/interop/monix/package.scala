package zio
package interop

import _root_.monix.eval.{ Task => MTask }
import _root_.monix.execution.{ Scheduler => MScheduler, ExecutionModel }

/**
 * Monix interoperability for ZIO.
 *
 *   - Monix tasks can be converted to ZIO tasks via
 *     `ZIO.fromMonixTask(monixTask)`.
 *   - ZIO tasks can be converted to Monix tasks via `zioTask.toMonixTask`.
 */
package object monix {

  implicit final class ZIOObjOps(private val unused: ZIO.type) extends AnyVal {

    def monixScheduler(
      executionModel: ExecutionModel = ExecutionModel.Default
    )(implicit trace: Trace): UIO[MScheduler] =
      ZIO.executor.map(e => MScheduler(e.asExecutionContext, executionModel))

    /**
     * Converts a Monix task into a ZIO task.
     *
     * Interrupting the returned effect will cancel the underlying Monix task.
     * The conversion is lazy: the Monix task is only executed if the returned
     * ZIO task is executed.
     *
     * If the returned ZIO task is interrupted, the underlying Monix task will be
     * cancelled.
     *
     * @param monixTask
     *   The Monix task.
     * @param executionModel
     *   The Monix execution model to use for the Monix execution. This only
     *   need be specified if you want to override the Monix default.
     */
    def fromMonixTask[A](monixTask: MTask[A], executionModel: ExecutionModel = ExecutionModel.Default)(implicit
      trace: Trace
    ): Task[A] =
      ZIO.monixScheduler(executionModel).flatMap { implicit scheduler =>
        ZIO.asyncInterrupt[Any, Throwable, A] { cb =>
          try
          // runSyncStep will try to execute the Monix effects synchronously
          // if it fails before hitting an async boundary, the failure will be thrown
          monixTask.runSyncStep match {
            case Right(result)        =>
              // Monix task ran synchronously and successfully
              Right(ZIO.succeedNow(result))
            case Left(asyncMonixTask) =>
              // Monix task hit an async boundary, so we have to use the callback (cb)
              // and return a cancelation effect
              val cancelable = asyncMonixTask.runAsync { result =>
                val zioEffect = ZIO.fromEither(result)
                cb(zioEffect)
              }
              Left(ZIO.succeed(cancelable.cancel()))
          } catch {
            // Monix task failed during synchronous execution
            case e: Throwable => Right(ZIO.fail(e))
          }
        }
      }

  }

  implicit final class ExtraZioEffectOps[-R, +A](private val effect: ZIO[R, Throwable, A]) extends AnyVal {

    /**
     * Converts this ZIO effect into a Monix task.
     *
     * The conversion is lazy: this effect will only be executed if the returned Monix task
     * is executed.
     * If the returned Monix task is cancelled, the underlying ZIO effect will
     * be interrupted.
     */
    def toMonixTask(implicit trace: Trace): URIO[R, MTask[A]] = ZIO.runtime[R].map(toMonixTaskUsingRuntime)

    /**
     * Converts this ZIO effect into a Monix task using a specified ZIO runtime
     * to run the ZIO effect.
     *
     * This is useful in situations where a Monix task is needed, but executing ZIO effects
     * is incovenient. For example, when using a library API that requires you to pass a
     * function that returns a Monix task. In such situations, you can acquire the ZIO
     * runtime up-front and use this method to create the Monix task.
     *
     * The conversion is lazy: this effect will only be executed if the returned Monix task
     * is executed.
     * If the returned Monix task is cancelled, the underlying ZIO effect will
     * be interrupted.
     */
    def toMonixTaskUsingRuntime(zioRuntime: Runtime[R])(implicit trace: Trace): MTask[A] =
      MTask.cancelable { cb =>
        val cancelable = zioRuntime.unsafeRunAsyncCancelable(effect) { exit =>
          exit.fold(failed => cb.onError(failed.squash), cb.onSuccess)
        }
        MTask.eval(cancelable(FiberId.None)).void
      }

  }

}
