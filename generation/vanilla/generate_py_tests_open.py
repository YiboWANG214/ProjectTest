import os
os.environ["CUDA_VISIBLE_DEVICES"]="0,1"

import argparse
import csv
import time
import openai

from utils import (
    load_config,
    get_prompts,
    get_output_files,
    save_generated_code,
    save_response,
    get_mock_response,
)

# Use a pipeline as a high-level helper
import transformers
from transformers import pipeline
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch
# import pprint
from transformers import BitsAndBytesConfig
import re

import vllm
from vllm import LLM, SamplingParams


OPENAI_TEMPERATURE = 0
OPENAI_TOP_P = 1


def generate_code(prompt, max_tokens, language, model, sampling_params, tokenizer, max_input_tokens, is_fix=False):

    start_time = time.time()
    system_prompt = "You are a coding assistant. You generate only source code."
    message =  "# "+prompt["classname"]+f".{language}\n" + prompt["original_code"] + "\n"
    if max_input_tokens != -1:
        message = tokenizer(message)
        message = tokenizer.decode(message.input_ids[:max_input_tokens])
    message = message + prompt["test_prompt"].strip() + "\n\t\t"
    messages = f'<s>[INST] <<SYS>>\n{system_prompt}\n<</SYS>>\n\n' + f'{message} [/INST]'

    # print("=====start generation=======")
    response = dict()
    with torch.no_grad():
        output = model.generate(messages, 
                                sampling_params,
                                )[0]
        generated_text = output.outputs[0].text
        response["generated_text"] = generated_text
    # print("=====end generation=======")
    time_taken = time.time() - start_time
    response["time_taken"] = time_taken
    if is_fix:
        response["prompt_id"] = prompt["prompt_id"]
    else:
        response["prompt_id"] = prompt["id"]
    response["original_code"] = prompt["original_code"]
    response["test_prompt"] = prompt["test_prompt"]
    return response


def generate_tests(
    config: dict,
    dataset: str,
    prompt_file: str,
    prompts: list,
    max_tokens: int,
    language: str,
    gen_model: str,
    model,
    sampling_params,
    tokenizer, 
    max_input_tokens,
) -> None:
    output_folder, scenario_folder, response_file, csv_file = get_output_files(
        config, dataset, prompt_file, max_tokens, gen_model
    )
    # opens output file in write mode (overwrite prior results)
    with open(response_file, "w") as json_file, open(csv_file, "w") as csv_out:
        csv_file = csv.writer(
            csv_out, delimiter=",", quotechar='"', quoting=csv.QUOTE_MINIMAL
        )
        csv_file.writerow(
            [
                "PROMPT_ID",
                "DURATION",
                "ORIGINAL_CODE",
                "TEST_PROMPT",
                "GENERATED_TEST",
            ]
        )
        json_file.write("[\n")
        for prompt in prompts:
            print("PROMPT", prompt["id"])
            try:
                # query Open AI to generate the unit test
                response = generate_code(prompt, max_tokens, language, model, sampling_params, tokenizer, max_input_tokens)
                # save the generated test in a file
                print("SAVING", prompt["id"], "at", scenario_folder)
                save_generated_code(prompt, response, max_tokens, scenario_folder, False, True)
                # save the response's metadata in CSV and JSON
                save_response(json_file, csv_file, prompt, prompts, response, False, True)
                print(
                    "Duration: ",
                    response["time_taken"],
                    "\n" + "-" * 30,
                )

            except Exception as e:
                print(e)
                print("ERROR", e)
                mock_response = get_mock_response(prompt, str(e))
                time.sleep(60)

        json_file.write("]")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "-t",
        "--tokens",
        type=int,
        choices=[x * 1000 for x in range(1, 5)],
        help="token limit (ex: 1000)",
        required=True,
    )
    parser.add_argument(
        "-i",
        "--input_tokens",
        type=int,
        help="maximum input tokens",
        required=True,
    )
    parser.add_argument(
        "-p",
        "--prompts",
        type=str,
        help="path to the JSON file with prompts",
        required=True,
    )
    parser.add_argument(
        "-d",
        "--dataset",
        type=str,
        choices=("SF110", "GitHub", "HumanEval", "ClassEval", "ProjectTest"),
        help="The dataset being used",
        required=True,
    )
    parser.add_argument(
        "-g",
        "--gen_model",
        type=str,
        choices=("CodeLlama", "StarCoder", "DeepSeek"),
        help="The model being used",
        required=True,
    )
    parser.add_argument(
        "-num",
        "--num_gpus",
        type=int,
        help="The number of gpus",
        required=True,
    )
    parser.add_argument(
        "-l",
        "--language",
        type=str,
        choices=("py", "java", "c", "cpp", "js"),
        help="The programming language being used",
        required=True,
    )

    args = parser.parse_args()

    tokenizer = AutoTokenizer.from_pretrained("codellama/CodeLlama-7b-Instruct-hf")

    sampling_params = SamplingParams(temperature=OPENAI_TEMPERATURE, 
                                     top_p=OPENAI_TOP_P,
                                     max_tokens=args.tokens
                                     )
    model = LLM(
                "codellama/CodeLlama-7b-Instruct-hf",
                tensor_parallel_size=args.num_gpus, 
                )

    config = load_config("config.json")
    print(args)
    # get list of parsed prompts from the JSON file
    prompts = get_prompts(config, args.prompts)

    print("Generating unit tests for", len(prompts), "prompts in", args.dataset)
    # generate unit tests
    generate_tests(config, args.dataset, args.prompts, prompts, args.tokens, args.language, args.gen_model, model, sampling_params, tokenizer, args.input_tokens)


if __name__ == "__main__":
    main()
