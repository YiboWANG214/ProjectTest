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
import anthropic

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

# Code Generation Configuration Parameters
GENAI_MODEL = "claude-3-5-sonnet-20241022"
OPENAI_TEMPERATURE = 0
OPENAI_TOP_P = 1
OPENAI_FREQUENCY_PENALTY = 0
OPENAI_PRESENCE_PENALTY = 0
ANTHROPIC_API_KEY = "."


def obtain_history(prompt, max_tokens, language, response, is_fix=False):
    start_time = time.time()
    messages=[
        {
            "role": "user",
            "content": "# "+prompt["classname"]+f".{language}\n"
            + prompt["original_code"]
            + "\n"
            + prompt["test_prompt"].strip()
            + "\n\t\t",
        },
    ]

    messages.append({"role": "assistant", "content": response['generated_text']})

    return messages



def fix_code(prompt, error_messages, conversation_history, original_code, max_tokens, language, is_fix=False):
    start_time = time.time()
    fix_prompt = f"Here are the error messages from the tests:\n{error_messages}\n\nErrors exist in the generated unit tests.\n\nPlease fix the unit tests to address these errors and provide the entire unit tests."
    messages = conversation_history + [
        {"role": "user", "content": fix_prompt}
    ]
    client = anthropic.Anthropic(
        api_key=ANTHROPIC_API_KEY
    )
    text = client.messages.create(
        model = GENAI_MODEL,
        system="You are a coding assistant. You generate only source code.",
        messages=messages,
        temperature=OPENAI_TEMPERATURE,
        top_p=OPENAI_TOP_P,
        max_tokens=4096
        ).content[0].text
    # print(text)
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


def fix1(code):
    """
    delete "```python" at the begining, and "```" at the end ==> H2
    """
    pattern = r"[\S\s.]*?\`\`\`python([\S\s.]*?)\`\`\`[\S\s.]*?"
    good_code = re.findall(pattern, code, re.DOTALL)
    if len(good_code) > 0:
        code = good_code[0]
        for i in range(1, len(good_code)):
            code += good_code[i]
    if code.count("if __name__ == '__main__':\n    unittest.main()") > 1:
        code = code.split("if __name__ == '__main__':\n    unittest.main()")
        code = "\n".join(code) + "\nif __name__ == '__main__':\n    unittest.main()"
    return code


def get_classname(code: str) -> str:
    return code.split("\n")[0][2:-8].strip()


def extract_codes(response):
    old_test = response["generated_text"]
    function_file = response["prompt_id"].split('/')[-1].split('.')[0]
    test_classname = get_classname(response["test_prompt"].strip())
    old_test = fix1(old_test)
    return old_test


def save_output_to_file(filename, content):
    """Save given content to a file."""
    with open(filename, "w") as file:
        file.write(content)


def save_initial_extracted_code(prompt: str, code: str, output_folder: str, isGPT3=False) -> None:
    filename = f"{prompt}_test.py"
    # create the output folder if needed
    if not os.path.exists(output_folder): os.makedirs(output_folder)

    with open(os.path.join(output_folder, filename), "w") as gen_file:
        if isGPT3:
            gen_file.write(code)
        else:
            print("isGPT3 error")



def save_to_file(filename, content):
    """Save the given content to a .py file."""
    with open(filename, "w") as file:
        file.write(content)


