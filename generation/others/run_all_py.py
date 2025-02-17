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


def run_tests_line_and_collect_errors():
    """Run tests with coverage and return error messages."""
    try:
        results = subprocess.run(["coverage", "run", "-m", "pytest"], text=True, capture_output=True)
        return results.stdout  # No errors
    except subprocess.CalledProcessError as e:
        print("Error", e)
        return e

def run_line_coverage_report():
    """Run tests with coverage and return error messages."""
    try:
        # coverage html --omit="*_test.py" -d tests/coverage
        results = subprocess.run(["coverage", "html", "--omit=*_test.py", "-d", "line_tests/coverage"], text=True, capture_output=True)
        return results.stdout  # No errors
    except subprocess.CalledProcessError as e:
        print("Error", e)
        return e

def run_tests_branch_and_collect_errors():
    """Run tests with coverage and return error messages."""
    try:
        results = subprocess.run(["coverage", "run", "--branch", "-m", "pytest"], text=True, capture_output=True)
        return results.stdout  # No errors
    except subprocess.CalledProcessError as e:
        print("Error", e)
        return e

def run_branch_coverage_report():
    """Run tests with coverage and return error messages."""
    try:
        # coverage html --omit="*_test.py" -d tests/coverage
        results = subprocess.run(["coverage", "html", "--omit=*_test.py", "-d", "branch_tests/coverage"], text=True, capture_output=True)
        return results.stdout  # No errors
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
    original_code_folder = "/home/yibo/Desktop/TestGeneration/ProjectEval/Python"
    generated_test_folder = f"/home/yibo/Desktop/TestGeneration/PyUnit/{gen_model}_Data_fix3_1/ProjectEvalPy_output/original_fix"
    output_folder_initial = f"/home/yibo/Desktop/TestGeneration/PyUnit/pytest/{gen_model}_fix3_1/ProjectEval/Python/original_fix"
    # opens output file in write mode (overwrite prior results)

    for prompt in prompts:
        print("PROMPT", prompt["id"])
        try:
            print("Change current path to: ", os.path.join(output_folder_initial, prompt["classname"]))
            os.chdir(os.path.join(output_folder_initial, prompt["classname"]))
            # run coverage, obtain error messages, cd back
            print("run coverage")
            errors_line = run_tests_line_and_collect_errors()
            run_line_coverage_report()
            errors_branch = run_tests_branch_and_collect_errors()
            run_branch_coverage_report()
            os.chdir("/home/yibo/Desktop/TestGeneration/PyUnit")
            output_line_filename = os.path.join(os.path.join(output_folder_initial, prompt["classname"]), "test_line_output.txt")
            save_output_to_file(output_line_filename, errors_line)
            output_branch_filename = os.path.join(os.path.join(output_folder_initial, prompt["classname"]), "test_branch_output.txt")
            save_output_to_file(output_branch_filename, errors_branch)

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
        choices=("SF110", "GitHub", "HumanEval", "ClassEval", "ProjectEval"),
        help="The dataset being used",
        required=True,
    )
    parser.add_argument(
        "-g",
        "--gen_model",
        type=str,
        # choices=("GPT3.5", "GPT4", "Gemini", "Claude"),
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

    print("Generating unit tests for", len(prompts), "prompts in", args.dataset, "for model: ", args.gen_model)
    # generate unit tests
    generate_tests(prompts, args.gen_model)


if __name__ == "__main__":
    main()
