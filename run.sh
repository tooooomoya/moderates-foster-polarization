#!/bin/bash

# ================================
# 1. コンパイル
# ================================
echo "Compiling Java sources..."
javac -cp "$(find lib -name '*.jar' | tr '\n' ':')" -d bin src/**/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed. Stop."
    exit 1
fi
echo "Compilation finished."


# ================================
# 2. 並列実行設定
# ================================
NUM_RUNS=10   # 0～9 の10回
LOGDIR="logs"

mkdir -p $LOGDIR

echo "Starting $NUM_RUNS parallel simulations..."


# ================================
# 3. 並列実行ループ（Seed固定）
# ================================
for seed in $(seq 0 9); do
    echo "  Run with seed=$seed"

    java -cp "$(find lib -name '*.jar' | tr '\n' ':'):bin" dynamics.OpinionDynamics $seed \
        > "${LOGDIR}/run_${seed}.log" &

    # プロセスを密集して起動しないように少し待つ
    sleep 0.1
done

# 全部終わるまで待機
wait

echo "All simulations completed!"
