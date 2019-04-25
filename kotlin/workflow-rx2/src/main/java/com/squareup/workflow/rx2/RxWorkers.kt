/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.rx2

import com.squareup.workflow.Worker
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.reactive.openSubscription
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.openSubscription

inline fun <reified T : Any> Observable<T>.asWorker(key: String = ""): Worker<T> =
  Worker.fromChannel(key) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    openSubscription()
  }

inline fun <reified T : Any> Flowable<T>.asWorker(key: String = ""): Worker<T> =
  Worker.fromChannel(key) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    openSubscription()
  }

inline fun <reified T : Any> Maybe<T>.asWorker(key: String = ""): Worker<T> =
  Worker.fromNullable(key) { await() }

inline fun <reified T : Any> Single<T>.asWorker(key: String = ""): Worker<T> =
  Worker.from(key) { await() }