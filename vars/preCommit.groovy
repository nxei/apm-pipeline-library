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

/**
  Run the pre-commit for the given commit if provided and generates the JUnit
  report if required

  preCommit(junit: false)

  preCommit(commit: 'abcdefg')
*/
def call(Map params = [:]) {
  def junitFlag = params.get('junit', true)
  def commit = params.get('commit', env.GIT_BASE_COMMIT)

  if (!commit?.trim()) {
    commit = env.GIT_BASE_COMMIT ?: error('preCommit: git commit to compare with is required.')
  }

  def reportFileName = 'pre-commit.out'

  sh """
    curl https://pre-commit.com/install-local.py | python -
    git diff-tree --no-commit-id --name-only -r ${commit} | xargs pre-commit run --files | tee ${reportFileName}
  """
  if(junitFlag) {
    preCommitToJunit(input: reportFileName, output: "${reportFileName}.xml")
    junit testResults: "${reportFileName}.xml", allowEmptyResults: true, keepLongStdio: true
  }
}