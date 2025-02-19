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
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class WithGCPEnvStepTests extends ApmBasePipelineTest {
  def script

  @Override
  @Before
  void setUp() throws Exception {
    super.setUp()
    helper.registerAllowedMethod('isInstalled', [Map.class], { return true })
    helper.registerAllowedMethod('fileExists', [Map.class], { return false })
    script = loadScript('vars/withGCPEnv.groovy')
  }

  @Test
  void test_missing_credentialsId_or_secret() throws Exception {
    testMissingArgument('credentialsId or secret', 'parameters are required') {
      script.call() {
        // NOOP
      }
    }
  }

  @Test
  void test_with_credentials() throws Exception {
    def ret = false
    script.call(credentialsId: 'foo') {
      ret = true
    }
    printCallStack()
    assertTrue(ret)
    assertTrue(assertMethodCallContainsPattern('withCredentials', ''))
    assertTrue(assertMethodCallContainsPattern('sh', 'gcloud auth activate-service-account --key-file ${FILE_CREDENTIAL}'))
    assertTrue(assertMethodCallContainsPattern('withEnv', 'PATH+GSUTIL'))
    assertFalse(assertMethodCallContainsPattern('sh', "wget -q -O"))
    assertJobStatusSuccess()
  }

  @Test
  void test_with_secret() throws Exception {
    def ret = false
    try {
    script.call(secret: VaultSecret.SECRET_GCP_PROVISIONER.toString()) {
      ret = true
    }
    } catch (e) {
      println e
    }
    printCallStack()
    assertTrue(ret)
    assertFalse(assertMethodCallContainsPattern('withCredentials', ''))
    assertTrue(assertMethodCallContainsPattern('sh', 'gcloud auth activate-service-account --key-file'))
    assertTrue(assertMethodCallContainsPattern('sh', 'rm'))
    assertJobStatusSuccess()
  }

  @Test
  void test_with_failed() throws Exception {
    helper.registerAllowedMethod('cmd', [Map.class], { m -> throw new Exception('force a failure') })
    def result = false
    try {
      script.call(credentialsId: 'foo') {
        result = true
      }
    } catch(err) {
      println err
      // NOOP
    }
    printCallStack()
    assertFalse(result)
  }

  @Test
  void test_without_gh_installed_by_default_with_wget() throws Exception {
    helper.registerAllowedMethod('isInstalled', [Map.class], { m -> return m.tool.equals('wget') })
    def result = false
    script.call(credentialsId: 'foo') {
      result = true
    }
    printCallStack()
    assertTrue(result)
    assertTrue(assertMethodCallContainsPattern('withEnv', 'PATH+GSUTIL'))
    assertTrue(assertMethodCallContainsPattern('sh', 'wget -q -O gsutil.tar.gz https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-319.0.0-linux-x86_64.tar.gz'))
    assertJobStatusSuccess()
  }

  @Test
  void test_without_gh_installed_by_default_no_wget_no_curl() throws Exception {
    helper.registerAllowedMethod('isInstalled', [Map.class], { return false })
    def result = false
    script.call(credentialsId: 'foo') {
      result = true
    }
    printCallStack()
    assertTrue(result)
    assertFalse(assertMethodCallContainsPattern('sh', 'wget -q -O'))
    assertFalse(assertMethodCallContainsPattern('sh', 'curl'))
    assertJobStatusSuccess()
  }

  @Test
  void test_without_gh_installed_by_default_no_wget() throws Exception {
    helper.registerAllowedMethod('isInstalled', [Map.class], { m -> return !(m.tool.equals('wget') || m.tool.equals('gsutil'))})
    def result = false
    script.call(credentialsId: 'foo') {
      result = true
    }
    printCallStack()
    assertTrue(result)
    assertFalse(assertMethodCallContainsPattern('sh', 'wget -q -O gsutil.tar.gz'))
    assertTrue(assertMethodCallContainsPattern('sh', 'curl -sSLo gsutil.tar.gz --retry 3 --retry-delay 2 --max-time 10'))
    assertJobStatusSuccess()
  }

  @Test
  void test_windows() throws Exception {
    helper.registerAllowedMethod('isUnix', [], { false })
    def result = false
    script.call(credentialsId: 'foo') {
      result = true
    }
    printCallStack()
    assertTrue(result)
    assertTrue(assertMethodCallContainsPattern('withCredentials', ''))
    assertTrue(assertMethodCallContainsPattern('bat', 'gcloud auth activate-service-account --key-file %FILE_CREDENTIAL%'))
    assertTrue(assertMethodCallContainsPattern('withEnv', 'PATH+GSUTIL'))
    assertFalse(assertMethodCallContainsPattern('bat', "wget -q -O"))
    assertJobStatusSuccess()
  }

  @Test
  void test_without_gh_installed_by_default_with_wget_in_windows() throws Exception {
    helper.registerAllowedMethod('isInstalled', [Map.class], { m -> return m.tool.equals('wget') })
    helper.registerAllowedMethod('isUnix', [], { false })
    def result = false
    script.call(credentialsId: 'foo') {
      result = true
    }
    printCallStack()
    assertTrue(result)
    assertTrue(assertMethodCallContainsPattern('withEnv', 'PATH+GSUTIL'))
    assertTrue(assertMethodCallContainsPattern('bat', 'wget -q -O gsutil.zip https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-319.0.0-windows-x86_64-bundled-python.zip'))
    assertJobStatusSuccess()
  }
}
