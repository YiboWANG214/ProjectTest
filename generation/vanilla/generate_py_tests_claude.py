import argparse
import csv
import time
import openai
import anthropic
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
GENAI_MODEL = "claude-3-5-sonnet-20241022"
OPENAI_TEMPERATURE = 0
OPENAI_TOP_P = 1
OPENAI_FREQUENCY_PENALTY = 0
OPENAI_PRESENCE_PENALTY = 0
ANTHROPIC_API_KEY = "."


def generate_code(prompt, max_tokens, language, is_fix=False):
    start_time = time.time()
    client = anthropic.Anthropic(
        api_key=ANTHROPIC_API_KEY
    )
    text = client.messages.create(
        model = GENAI_MODEL,
        system="You are a coding assistant. You generate only source code.",
        messages=[
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
        top_p=OPENAI_TOP_P,
        max_tokens=4096
        ).content[0].text
    response = dict()
    response['generated_text'] = text

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
        choices=("GPT3.5", "GPT4", "Gemini", "Claude"),
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
