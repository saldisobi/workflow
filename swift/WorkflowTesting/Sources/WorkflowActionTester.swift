/*
 * Copyright 2020 Square Inc.
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

// WorkflowTesting only available in Debug mode.
//
// `@testable import Workflow` will fail compilation in Release mode.
#if DEBUG

    @testable import Workflow

    extension WorkflowAction {
        /// Returns a state tester containing `self`.
        public static func tester(withState state: WorkflowType.State) -> WorkflowActionTester<WorkflowType, Self> {
            return WorkflowActionTester(state: state)
        }
    }

    /// Testing helper that chains event sending and state/output assertions
    /// to make tests easier to write.
    ///
    /// ```
    /// reducer
    ///     .tester(withState: .firstState)
    ///     .assertState { state in
    ///         XCTAssertEqual(.firstState, state)
    ///     }
    ///     .send(event: .exampleEvent) { output in
    ///         XCTAssertEqual(.finished, output)
    ///     }
    ///     .assertState { state in
    ///         XCTAssertEqual(.differentState, state)
    ///     }
    /// ```
    public struct WorkflowActionTester<WorkflowType, Action> where Action: WorkflowAction, Action.WorkflowType == WorkflowType {
        /// The current state
        internal let state: WorkflowType.State

        /// Initializes a new state tester
        fileprivate init(state: WorkflowType.State) {
            self.state = state
        }

        /// Sends an event to the reducer.
        ///
        /// - parameter event: The event to send.
        ///
        /// - parameter outputAssertions: An optional closure that runs validations on the output generated by the reducer.
        ///
        /// - returns: A new state tester containing the state after the update.
        @discardableResult
        public func send(action: Action, outputAssertions: (WorkflowType.Output?) -> Void = { _ in }) -> WorkflowActionTester<WorkflowType, Action> {
            var newState = state
            let output = action.apply(toState: &newState)

            outputAssertions(output)

            return WorkflowActionTester(state: newState)
        }

        /// Invokes the given closure (which is intended to contain test assertions) with the current state.
        ///
        /// - parameter assertions: A closure that accepts a single state value.
        ///
        /// - returns: A tester containing the current state.
        @discardableResult
        public func assertState(_ assertions: (WorkflowType.State) -> Void) -> WorkflowActionTester<WorkflowType, Action> {
            assertions(state)
            return self
        }
    }

#endif
