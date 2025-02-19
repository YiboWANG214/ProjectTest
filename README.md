# ProjectTest

This is the repo for paper: ProjectTest: A Project-level LLM Unit Test Generation Benchmark and Impact of Error Fixing Mechanisms (https://arxiv.org/abs/2502.06556).

## Dataset
Dataset is in the folder "./dataset".

Dataset statistics:

| Language   | Avg. #Files | Avg. LOC | Avg. #Stars | Avg. #Forks |
|------------|------------|----------|-------------|-------------|
| Python     | 6.10       | 654.60   | 5810.30     | 996.90      |
| Java       | 4.65       | 282.60   | 3306.05     | 1347.65     |
| JavaScript | 4.00       | 558.05   | 17242.30    | 5476.45     |


Detailed information of each project is in Appendix A.

## Preprocessing
See data_preprocess.ipynb

The processed data is saved in the folder "./ProjectTest"

## Unit Test Generation
See the folder "./generation".

To generate unit tests for Python/JS/JAVA using API (GPT-3.5, GPT-4, O1, Gemini-1.5-Pro, Claude-3.5-Sonnet, etc), run the script \
```sh ./generation/vanilla/my_test.sh``` 

To generate unit tests for Python/JS/JAVA using open-source models (deepseek-coder-6.7b-instruct, CodeLlama-7b-Instruct-hf, codegemma-7b-it, CodeQwen1.5-7B-Chat, etc), run the script \
```sh ./generation/vanilla/my_open_py.sh```\
```sh ./generation/vanilla/my_open_js.sh```\
```sh ./generation/vanilla/my_open_java.sh```

The generated data is saved in the folder "./generated_tests"

## LLM Self-fixing
See the folder "./generation/self_fix".

To generate unit tests for Python/JS/JAVA using LLM self-fixing, run the script \
```sh ./generation/self_fix/my_test.sh```\
```sh ./generation/self_fix/my_open_py.sh```\
```sh ./generation/self_fix/my_open_js.sh```\
```sh ./generation/self_fix/my_open_java.sh```

## Evaluation
See the folders "./pytest", "./jstest", "./javatest"