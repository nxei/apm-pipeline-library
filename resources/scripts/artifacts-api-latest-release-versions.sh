#!/usr/bin/env bash
# Licensed to Elasticsearch B.V. under one or more contributor
# license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright
# ownership. Elasticsearch B.V. licenses this file to you under
# the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http:#www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

#
# It queries the artifacts-api entry point to fetch all the existing
# release versions.
# It prints the output in the console in addition to the file
# latest-release-versions.json
#
# Since it prints the output in the console avoid any cosmetic changes
# with echo or set -x
#
set -eo pipefail

URL="https://artifacts-api.elastic.co/v1"

OUTPUT=latest-release-versions.json

curl -s ${URL}/versions | jq -r '.versions[] | select(contains("SNAPSHOT")|not)' | jq -R . | jq -s '. | sort' | tee ${OUTPUT}