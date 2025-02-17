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
import threading

import string

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

os.environ["TOKENIZERS_PARALLELISM"] = "false"
OPENAI_TEMPERATURE = 0
OPENAI_TOP_P = 1
OPENAI_FREQUENCY_PENALTY = 0
OPENAI_PRESENCE_PENALTY = 0


def obtain_history(model_name, prompt, max_tokens, language, response, tokenizer, max_input_tokens, is_fix=False):
    start_time = time.time()
    system_prompt = "You are a coding assistant. You generate only source code."
    message =  "# "+prompt["classname"]+f".{language}\n" + prompt["original_code"] + "\n"

    if max_input_tokens != -1:
        tokens_num = max_input_tokens - len(tokenizer(system_prompt).input_ids) - len(tokenizer(prompt["test_prompt"].strip() + "\n\t\t").input_ids) - 50
        message = tokenizer.encode(message, truncation=True, max_length=tokens_num)
        print("extracted length: ", tokens_num, len(message))
        print("max: ", max_input_tokens, "system prompt: ", len(tokenizer(system_prompt).input_ids), "test prompt: ", len(tokenizer(prompt["test_prompt"].strip() + "\n\t\t").input_ids), "message: ", len(message))
        message = tokenizer.decode(message[:min(tokens_num, len(message))])

    message = message + prompt["test_prompt"].strip() + "\n\t\t"
    if model_name == "google/codegemma-7b-it":
        # messages = f'<s>[INST] <<SYS>>\n{system_prompt}\n<</SYS>>\n\n' + f'{message} [/INST]'
        messages=[
            {
                "role": "user",
                "content": system_prompt + message
            },
        ]
    else:
        messages=[
            {
                "role": "system",
                "content": system_prompt,
            },
            {
                "role": "user",
                "content": message
            },
        ]
    # print(response)
    messages.append({"role": "assistant", "content": response["generated_text"][-1]["content"]})

    return messages


