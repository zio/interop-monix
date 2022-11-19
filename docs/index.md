---
id: index
title: "Introduction to ZIO Interop Monix"
sidebar_label: "ZIO Interop Monix"
---

This library provides interoperability between **Monix 3.4** and **ZIO 1 and ZIO 2**. Both JVM and Scala.js are supported.

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
