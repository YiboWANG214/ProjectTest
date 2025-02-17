import argparse
import csv
import time
import openai
import torch

from utils import (
    load_config,
    get_prompts,
    get_output_files,
    save_generated_code,
    save_response,
    get_mock_response,
)

# Code Generation Configuration Parameters
# OPENAI_MODEL = "gpt-3.5-turbo"
# OPENAI_MODEL = "gpt-4-turbo"
OPENAI_MODEL = "o1"
OPENAI_TEMPERATURE = 0
OPENAI_TOP_P = 1
OPENAI_FREQUENCY_PENALTY = 0
OPENAI_PRESENCE_PENALTY = 0


def generate_code(prompt, max_tokens, language, is_fix=False):
    start_time = time.time()
    if max_tokens == -1:
        response = openai.ChatCompletion.create(
            model=OPENAI_MODEL,
            messages=[
                {
                    "role": "system",
                    "content": "You are a coding assistant. You generate only source code.",
                },
                {
                    "role": "user",
                    "content": "# "+prompt["classname"]+f".{language}\n"
                    + prompt["original_code"]
                    + "\n"
                    + prompt["test_prompt"].strip()
                    + "\n\t\t",
                },
            ],
            top_p=OPENAI_TOP_P,
            frequency_penalty=OPENAI_FREQUENCY_PENALTY,
            presence_penalty=OPENAI_PRESENCE_PENALTY,
            seed=42
        )
    else:
        response = openai.ChatCompletion.create(
            model=OPENAI_MODEL,
            messages=[
                {
                    "role": "system",
                    "content": "You are a coding assistant. You generate only source code.",
                },
                {
                    "role": "user",
                    "content": "# "+prompt["classname"]+f".{language}\n"
                    + prompt["original_code"]
                    + "\n"
                    + prompt["test_prompt"].strip()
                    + "\n\t\t",
                },
            ],
            temperature=OPENAI_TEMPERATURE,
            max_tokens=max_tokens,
            top_p=OPENAI_TOP_P,
            frequency_penalty=OPENAI_FREQUENCY_PENALTY,
            presence_penalty=OPENAI_PRESENCE_PENALTY,
            seed=42
        )

    time_taken = time.time() - start_time
    response["time_taken"] = time_taken
    if is_fix:
        response["prompt_id"] = prompt["prompt_id"]
    else:
        response["prompt_id"] = prompt["id"]
    response["original_code"] = prompt["original_code"]
    response["test_prompt"] = prompt["test_prompt"]

    if time_taken <= 60:
        time.sleep(60 - time_taken + 20)
    return response


def generate_tests(
    config: dict,
    dataset: str,
    prompt_file: str,
    prompts: list,
    max_tokens: int,
    language: str,
    gen_model: str,
) -> None:
    output_folder, scenario_folder, response_file, csv_file = get_output_files(
        config, dataset, prompt_file, max_tokens, gen_model, language
    )
    # opens output file in write mode (overwrite prior results)
    with open(response_file, "w") as json_file, open(csv_file, "w") as csv_out:
        csv_file = csv.writer(
            csv_out, delimiter=",", quotechar='"', quoting=csv.QUOTE_MINIMAL
        )
        csv_file.writerow(
            [
                "ID",
                "PROMPT_ID",
                "DURATION",
                "FINISH_REASON",
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
                response = generate_code(prompt, max_tokens, language)
                # save the generated test in a file
                print("SAVING", prompt["id"], "at", scenario_folder)
                save_generated_code(prompt, response, max_tokens, scenario_folder, True)
                # save the response's metadata in CSV and JSON
                save_response(json_file, csv_file, prompt, prompts, response, True)
                print(
                    "Duration: ",
                    response["time_taken"],
                    "Finish Reason:",
                    response["choices"][0]["finish_reason"],
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
        help="token limit (ex: 1000)",
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
        choices=("GPT3.5", "GPT4", "O1"),
        help="The API being used",
        required=True,
    )
    parser.add_argument(
        "-l",
        "--language",
        type=str,
        choices=("Py", "java", "c", "cpp", "js"),
        help="The programming language being used",
        required=True,
    )

    args = parser.parse_args()

    config = load_config("config.json")
    print(args)
    seed = 42
    torch.cuda.manual_seed_all(seed)
    # get list of parsed prompts from the JSON file
    prompts = get_prompts(config, args.prompts)

    print("Generating unit tests for", len(prompts), "prompts in", args.dataset)
    # generate unit tests
    generate_tests(config, args.dataset, args.prompts, prompts, args.tokens, args.language, args.gen_model)


if __name__ == "__main__":
    main()