def fix_code(model, tokenizer, model_name, prompt, error_messages, conversation_history, original_code, max_tokens, max_input_tokens, language, sampling_params, is_fix=False):
    """
    Returns a response object from OpenAI enriched with the prompt metadata.
    @param max_tokens: what is the token size limit used
    @param is_fix: True if we are generating code for a previous prompt that had an error
    @param prompt: the prompt object
    """
    start_time = time.time()
    fix_prompt = f"Here are the error messages from the tests:\n{error_messages}\n\nErrors exist in the generated unit tests.\n\nPlease fix the unit tests to address these errors and provide the entire unit tests."
    response = {}
    if max_input_tokens != -1:
        # priority: system_prompt, generated_tests (initial response), fix_prompt, initial prompt
        if model_name == "google/codegemma-7b-it":
            initial_response_len = min(len(tokenizer(conversation_history[1]["content"]).input_ids), max_input_tokens - 2000)
            initial_response = tokenizer.encode(conversation_history[1]["content"], truncation=True, max_length=initial_response_len)
            initial_response = tokenizer.decode(initial_response[:initial_response_len])
            fix_prompt_len = min(len(tokenizer(fix_prompt).input_ids), max_input_tokens - initial_response_len - 2000)
            fix_prompt = tokenizer.encode(fix_prompt, truncation=True, max_length=fix_prompt_len)
            fix_prompt = tokenizer.decode(fix_prompt[:fix_prompt_len])
            initial_prompt_len = len(tokenizer(conversation_history[0]["content"]).input_ids)
            token_nums = max_input_tokens - initial_response_len - fix_prompt_len - 2000
            if token_nums < 0:
                token_nums = 0
                print("error: token_nums is less than 0")
            message = tokenizer.encode(conversation_history[0]["content"], truncation=True, max_length=token_nums)
            message_len = len(message)
            print("extracted length: ", token_nums, message_len)
            print("max: ", max_input_tokens, "initial prompt: ", initial_prompt_len, "initial response: ", initial_response_len, "fix prompt length: ", fix_prompt_len, "message: ", len(message))
            message = tokenizer.decode(message[:min(token_nums, message_len)])
            print("build new message:", initial_prompt_len, min(token_nums, message_len), initial_response_len, fix_prompt_len)
            print("new message maximum length: ", max_input_tokens - min(token_nums, message_len) - initial_response_len - fix_prompt_len)
            
            messages = [
                {"role": "user", "content": message},
                {"role": "assistant", "content": initial_response},
                {"role": "user", "content": fix_prompt}
            ]
        else:
            system_prompt_len = len(tokenizer(conversation_history[0]["content"]).input_ids)
            initial_response_len = min(len(tokenizer(conversation_history[2]["content"]).input_ids), max_input_tokens - system_prompt_len - 2000)
            initial_response = tokenizer.encode(conversation_history[2]["content"], truncation=True, max_length=initial_response_len)
            initial_response = tokenizer.decode(initial_response[:initial_response_len])
            fix_prompt_len = min(len(tokenizer(fix_prompt).input_ids), max_input_tokens - system_prompt_len - initial_response_len - 2000)
            fix_prompt = tokenizer.encode(fix_prompt, truncation=True, max_length=fix_prompt_len)
            fix_prompt = tokenizer.decode(fix_prompt[:fix_prompt_len])
            initial_prompt_len = len(tokenizer(conversation_history[1]["content"]).input_ids)
            token_nums = max_input_tokens - system_prompt_len - initial_response_len - fix_prompt_len - 2000
            if token_nums < 0:
                token_nums = 0
                print("error: token_nums is less than 0")
            message = tokenizer.encode(conversation_history[1]["content"], truncation=True, max_length=token_nums)
            message_len = len(message)
            print("extracted length: ", token_nums, message_len)
            print("max: ", max_input_tokens, "system prompt: ", system_prompt_len, "initial response: ", initial_response_len, "fix prompt length: ", fix_prompt_len, "message: ", len(message))
            message = tokenizer.decode(message[:min(token_nums, message_len)])
            print("build new message:", system_prompt_len, min(token_nums, message_len), initial_response_len, fix_prompt_len)
            print("new message maximum length: ", max_input_tokens - system_prompt_len - min(token_nums, message_len) - initial_response_len - fix_prompt_len)
            messages = [
                {"role": "system", "content": conversation_history[0]["content"]},
                {"role": "user", "content": message},
                {"role": "assistant", "content": conversation_history[2]["content"]},
                {"role": "user", "content": fix_prompt}
            ]
        # print(messages[0], messages[2:])
        # print(messages)
    with torch.no_grad():
        print("start")
        output = model.chat(messages, sampling_params)[0]
        generated_text = output.outputs[0].text
        response["generated_text"] = generated_text
        print("length of generated text: ", len(tokenizer(generated_text).input_ids))
        # print(generated_text)
        print("end")

    time_taken = time.time() - start_time
    response["time_taken"] = time_taken
    if is_fix:
        response["prompt_id"] = prompt["prompt_id"]
    else:
        response["prompt_id"] = prompt["id"]
    response["original_code"] = prompt["original_code"]
    response["test_prompt"] = prompt["test_prompt"]

    if time_taken <= 60:
        time.sleep(60 - time_taken + 20)  # wait 5 seconds more to avoid rate limit
    return response




