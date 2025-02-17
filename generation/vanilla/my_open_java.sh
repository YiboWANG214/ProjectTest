#!/bin/bash
export CUDA_VISIBLE_DEVICES=0,1
log_dir=./log_0125
mkdir -p $log_dir

declare -a models=(
                "16000 16000 deepseek-ai/deepseek-coder-6.7b-instruct DeepSeek" \
                "16000 16000 codellama/CodeLlama-7b-Instruct-hf CodeLlama" \
                "8000 8000 google/codegemma-7b-it CodeGemma" \
                "64000 64000 Qwen/CodeQwen1.5-7B-Chat Qwen"
                )

for temp in "${models[@]}"
do
    # Read the delimited string into an array
    IFS=' ' read -r -a params <<< "$temp"
    gen_tokens=${params[0]}
    input_tokens=${params[1]}
    model=${params[2]}
    model_nickname=${params[3]}

    echo "gen_tokens: $gen_tokens"
    echo "input_tokens: $input_tokens"
    echo "model: $model"
    echo "model_nickname: $model_nickname"

    # Construct log file name with model nickname for uniqueness
    log_file="$log_dir/${model_nickname}_${input_tokens}_java.log"

    # Execute Python script with parameters and log the output
    python3 generate_java_tests_open.py \
            -t $gen_tokens \
            -i $input_tokens \
            -m $model \
            -num 2 \
            -l java \
            -d ProjectTest \
            -g $model_nickname \
            -p ./ProjectTest/JAVA/original_prompt.json \
            2>&1 | tee $log_file
done