def generate_tests(
    config: dict,
    dataset: str,
    prompt_file: str,
    prompts: list,
    max_tokens: int,
    language: str,
    gen_model: str,
) -> None:
    output_folder_initial, scenario_folder_initial, response_file_initial, csv_file_initial = get_output_files_initial(
        config, dataset, prompt_file, max_tokens, gen_model, language
    )
    output_folder_fix, scenario_folder_fix, response_file_fix, csv_file_fix = get_output_files_fix(
        config, dataset, prompt_file, max_tokens, gen_model, language
    )
    original_code_folder = "./dataset/Python"
    generated_test_folder = f"./generated_tests/{gen_model}_Data/ProjectTestPy_output/original_fix"
    output_folder_initial = f"./pytest/{gen_model}_initial3/ProjectTest/Python/original_fix"
    output_folder_fix = f"./pytest/{gen_model}_fix3/ProjectTest/Python/original_fix"
    tests_output = json.load(open(f"./generated_tests/{gen_model}_Data/ProjectTestPy_output/original_output_-1.json"))
    # opens output file in write mode (overwrite prior results)
    with open(response_file_fix, "w") as json_file, open(csv_file_fix, "w") as csv_out:
        csv_file_fix = csv.writer(
            csv_out, delimiter=",", quotechar='"', quoting=csv.QUOTE_MINIMAL
        )
        csv_file_fix.writerow(
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
                current_path = os.getcwd()
                initial_response = ""
                for test_dict in tests_output:
                    test_classname = test_dict["prompt_id"].split('/')[-1].split('.')[0]
                    if test_classname == prompt["classname"]:
                        initial_response = test_dict
                        break
                conversation_history = obtain_history(prompt, max_tokens, language, initial_response)
                full_path = os.path.join(original_code_folder, prompt["classname"])
                if os.path.exists(os.path.join(output_folder_initial, prompt["classname"])):
                    shutil.rmtree(os.path.join(output_folder_initial, prompt["classname"]))
                shutil.copytree(full_path, os.path.join(output_folder_initial, prompt["classname"]))
                test_file = os.path.join(generated_test_folder, prompt["classname"]+"_test.py")
                if os.path.isfile(test_file):
                    print(prompt["classname"])
                    shutil.copy2(test_file, os.path.join(output_folder_initial, prompt["classname"]))
                print("Change current path to: ", os.path.join(output_folder_initial, prompt["classname"]))
                os.chdir(os.path.join(output_folder_initial, prompt["classname"]))
                # run coverage, obtain error messages, cd back
                print("run coverage")
                errors = run_tests_line_and_collect_errors()
                run_line_coverage_report()
                errors2 = run_tests_branch_and_collect_errors()
                run_branch_coverage_report()
                os.chdir(current_path)
                output_filename = os.path.join(os.path.join(output_folder_initial, prompt["classname"]), "test_output.txt")
                save_output_to_file(output_filename, errors)
                
                # query Open AI to fix the errors from initial response
                print("fix code")
                response = fix_code(prompt, errors, conversation_history, initial_response['generated_text'], max_tokens, language)
                # extract codes from fixed response
                print("extract fix code")
                fixed_codes = extract_codes(response)
                save_initial_extracted_code(prompt["classname"], fixed_codes, scenario_folder_fix, True)
                # move project and test to ./pytest/GPT4_fix, cd to that path
                full_path = os.path.join(original_code_folder, prompt["classname"])
                if os.path.exists(os.path.join(output_folder_fix, prompt["classname"])):
                    shutil.rmtree(os.path.join(output_folder_fix, prompt["classname"]))
                shutil.copytree(full_path, os.path.join(output_folder_fix, prompt["classname"]))
                save_to_file(os.path.join(os.path.join(output_folder_fix, prompt["classname"]), prompt["classname"] + "_test.py"), fixed_codes)
                print("Change current path again to: ", os.path.join(output_folder_fix, prompt["classname"]))
                os.chdir(os.path.join(output_folder_fix, prompt["classname"]))
                # run coverage, save outputs, cd back
                print("run coverage")
                outputs = run_tests_line_and_collect_errors()
                run_line_coverage_report()
                errors2 = run_tests_branch_and_collect_errors()
                run_branch_coverage_report()
                os.chdir(current_path)
                # save outputs
                output_filename = os.path.join(os.path.join(output_folder_fix, prompt["classname"]), "test_output.txt")
                save_output_to_file(output_filename, outputs)
                # save the generated test in a file
                print("SAVING", prompt["id"], "at", scenario_folder_fix)
                save_generated_code(prompt, response, max_tokens, scenario_folder_fix, False, True)
                # save the response's metadata in CSV and JSON
                save_response(json_file, csv_file_fix, prompt, prompts, response, False, True)
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
