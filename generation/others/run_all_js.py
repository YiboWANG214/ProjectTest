import argparse
import csv
import time
import openai
import torch
import re
import os
import json
import subprocess
import shutil

from utils import (
    load_config,
    get_prompts,
    get_output_files,
    get_output_files_initial,
    get_output_files_fix,
    save_generated_code,
    save_response,
    get_mock_response,
)


def run_tests_and_collect_errors():
    """Run tests with coverage and return error messages."""
    try:
        results1 = subprocess.run(["npm", "install", "jest", "--save-dev", "jest-environment-jsdom", "@babel/core", "@babel/preset-env", "babel-jest"], text=True, capture_output=True)
        results = subprocess.run(["npm", "test", "--", "--coverage"], text=True, capture_output=True)
        return results.stdout+results.stderr  # No errors
    except subprocess.CalledProcessError as e:
        print("Error", e)
        return e


def get_classname(code: str) -> str:
    """
    Gets the name of the CUT from the test prompt.
    @param code: the test prompt or the original code (it assumes it starts with `// classname.java`)
    @return: the classname of the CUT or the unit test to be generated
    """
    return code.split("\n")[0][2:-8].strip()


def save_output_to_file(filename, content):
    """Save given content to a file."""
    with open(filename, "w") as file:
        file.write(content)

def save_to_file(filename, content):
    """Save the given content to a .py file."""
    with open(filename, "w") as file:
        file.write(content)


def generate_tests(
    prompts: list,
    gen_model: str,
) -> None:

    # sets the data output paths
    original_code_folder = "./dataset/JS"
    generated_test_folder = f"./generated_tests/{gen_model}_Data/ProjectTestPy_output/original_fix"
    output_folder_initial = f"./jstest/{gen_model}/ProjectTest/JS/original_fix"
    # opens output file in write mode (overwrite prior results)

    for prompt in prompts:
        print("PROMPT", prompt["id"])
        try:
            current_path = os.getcwd()
            print("Change current path to: ", os.path.join(output_folder_initial, prompt["classname"]))
            os.chdir(os.path.join(output_folder_initial, prompt["classname"]))
            # run coverage, obtain error messages, cd back
            print("run coverage")
            errors = run_tests_and_collect_errors()
            os.chdir(current_path)
            output_filename = os.path.join(os.path.join(output_folder_initial, prompt["classname"]), "test_line_output.txt")
            save_output_to_file(output_filename, errors)

        except Exception as e:
            print(e)
            print("ERROR", e)
            mock_response = get_mock_response(prompt, str(e))
            time.sleep(60)  # some sleep to make sure we don't go over rate limit
            # save_response(json_file, csv_file, prompt, prompts, mock_response)
        # break


def main():
    parser = argparse.ArgumentParser()
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
        choices=("GPT3.5", "GPT4", "Gemini", "Claude", "O1"),
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
    generate_tests(prompts, args.gen_model)


if __name__ == "__main__":
    main()
