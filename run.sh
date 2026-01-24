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
SEED_START=10
NUM_RUNS=10   # 10 最大並列数を超えたら、その分時間がかかる
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
    
    # --- 追加: 前半・後半でターゲットを切り替えるロジック ---
    # 閾値を計算 (例: start=15, runs=10 なら limit=20)
    # seed 15,16,17,18,19 -> 1.0
    # seed 20,21,22,23,24 -> -1.0
    local limit=$((SEED_START + NUM_RUNS / 2))
    local target_opinion="1.0"
    
    if [ "$seed" -ge "$limit" ]; then
        target_opinion="-1.0"
    fi
    # ----------------------------------------------------

    echo "[START] seed=$seed target=$target_opinion $(date)" >> "$logfile"

    java -Xmx${JAVA_HEAP} \
         -XX:+ExitOnOutOfMemoryError \
         -cp "${LIBCP}:bin" \
         dynamics.OpinionDynamics "$seed" "$target_opinion" \
         >> "$logfile" 2>&1

    echo "[END]   seed=$seed target=$target_opinion $(date)" >> "$logfile"
}

export -f run_one
export LIBCP JAVA_HEAP LOGDIR SEED_START NUM_RUNS

seq $SEED_START $((SEED_START + NUM_RUNS - 1)) \
  | xargs -n 1 -P "$MAX_PARALLEL" -I {} bash -c 'run_one "$@"' _ {}

echo "All simulations completed!"
