#!/bin/bash
set -euo pipefail

cleanup() {
    echo "Interrupted. Cleaning up..."
    pkill -f OpinionDynamics || true
    exit 1
}
trap cleanup SIGINT SIGTERM

# ================================
# 1. コンパイル
# ================================
echo "Compiling Java sources..."

LIBCP="$(find lib -name '*.jar' | tr '\n' ':')"

rm -rf bin
mkdir -p bin

javac -cp "$LIBCP" -d bin src/**/*.java

echo "Compilation finished."

# ================================
# 2. 並列実行設定
# ================================
SEED_START=15
NUM_RUNS=5   # 10 最大並列数を超えたら、その分時間がかかる
MAX_PARALLEL=12       # ★ 最大並列数（重要）
JAVA_HEAP="2g"       # ★ 1プロセスあたりの最大ヒープ
LOGDIR="logs"

mkdir -p "$LOGDIR"

echo "Starting $NUM_RUNS simulations (max parallel = $MAX_PARALLEL)..."

# ================================
# 3. 並列実行（制御付き）
# ================================
run_one() {
    local seed=$1
    local logfile="${LOGDIR}/run_${seed}.log"

    echo "[START] seed=$seed $(date)" >> "$logfile"

    java -Xmx${JAVA_HEAP} \
         -XX:+ExitOnOutOfMemoryError \
         -cp "${LIBCP}:bin" \
         dynamics.OpinionDynamics "$seed" \
         >> "$logfile" 2>&1

    echo "[END]   seed=$seed $(date)" >> "$logfile"
}

export -f run_one
export LIBCP JAVA_HEAP LOGDIR

seq $SEED_START $((SEED_START + NUM_RUNS - 1)) \
  | xargs -n 1 -P "$MAX_PARALLEL" -I {} bash -c 'run_one "$@"' _ {}

echo "All simulations completed!"