def run_with_timeout(command, timeout):
    """
    Run a command with a timeout. If the timeout is exceeded, kill the process.
    
    Args:
    - command: The command to execute as a list of arguments.
    - timeout: The timeout in seconds.
    """
    try:
        # Define the subprocess
        process = subprocess.Popen(
            command,
            text=True,
            # capture_output=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        
        # Use a timer to handle the timeout
        timer = threading.Timer(timeout, lambda: process.kill())
        try:
            timer.start()
            stdout, stderr = process.communicate()
            # If process completes in time, return the results
            returncode = process.returncode
            return returncode, stdout, stderr
        finally:
            timer.cancel()  # Cancel the timer if process completes
    except subprocess.TimeoutExpired:
        # Handle timeout explicitly (should not normally reach here due to the timer)
        print("this is time out error")
        return -1, None, None


def run_tests_and_collect_errors():
    """Run tests with coverage and return error messages."""
    try:
        # mvn clean test -Dmaven.test.failure.ignore=true jacoco:report
        # results = subprocess.run(["mvn", "clean", "test", "-Dmaven.test.failure.ignore=true", "jacoco:report"], text=True, capture_output=True)
        returncode, stdout, stderr = run_with_timeout(["mvn", "clean", "test", "-Dmaven.test.failure.ignore=true", "jacoco:report"], 30)
        # print(results.stderr + results.stdout)
        return stderr + stdout  # No errors
    except subprocess.CalledProcessError as e:
        print("Error", e)
        return e


def fix1(code):
    """
    delete "```javascript" at the begining, and "```" at the end
    """
    pattern = r"[\S\s.]*?\`\`\`java([\S\s.]*?)\`\`\`[\S\s.]*?"
    good_code = re.findall(pattern, code, re.DOTALL)
    code = []
    if len(good_code) > 0:
        # print("This is length: " , len(good_code))
        code.append(good_code[0])
        for i in range(1, len(good_code)):
            code.append(good_code[i])
    return code

def fix3(code):
    """
    delete "```" at the begining, and "```" at the end
    """
    pattern = r"[\S\s.]*?\`\`\`([\S\s.]*?)\`\`\`[\S\s.]*?"
    good_code = re.findall(pattern, code, re.DOTALL)
    code = []
    if len(good_code) > 0:
        # print("This is length: " , len(good_code))
        code.append(good_code[0])
        for i in range(1, len(good_code)):
            code.append(good_code[i])
    return code

def get_classname(code: str) -> str:
    """
    Gets the name of the CUT from the test prompt.
    @param code: the test prompt or the original code (it assumes it starts with `// classname.java`)
    @return: the classname of the CUT or the unit test to be generated
    """
    return code.split("\n")[0][3:-5].strip()


def extract_codes(response):
    old_test = response["generated_text"]
    # old_test = test_dict["generated_text"]
    function_file = response["prompt_id"].split('/')[-1].split('.')[0]
    test_classname = get_classname(response["test_prompt"].strip())
    if "```java" in old_test:
        old_test = fix1(old_test)
    else:
        old_test = fix3(old_test)
    return old_test



def save_output_to_file(filename, content):
    """Save given content to a file."""
    with open(filename, "w", encoding="utf-8") as file:
        file.write(content)


def save_initial_extracted_code(prompt: str, code: str, output_folder: str, isGPT3=False) -> None:
    filename = f"{prompt}.java"
    # create the output folder if needed
    if not os.path.exists(output_folder): os.makedirs(output_folder)

    with open(os.path.join(output_folder, filename), "w") as gen_file:
        if isGPT3:
            gen_file.write(code)
        else:
            # gen_file.write(prompt["test_prompt"] + "\n" + response['choices'][0]["text"])
            print("isGPT3 error")



def save_to_file(filename, content):
    """Save the given content to a .py file."""
    with open(filename, "w") as file:
        file.write(content)


def remove_namespace(doc):
    for elem in doc.getiterator():
        if 'ns0:' in elem.tag:
            elem.tag = elem.tag.replace('ns0:', '')
    return doc

def copy_files(src_folder, dest_folder):
    # Ensure source folder exists
    if not os.path.exists(src_folder):
        print(f"Source folder '{src_folder}' does not exist.")
        return
    
    # Ensure destination folder exists
    if not os.path.exists(dest_folder):
        print(f"Destination folder '{dest_folder}' does not exist.")
        return
    
    shutil.copytree(src_folder, dest_folder, dirs_exist_ok=True)


def convert_and_clean(text):
    # Convert text to lowercase
    text = text.lower()
    # Remove punctuation
    text = text.translate(str.maketrans('', '', string.punctuation))
    return text



def split_text_file(input_data, separator="package", output_dir="output_files", is_file_path=True, num = 0):
    """
    Splits the input (either a file or a text) into sections, cleans out lines starting with '#',
    and saves the sections into separate files.
    
    Parameters:
    - input_data: File path or text input
    - separator: Text used to split the input
    - output_dir: Directory where the output files will be saved
    - is_file_path: Boolean indicating if input_data is a file path
    """
    # Ensure the output directory exists
    os.makedirs(output_dir, exist_ok=True)
    
    # Read content from file or use the provided text
    if is_file_path:
        with open(input_data, 'r') as file:
            content = file.read()
        base_name = os.path.splitext(os.path.basename(input_data))[0]
    else:
        content = input_data
        base_name = f"test{num}"
    
    # Split the content while keeping the separator
    sections = content.split(separator)
    sections = [sections[0]]+[separator + section for section in sections[1:] if section]  # Re-add the separator
    
    # Process and save each section into a file
    for i, section in enumerate(sections):
        # Remove lines that start with "#"
        cleaned_section = "\n".join(line for line in section.splitlines() if not line.strip().startswith("#"))
        
        # Check if "public class " is in the section
        match = re.search(r'public class (\w+)', cleaned_section)
        if match:
            file_name = f"{match.group(1)}.java"  # Use the class name as the file name
        else:
            file_name = f"{base_name}_section_{i+1}.java"  # Default to base_name_section_<N>.txt
        
        output_file_path = os.path.join(output_dir, file_name)
        with open(output_file_path, 'w') as output_file:
            output_file.write(cleaned_section)
    
    print(f"Split text into {len(sections)} sections and saved to '{output_dir}' directory.")



def generate_tests(
    args,
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
    model_name,
) -> None:
    """
    Generates tests for the given scenario.
    @param config: dictionary of the parsed configuration
    @param dataset: the dataset associated with the prompt file
    @param prompts: a list of parsed prompts
    @param prompt_file: filename for the scenario (ex: "Scenario1_prompt.json")
    @param max_tokens: maximum number of tokens for generation
    """

    # sets the data output paths
    # print(max_tokens)
    output_folder_initial, scenario_folder_initial, response_file_initial, csv_file_initial = get_output_files_initial(
        config, dataset, prompt_file, max_tokens, gen_model, language
    )
    output_folder_fix, scenario_folder_fix, response_file_fix, csv_file_fix = get_output_files_fix(
        config, dataset, prompt_file, max_tokens, gen_model, language
    )
    original_code_folder = "./dataset/JAVA"
    generated_test_folder = f"./generated_tests/{gen_model}_Data/ProjectTestjava_output/original_fix"
    output_folder_initial = f"./javatest/{gen_model}_initial3/ProjectTest/JAVA/original_fix"
    output_folder_fix = f"./javatest/{gen_model}_fix3/ProjectTest/JAVA/original_fix"
    tests_output = json.load(open(f"./generated_tests/{gen_model}_Data/ProjectTestjava_output/original_output_{max_tokens}.json"))
    # opens output file in write mode (overwrite prior results)
    with open(response_file_fix, "w", encoding='utf-8') as json_file, open(csv_file_fix, "w", encoding='utf-8') as csv_out:
        csv_file_fix = csv.writer(
            csv_out, delimiter=",", quotechar='"', quoting=csv.QUOTE_MINIMAL
        )
        csv_file_fix.writerow(
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
                current_path = os.getcwd()
                print("current path is: ", current_path)
                initial_response = ""
                for test_dict in tests_output:
                    test_classname = test_dict["prompt_id"].split('/')[-1].split('.')[0]
                    print(test_classname, prompt["classname"])
                    if test_classname == prompt["classname"]:
                        initial_response = test_dict
                        print("type of initial response", type(test_dict))
                        break
                # print(initial_response)
                conversation_history = obtain_history(model_name, prompt, max_tokens, language, initial_response, tokenizer, max_input_tokens)
                full_path = os.path.join(original_code_folder, prompt["classname"])
                print("full_path: ", full_path)
                if os.path.exists(os.path.join(output_folder_initial, prompt["classname"])):
                    shutil.rmtree(os.path.join(output_folder_initial, prompt["classname"]))
                main_path = os.path.join(output_folder_initial, f"{convert_and_clean(gen_model)}_{prompt['classname']}/src/main/java/projecteval/{prompt['classname']}")
                os.makedirs(main_path, exist_ok=True)
                # print("full path: ", full_path)
                print("main path: ", main_path)
                copy_files(full_path, main_path)
                for file_name in os.listdir(generated_test_folder):
                    if file_name.startswith(prompt["classname"]):
                        source_file = os.path.join(generated_test_folder, file_name)
                        destination_path = os.path.join(output_folder_initial, f"{convert_and_clean(gen_model)}_{prompt['classname']}/src/test/java/projecteval/{prompt['classname']}")
                        os.makedirs(destination_path, exist_ok=True)
                        print("source file", source_file)
                        print("destination path", destination_path)
                        split_text_file(source_file, "package", destination_path)
                shutil.copy2(f"./javatest/{gen_model}/abcd_without/{convert_and_clean(gen_model)}_{prompt['classname']}/pom.xml", os.path.join(output_folder_initial, f"{convert_and_clean(gen_model)}_{prompt['classname']}"))
                print("Change current path to: ", os.path.join(output_folder_initial, f"{convert_and_clean(gen_model)}_{prompt['classname']}"))
                os.chdir(os.path.join(output_folder_initial, f"{convert_and_clean(gen_model)}_{prompt['classname']}"))
                # run coverage, obtain error messages, cd back
                print("run coverage")
                errors = run_tests_and_collect_errors()
                print("finish run coverage")
                os.chdir(current_path)
                output_filename = os.path.join(os.path.join(output_folder_initial, f"{convert_and_clean(gen_model)}_{prompt['classname']}"), "test_output.txt")
                save_output_to_file(output_filename, errors)
                
                # query Open AI to fix the errors from initial response
                print("fix code")
                response = fix_code(model, tokenizer, model_name, prompt, errors, conversation_history, initial_response['generated_text'], max_tokens, max_input_tokens, language, sampling_params)
                # extract codes from fixed response
                print("extract fix code")
                fixed_codes = extract_codes(response)
                # move project and test to ./pytest/GPT4_fix, cd to that path
                full_path = os.path.join(original_code_folder, prompt["classname"])
                if os.path.exists(os.path.join(output_folder_fix, prompt["classname"])):
                    shutil.rmtree(os.path.join(output_folder_fix, prompt["classname"]))
                main_path = os.path.join(output_folder_fix, f"{convert_and_clean(gen_model)}_{prompt['classname']}/src/main/java/projecteval/{prompt['classname']}")
                os.makedirs(main_path, exist_ok=True)
                copy_files(full_path, main_path)
                for i in range(len(fixed_codes)):
                    destination_path = os.path.join(output_folder_fix, f"{convert_and_clean(gen_model)}_{prompt['classname']}/src/test/java/projecteval/{prompt['classname']}")
                    os.makedirs(destination_path, exist_ok=True)
                    # print("destination path", destination_path)
                    split_text_file(fixed_codes[i], "package", destination_path, False, i)
                shutil.copy2(f"./javatest/{gen_model}/abcd_without/{convert_and_clean(gen_model)}_{prompt['classname']}/pom.xml", os.path.join(output_folder_fix, f"{convert_and_clean(gen_model)}_{prompt['classname']}"))
                print("Change current path again to: ", os.path.join(output_folder_fix, f"{convert_and_clean(gen_model)}_{prompt['classname']}"))
                os.chdir(os.path.join(output_folder_fix, f"{convert_and_clean(gen_model)}_{prompt['classname']}"))
                # run coverage, save outputs, cd back
                print("run coverage")
                outputs = run_tests_and_collect_errors()
                # run_coverage_report()
                os.chdir(current_path)
                # save outputs
                output_filename = os.path.join(os.path.join(output_folder_fix, f"{convert_and_clean(gen_model)}_{prompt['classname']}"), "test_output.txt")
                save_output_to_file(output_filename, outputs)
                # save the generated test in a file
                print("SAVING", prompt["id"], "at", scenario_folder_fix)
                save_generated_code(prompt, response, max_tokens, scenario_folder_fix, model_name, False, True)
                # save the response's metadata in CSV and JSON
                save_response(json_file, csv_file_fix, prompt, prompts, response, model_name, False, True)
                print(
                    "Duration: ",
                    response["time_taken"],
                    # "Finish Reason:",
                    # response["choices"][0]["finish_reason"],
                    "\n" + "-" * 30,
                )

            except Exception as e:
                print(e)
                print("ERROR", e)
                mock_response = get_mock_response(prompt, str(e))
                time.sleep(60)  # some sleep to make sure we don't go over rate limit
                # save_response(json_file, csv_file, prompt, prompts, mock_response)
            # break

        json_file.write("]")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "-t",
        "--tokens",
        type=int,
        # choices=[x * 1000 for x in range(1, 5)],
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
        "-m",
        "--model_name",
        type=str,
        help="model names used in model and tokenizer",
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
        choices=("CodeLlama", "StarCoder", "DeepSeek", "CodeGemma", "Qwen"),
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
    print(args)
    seed = 42
    torch.cuda.manual_seed_all(seed)

    tokenizer = AutoTokenizer.from_pretrained(args.model_name)
    # print("tokenizer ready")
    sampling_params = 0
    print(args.tokens)
    if args.tokens != -1:
        sampling_params = SamplingParams(temperature=OPENAI_TEMPERATURE, 
                                        top_p=OPENAI_TOP_P,
                                        max_tokens=args.tokens
                                        )
    else:
        sampling_params = SamplingParams(temperature=OPENAI_TEMPERATURE, 
                                        top_p=OPENAI_TOP_P,
                                        )

    model = LLM(
                args.model_name,
                # "google/codegemma-7b-it",
                tensor_parallel_size=args.num_gpus, 
                # max_model_len = 4096,
                )
    print("\n\n model max seq len is: ", tokenizer.model_max_length)
    config = load_config("config.json")
    print(args)
    # get list of parsed prompts from the JSON file
    prompts = get_prompts(config, args.prompts)

    print("Generating unit tests for", len(prompts), "prompts in", args.dataset)
    # generate unit tests
    generate_tests(args, config, args.dataset, args.prompts, prompts, args.tokens, args.language, args.gen_model, model, sampling_params, tokenizer, args.input_tokens, args.model_name)


if __name__ == "__main__":
    main()
