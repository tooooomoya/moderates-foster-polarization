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

# findで見つからなかった場合のエラー回避
LIBCP="$(find lib -name '*.jar' 2>/dev/null | tr '\n' ':')bin"

rm -rf bin
mkdir -p bin

# 依存ライブラリがない場合も考慮してコンパイル
if [ -n "$(find lib -name '*.jar' 2>/dev/null)" ]; then
    javac -cp "$LIBCP" -d bin src/**/*.java
else
    javac -d bin src/**/*.java
fi

echo "Compilation finished."

# ================================
# 2. 並列実行設定
# ================================
SEED_START=15
NUM_SEEDS=10           # ★ Seedの数（各Seedで2回実行するので合計実行数は 2 * NUM_SEEDS になります）
MAX_PARALLEL=10       # ★ 最大並列数
JAVA_HEAP="2g"
LOGDIR="logs"

mkdir -p "$LOGDIR"

echo "Starting simulations for $NUM_SEEDS seeds (x2 targets = $((NUM_SEEDS * 2)) runs)..."

# ================================
# 3. 並列実行関数
# ================================
run_one() {
    local seed=$1
    local target=$2
    
    # ログファイル名にターゲットも含めて区別する
    # 例: run_10_pos.log, run_10_neg.log
    local label="pos"
    if [ "$target" = "-1.0" ]; then
        label="neg"
    fi
    local logfile="${LOGDIR}/run_${seed}_${label}.log"

    echo "[START] seed=$seed target=$target $(date)" > "$logfile"

    # Java実行 (クラスパス設定に注意: LIBCPに既にbinが含まれていないか確認)
    # ここでは既存のLIBCP + :bin としています
    java -Xmx${JAVA_HEAP} \
         -XX:+ExitOnOutOfMemoryError \
         -cp "${LIBCP}:bin" \
         dynamics.OpinionDynamics "$seed" "$target" \
         >> "$logfile" 2>&1

    echo "[END]   seed=$seed target=$target $(date)" >> "$logfile"
}

export -f run_one
export LIBCP JAVA_HEAP LOGDIR

# ================================
# 4. タスク生成と実行
# ================================
# Seed範囲: SEED_START から (SEED_START + NUM_SEEDS - 1) まで
# 各Seedについて 1.0 と -1.0 の組み合わせを出力し、xargsで並列実行

seq $SEED_START $((SEED_START + NUM_SEEDS - 1)) | \
while read -r seed; do
    echo "$seed"
    echo "1.0"
    echo "$seed"
    echo "-1.0"
done | xargs -n 2 -P "$MAX_PARALLEL" bash -c 'run_one "$1" "$2"' _

echo "All simulations completed!"