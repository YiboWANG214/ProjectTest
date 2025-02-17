import json
import os
import uuid

import openai


def load_config(config_file: str) -> dict:
    with open(config_file) as json_file:
        config = json.load(json_file)
    # sets the OpenAI key
    openai.api_key = config["OPEN_AI_KEY"]
    return config


def get_prompts(config: dict, prompt_file: str) -> list:
    scenario_path = os.path.join(config["BASE_DIRECTORY"], prompt_file)
    # print(scenario_path)
    with open(scenario_path, 'r') as scenario_file:
        prompts = json.load(scenario_file)
    return prompts


def get_output_files(config: dict, dataset: str, prompt_file: str, max_tokens: int, model: str, language: str) -> tuple:
    scenario_name = os.path.basename(prompt_file).split("_prompt")[0]
    output_folder = os.path.join(config["BASE_DIRECTORY"], f"{model}_Data", f"{dataset}{language}_output/")
    scenario_folder = os.path.join(output_folder, scenario_name)
    json_file = os.path.join(output_folder, f"{scenario_name}_output_{max_tokens}.json")
    csv_file = json_file.replace(".json", ".csv")
    return output_folder, scenario_folder, json_file, csv_file


def get_output_files_initial(config: dict, dataset: str, prompt_file: str, max_tokens: int, model: str, language: str) -> tuple:
    scenario_name = os.path.basename(prompt_file).split("_prompt")[0]
    output_folder = os.path.join(config["BASE_DIRECTORY"], f"{model}_initial3_Data", f"{dataset}{language}_output/")
    scenario_folder = os.path.join(output_folder, scenario_name)
    json_file = os.path.join(output_folder, f"{scenario_name}_output_{max_tokens}.json")
    csv_file = json_file.replace(".json", ".csv")
    return output_folder, scenario_folder, json_file, csv_file

def get_output_files_fix(config: dict, dataset: str, prompt_file: str, max_tokens: int, model: str, language: str) -> tuple:
    scenario_name = os.path.basename(prompt_file).split("_prompt")[0]
    output_folder = os.path.join(config["BASE_DIRECTORY"], f"{model}_fix3_Data", f"{dataset}{language}_output/")
    scenario_folder = os.path.join(output_folder, scenario_name)
    json_file = os.path.join(output_folder, f"{scenario_name}_output_{max_tokens}.json")
    csv_file = json_file.replace(".json", ".csv")
    return output_folder, scenario_folder, json_file, csv_file


def save_generated_code(prompt: dict, response: dict, max_tokens: int, output_folder: str, isGPT3=False, isOpenGemini=False) -> None:
    original_filename, extension = os.path.splitext(response['prompt_id'])
    original_filename = os.path.basename(original_filename)
    filename = f"{original_filename}T{max_tokens}_test{extension}"

    # create the output folder if needed
    if not os.path.exists(output_folder): os.makedirs(output_folder)

    with open(os.path.join(output_folder, filename), "w") as gen_file:
        if isGPT3:
            gen_file.write(response['choices'][0]["message"]["content"])
        elif isOpenGemini:
            gen_file.write(prompt["test_prompt"] + "\n" + response["generated_text"])
        else:
            gen_file.write(prompt["test_prompt"] + "\n" + response['choices'][0]["text"])


def save_response(json_file, csv_file, prompt: dict, prompts: list, response: dict, isGPT=False, isOpenGemini=False) -> None:
    json_file.write(json.dumps(response, indent=4))  # save immediately
    if prompt != prompts[-1]:
        json_file.write(",")
    json_file.write("\n")

    if isGPT:
        csv_file.writerow(
            [response['id'], response['prompt_id'], response['time_taken'],
             response["choices"][0]["finish_reason"],
             response["original_code"],
             response['test_prompt'],
             response['choices'][0]["message"]["content"]
             ])
    elif isOpenGemini:
        csv_file.writerow(
            [response['prompt_id'], response['time_taken'],
             response["original_code"],
             response['test_prompt'],
             prompt["test_prompt"] + "\n" + response["generated_text"]
             ])
    else:
        csv_file.writerow(
            [response['id'], response['prompt_id'], response['time_taken'],
             response["choices"][0]["finish_reason"],
             response["original_code"],
             response['test_prompt'],
             prompt["test_prompt"] + "\n" + response['choices'][0]["text"]
             ])


def get_mock_response(prompt: dict, error_msg: str) -> dict:
    return dict(
        choices=[{
            "finish_reason": "ERROR - " + error_msg,
            "text": ""
        }],
        id=str(uuid.uuid4()),  # generates a dummy ID
        prompt_id=prompt["id"],
        original_code=prompt["original_code"],
        test_prompt=prompt["test_prompt"],
        time_taken=-1,  # dummy
    )
 d

def save_to_dummy_folder(new_code: str, r: dict, suffix: int = 0) -> None:
    filename = r["prompt_id"][1:].replace("/", "_")
    if suffix > 0: filename = filename.replace(".java",f"_{suffix}.java")
    with open(f"./dummy_output/{filename}" , "w") as f:
        f.write(new_code)
