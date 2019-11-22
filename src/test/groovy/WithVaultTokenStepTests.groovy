// Licensed to Elasticsearch B.V. under one or more contributor
// license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright
// ownership. Elasticsearch B.V. licenses this file to you under
// the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import org.junit.Before
import org.junit.Test
import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.junit.Assert.assertTrue

class WithVaultTokenStepTests extends ApmBasePipelineTest {
  String scriptName = 'vars/withVaultToken.groovy'

  @Override
  @Before
  void setUp() throws Exception {
    super.setUp()
    env.WORKSPACE = '/foo'
  }

  @Test
  void testDefaultParameters() throws Exception {
    def script = loadScript(scriptName)
    def isOK = false
    script.call {
      isOK = true
    }
    printCallStack()
    assertTrue(isOK)
    assertTrue(helper.callStack.findAll { call ->
      call.methodName == 'dir'
    }.any { call ->
      callArgsToString(call).contains('/foo')
    })
    assertTrue(helper.callStack.findAll { call ->
      call.methodName == 'sh'
    }.any { call ->
      callArgsToString(call).contains('rm .vault-token')
    })
    assertJobStatusSuccess()
  }

  @Test
  void testDefaultParametersAndWindows() throws Exception {
    helper.registerAllowedMethod('isUnix', [ ], { false })
    def script = loadScript(scriptName)
    def isOK = false
    script.call {
      isOK = true
    }
    printCallStack()
    assertTrue(isOK)
    assertTrue(helper.callStack.findAll { call ->
      call.methodName == 'bat'
    }.any { call ->
      callArgsToString(call).contains('del .vault-token')
    })
    assertJobStatusSuccess()
  }

  @Test
  void testWithAllParameters() throws Exception {
    def script = loadScript(scriptName)
    def isOK = false
    script.call(path: '/bar', tokenFile: 'mytoken') {
      isOK = true
    }
    printCallStack()
    assertTrue(isOK)
    assertTrue(helper.callStack.findAll { call ->
      call.methodName == 'dir'
    }.any { call ->
      callArgsToString(call).contains('/bar')
    })
    assertTrue(helper.callStack.findAll { call ->
      call.methodName == 'sh'
    }.any { call ->
      callArgsToString(call).contains('rm mytoken')
    })
    assertJobStatusSuccess()
  }

  @Test
  void testWithBodyError() throws Exception {
    def script = loadScript(scriptName)
    try {
      script.call {
        throw new Exception('Mock an error')
      }
    } catch(e){
      //NOOP
    }
    printCallStack()
    assertTrue(helper.callStack.findAll { call ->
      call.methodName == 'error'
    }.any { call ->
      callArgsToString(call).contains('withVaultToken: error')
    })
    assertTrue(helper.callStack.findAll { call ->
      call.methodName == 'sh'
    }.any { call ->
      callArgsToString(call).contains('rm .vault-token')
    })
    assertJobStatusFailure()
  }
}