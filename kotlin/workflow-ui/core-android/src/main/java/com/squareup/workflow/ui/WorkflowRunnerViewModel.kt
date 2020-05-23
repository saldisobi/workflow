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
package com.squareup.workflow.ui

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import com.squareup.workflow.RenderingAndSnapshot
import com.squareup.workflow.Snapshot
import com.squareup.workflow.launchWorkflowIn2
import com.squareup.workflow.ui.WorkflowRunner.Config
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.annotations.TestOnly

internal class WorkflowRunnerViewModel<OutputT : Any>(
  private val scope: CoroutineScope,
  private val result: Deferred<OutputT>,
  private val renderingsAndSnapshots: StateFlow<RenderingAndSnapshot<Any>>
) : ViewModel(), WorkflowRunner<OutputT>, SavedStateProvider {

  internal class Factory<PropsT, OutputT : Any>(
    private val savedStateRegistry: SavedStateRegistry,
    private val configure: () -> Config<PropsT, OutputT>
  ) : ViewModelProvider.Factory {
    private val snapshot = savedStateRegistry
        .consumeRestoredStateForKey(BUNDLE_KEY)
        ?.getParcelable<PickledWorkflow>(BUNDLE_KEY)
        ?.snapshot

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val config = configure()
      // TODO this will always fail, do it for real.
      val props = config.props as StateFlow<PropsT>
      val scope = CoroutineScope(config.dispatcher)
      val result = CompletableDeferred<OutputT>(parent = scope.coroutineContext[Job])

      val renderingsAndSnapshots = launchWorkflowIn2(
          scope, config.workflow, props,
          initialSnapshot = snapshot,
          diagnosticListener = config.diagnosticListener
      ) { output ->
        result.complete(output)
        // Cancel the entire workflow runtime after the first output is emitted.
        scope.cancel(CancellationException("WorkflowRunnerViewModel delivered result"))
      }

      @Suppress("UNCHECKED_CAST")
      return WorkflowRunnerViewModel(scope, result, renderingsAndSnapshots).also {
        savedStateRegistry.registerSavedStateProvider(BUNDLE_KEY, it)
      } as T
    }
  }

  override suspend fun awaitResult(): OutputT = result.await()

  private val lastSnapshot: Snapshot get() = renderingsAndSnapshots.value.snapshot

  @OptIn(ExperimentalCoroutinesApi::class)
  override val renderings: StateFlow<Any> = renderingsAndSnapshots
      // TODO state-friendly map
      .map { it.rendering }

  override fun onCleared() {
    scope.cancel(CancellationException("WorkflowRunnerViewModel cleared."))
  }

  override fun saveState() = Bundle().apply {
    putParcelable(BUNDLE_KEY, PickledWorkflow(lastSnapshot))
  }

  @TestOnly
  internal fun clearForTest() = onCleared()

  @TestOnly
  internal fun getLastSnapshotForTest() = lastSnapshot

  private companion object {
    /**
     * Namespace key, used in two namespaces:
     *  - associates the [WorkflowRunnerViewModel] with the [SavedStateRegistry]
     *  - and is also the key for the [PickledWorkflow] in the bundle created by [saveState].
     */
    val BUNDLE_KEY = WorkflowRunner::class.java.name + "-workflow"
  }
}
