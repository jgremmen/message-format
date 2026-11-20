#!/usr/bin/env bash
#
# Copyright 2026 Jeroen Gremmen
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Compares the JMH benchmarks of the current working tree with a baseline commit.
#
# Usage: compare.sh [-r <git ref>] [-t <threads,...>] [-- <JMH options>]
#
#   -r  baseline commit, branch or tag (default: HEAD, i.e. the working tree changes are compared to the last commit)
#   -t  comma separated thread counts (default: 1,4,<number of CPUs>)
#
# Everything after -- is passed to JMH, e.g. a benchmark name regex or shorter runs: -- FormatterLookup -wi 1 -i 3
#
# The baseline is built from a detached git worktree in .build/benchmark, which is removed afterwards. The
# benchmarks of the working tree run against both versions, so they must only use API present in the baseline.
# Baseline and current runs alternate per thread count, and which of them runs first alternates as well, so slow drift
# of the machine state (e.g. thermal throttling) does not favor one of them.

set -euo pipefail

root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
ref=HEAD
threads="1,4,$(getconf _NPROCESSORS_ONLN)"
jmh_options=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -r) ref="$2"; shift 2 ;;
    -t) threads="$2"; shift 2 ;;
    --) shift; jmh_options=("$@"); break ;;
    *) echo "usage: $0 [-r <git ref>] [-t <threads,...>] [-- <JMH options>]" >&2; exit 2 ;;
  esac
done

work="$root/.build/benchmark"
worktree="$work/baseline-worktree"
baseline_jar="$work/baseline.jar"
baseline_commit=$(git -C "$root" rev-parse --short "$ref^{commit}")

mkdir -p "$work"
rm -f "$work"/*.csv "$work"/*.log

# build the baseline jar
git -C "$root" worktree remove --force "$worktree" 2>/dev/null || true
rm -rf "$worktree"  # leftovers not known to git, e.g. directories created by an IDE
git -C "$root" worktree add --detach --quiet "$worktree" "$baseline_commit"
trap 'git -C "$root" worktree remove --force "$worktree"' EXIT

# local, untracked build settings (e.g. toolchain locations) apply to the baseline build as well
[[ -f "$root/gradle.properties" ]] && cp "$root/gradle.properties" "$worktree/"

echo "Building baseline $baseline_commit ..."
(cd "$worktree" && ./gradlew --quiet :message-format:jar)

find "$worktree/message-format/.build/libs" -name 'message-format-*.jar' \
    ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-test-fixtures.jar' -exec cp {} "$baseline_jar" \;
[[ -f "$baseline_jar" ]] || { echo "baseline jar not found" >&2; exit 1; }

run_jmh() {  # <result base name> <threads> [gradle options...]
  local result="$1" t="$2"
  shift 2

  (cd "$root" && ./gradlew --quiet :message-format-benchmark:jmh "$@" \
      -Pjmh.args="-t $t -rf csv -rff $result.csv ${jmh_options[*]:-}") > "$result.log" 2>&1 ||
    { echo "benchmark failed, see $result.log" >&2; exit 1; }
}

run_baseline() {  # <threads>
  echo "Running baseline ($baseline_commit) with $1 thread(s) ..."
  run_jmh "$work/baseline-t$1" "$1" -PbaselineJar="$baseline_jar"
}

run_current() {  # <threads>
  echo "Running working tree with $1 thread(s) ..."
  run_jmh "$work/current-t$1" "$1"
}

baseline_first=true

for t in ${threads//,/ }; do
  if $baseline_first; then
    run_baseline "$t"
    run_current "$t"
    baseline_first=false
  else
    run_current "$t"
    run_baseline "$t"
    baseline_first=true
  fi
done

# join the results by benchmark and thread count
echo
echo "Baseline: $baseline_commit, current: working tree (scores in benchmark units, higher is better for thrpt)"
echo

for t in ${threads//,/ }; do
  awk -F, -v t="$t" '
    FNR == 1 { next }
    { gsub(/"/, ""); sub(/^.*\.benchmark\./, "", $1); key = $1 " " $2 }
    FILENAME ~ /baseline-/ { base[key] = $5; unit[key] = $7; order[++n] = key; next }
    { cur[key] = $5; err[key] = $6 }
    END {
      for(i = 1; i <= n; i++) {
        k = order[i]
        change = base[k] > 0 ? sprintf("%+.1f%%", (cur[k] - base[k]) * 100 / base[k]) : "n/a"
        printf "%-44s %3s  %14.3f  %14.3f ± %-10.3f %9s  %s\n", k, t, base[k], cur[k], err[k], change, unit[k]
      }
    }' "$work/baseline-t$t.csv" "$work/current-t$t.csv"
done | { printf "%-44s %3s  %14s  %27s %9s  %s\n" "Benchmark (mode)" "Thr" "Baseline" "Current" "Change" "Unit"; cat; }

echo
echo "Raw results: $work"
