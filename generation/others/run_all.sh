#!/bin/bash
#$ -M x@y.edu   # Email address for job notification
#$ -m abe            # Send mail when job begins, ends and aborts
#$ -pe smp 1     # Specify parallel environment and legal core size
#$ -q long           # Specify queue
#$ -N  RQ1_SF110_2K_4K

python3 run_all_js.py -l js -d ProjectTest -g GPT3.5 -p ./ProjectTest/JS/original_prompt.json

