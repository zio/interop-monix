# Monix Interoperability for ZIO

| Project Stage | CI | Release | Snapshot | Discord |
| --- | --- | --- | --- | --- |
| [![Project stage][Stage]][Stage-Page] | ![CI][Badge-CI] | [![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases] | [![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots] | [![Discord][Badge-Discord]][Link-Discord] |

This library provides interoperability between **Monix 3.4** and **ZIO 1 and ZIO 2**.

## Tasks

Monix tasks can be converted to ZIO tasks:

```scala
import zio._
import zio.interop.monix._
import monix.eval

val monixTask: eval.Task[String] = ???

val zioTask: Task[String] = ZIO.fromMonixTask(monixTask)
```

The conversion is lazy: the Monix task will only be executed if the returned ZIO task is executed.

ZIO tasks can be converted to Monix tasks:

```scala
import zio._
import zio.interop.monix._
import monix.eval
import monix.execution.Scheduler.Implicits.global

val zioTask: Task[String] = ???

val createMonixTask: UIO[eval.Task[String]] = zioTask.toMonixTask()

// illustrative, you wouldn't usually do things this way
val monixTask: eval.Task[String] = Runtime.default.unsafeRun(createMonixTask)
val stringResult = monixTask.runSyncUnsafe
```

The conversion is lazy: the ZIO effect so converted will only be executed if the returned Monix task is executed.

Sometimes you need to provide a Monix task in a context where using a ZIO effect is difficult. For example, when an API requires you to provide a function that returns a Monix task. In these situations, the `toMonixTaskUsingRuntime` method can be used:

```scala
import zio._
import zio.interop.monix._
import monix.eval

def monixBasedApi(f: String => eval.Task[Unit]): eval.Task[Unit] = ???

def zioBasedProcessor(s: String): Task[Unit] = ???

val zioEffects = for {
    zioRuntime <- ZIO.runtime[Any]
    monixTask = 
    _ <- ZIO.fromMonixTask {
        monixBasedApi(s =>
            zioBasedProcessor(s).toMonixTaskUsingRuntime(zioRuntime)
        )
    }
} yield ()
```

Cancellation/Interruption is propagated between the effect systems. Interrupting a ZIO task based on a Monix task will cancel the underlying Monix task and vice-versa. Be aware that ZIO interruption does not return until cancellation effects have completed, whereas Monix cancellation returns as soon as the signal is sent, without waiting for the cancellation effects to complete.

## Monix Scheduler

Sometimes it is useful to have a Monix `Scheduler` available for interop purposes. The `Runtime#monixScheduler` method will create a scheduler that shares its execution context with the ZIO runtime:

```scala
import zio._
import zio.interop.monix._
import monix.execution.Scheduler

ZIO.runtime[Any].flatMap { runtime =>
    implicit val monixScheduler: Scheduler = runtime.monixScheduler()

    // do Monixy things
}
```


[Badge-CI]: https://github.com/zio/interop-monix/workflows/CI/badge.svg
[Badge-Discord]: https://img.shields.io/discord/629491597070827530?logo=discord
[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/zio-interop-monix_2.12.svg
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/dev.zio/zio-interop-monix_2.12.svg
[Link-Discord]: https://discord.gg/2ccFBr4
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-interop-monix_2.12/
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/dev/zio/zio-interop-monix_2.12/
[Stage]: https://img.shields.io/badge/Project%20Stage-Production%20Ready-brightgreen.svg
[Stage-Page]: https://github.com/zio/zio/wiki/Project-Stages
