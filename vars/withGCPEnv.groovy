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
Configure the GCP context to run the given body closure

withGCPEnv(credentialsId: 'foo') {
  // block
}
*/
def call(Map args = [:], Closure body) {
  def credentialsId = args.containsKey('credentialsId') ? args.credentialsId : ''
  def secret = args.containsKey('secret') ? args.secret : ''

  if (!credentialsId.trim() && !secret.trim()) {
    error('withGCPEnv: credentialsId or secret parameters are required.')
  }

  def gsUtilLocation = pwd(tmp: true)
  def gsUtilLocationWin = "${gsUtilLocation}/google-cloud-sdk"
  def secretFileLocation = "${gsUtilLocation}/google-cloud-credentials.json"

  withEnv(["PATH+GSUTIL=${gsUtilLocation}", "PATH+GSUTIL_BIN=${gsUtilLocation}/bin",
           "PATH+GSUTILWIN=${gsUtilLocationWin}", "PATH+GSUTILWIN_BIN=${gsUtilLocationWin}/bin"]) {
    if(!isInstalled(tool: 'gsutil', flag: '--version')) {
      downloadInstaller(gsUtilLocation)
    }

    if (secret) {
      def props = getVaultSecret(secret: secret)
      if (props?.errors) {
        error "withGCPEnv: Unable to get credentials from the vault: ${props.errors.toString()}"
      }
      def value = props?.data
      def credentialsContent = value?.credentials
      if (!credentialsContent?.trim()) {
        error "withGCPEnv: Unable to read the credentials value"
      }
      writeFile(file: secretFileLocation, text: credentialsContent)
      gcloudAuth(secretFileLocation)
    } else {
      withCredentials([file(credentialsId: credentialsId, variable: 'FILE_CREDENTIAL')]) {
        gcloudAuth(isUnix() ? '${FILE_CREDENTIAL}' : '%FILE_CREDENTIAL%')
      }
    }
    try {
      if (secret) {
        // Somehow the login works for google bucket integrations but something
        // it's not right when using the VM creation with terraform and GCP
        // Setting GOOGLE_APPLICATION_CREDENTIALS seems to be the workaround
        // https://cloud.google.com/docs/authentication/getting-started
        withEnv(["GOOGLE_APPLICATION_CREDENTIALS=${secretFileLocation}"]){
          body()
        }
      } else {
        body()
      }
    } finally {
      if (fileExists("${secretFileLocation}")) {
        if(isUnix()){
          sh "rm ${secretFileLocation}"
        } else {
          bat "del ${secretFileLocation}"
        }
      }
    }
  }
}

def gcloudAuth(keyFile) {
  cmd(label: 'authenticate', script: 'gcloud auth activate-service-account --key-file ' + keyFile)
}

def downloadInstaller(where) {
  def url = googleCloudSdkURL()
  def tarball = "gsutil.${isUnix() ? 'tar.gz' : 'zip'}"

  dir(where) {
    if (!downloadWithWget(url: url, output: tarball)) {
      downloadWithCurl(url: url, output: tarball)
    }
    uncompress(tarball)
  }
}

def googleCloudSdkURL() {
  def url = 'https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-319.0.0'
  def arch = is64() ? 'x86_64' : 'x86'
  if (isUnix()) {
    return "${url}-linux-${arch}.tar.gz"
  } else {
    // use the bundled python artifact to avoid issues with the existing python2 installation when running gsutil in some Windows versions
    return "${url}-windows-${arch}-bundled-python.zip"
  }
}

def uncompress(tarball) {
  if (isUnix()) {
    sh(label: 'untar gsutil', script: "tar -xpf ${tarball} --strip-components=1")
  } else {
    unzip(quiet: true, zipFile: tarball)
  }
}
