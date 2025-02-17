import os
import sys
import coverage
import pytest
import unittest
import re
from pathlib import Path
import importlib
import subprocess
import inspect

from collections.abc import Iterable

def is_iterable(element):
    """Determine if the given element is iterable."""
    try:
        iter(element)  # Try creating an iterator from the element
        return True
    except TypeError:
        return False


def run_coverage(test_name, project_path):
    """Run coverage for a specific test"""
    # Run tests using subprocess with `coverage run`
    run_name = test_name.split('.')[1]+'.py::'+test_name.split('.')[2]+'::'+test_name.split('.')[3]
    # print("run_coverage", test_name, '.'.join(test_name.split('.')[1:]), run_name)
    os.chdir(project_path)
    test_command = [
        "coverage",
        "run",
        "-m",
        "pytest",
        run_name,
    ]
    # print('Running command:', ' '.join(test_command))
    result = subprocess.run(test_command, text=True, capture_output=True)

    cov = coverage.Coverage(source=[project_path])
    cov.load()

    # Get the coverage data
    data = cov.get_data()

    # Get all files that were covered during this test run
    covered_files = data.measured_files()

    # Dictionary to store covered lines per file
    covered_lines = {}

    # Get the lines covered by this test for each file
    for filename in covered_files:
        if filename.endswith("test.py"):  # Skip test files
            continue
        relative_filename = os.path.relpath(filename, project_path)
        covered_lines[relative_filename] = set(data.lines(filename))

    cov.erase()

    return covered_lines


# Automatically discover all test functions
def get_test_functions(project_path):
    test_functions = []
    loader = unittest.TestLoader()
    # Load all the test cases from the project's test directory
    suite = loader.discover(start_dir=project_path, pattern='*_test.py')
    
    # Loop through each test suite and find test methods
    # print(suite)
    for test_suite in suite:
        for test_case in test_suite:
            # print(test_case)
            if not is_iterable(test_case):
                continue
            for test in test_case:
                # print(test)
                # Get the full test function name, including the class
                test_functions.append(f"{test.__class__.__name__}.{test._testMethodName}")
    return test_functions



def count_total_lines_of_code(project_path):
    """Count the total number of non-empty lines of code in all .py files within the project, excluding comments, strings,
    and counting multi-line function headers, dictionaries, lists, and expressions as one line."""
    total_lines = 0
    multi_line_string = False   # To track if we are within a multi-line string
    in_multiline_def = False    # To track if we are inside a multi-line function definition
    in_multiline_collection = False  # To track if we are inside a multi-line dictionary or list
    in_multiline_expression = False  # To track if we are inside a multi-line expression (like mathematical expressions)

    for dirpath, dirnames, filenames in os.walk(project_path):
        for filename in filenames:
            if filename.endswith("test.py"):  # Skip test files
                continue
            if filename.endswith('.py'):
                filepath = os.path.join(dirpath, filename)
                with open(filepath, 'r', encoding='utf-8') as file:
                    for line in file:
                        stripped_line = line.strip()

                        # Handle multi-line strings
                        if multi_line_string:
                            if "'''" in stripped_line or '"""' in stripped_line:
                                multi_line_string = False
                            continue  # Skip the entire line
                        elif stripped_line.startswith("'''") or stripped_line.startswith('"""'):
                            multi_line_string = True
                            if stripped_line.count("'''") > 1 or stripped_line.count('"""') > 1:
                                multi_line_string = False
                            continue  # Skip the entire line

                        # Handle function headers that span multiple lines
                        if in_multiline_def:
                            if stripped_line.endswith('):'):
                                in_multiline_def = False
                            continue  # Ignore other lines of the function definition
                        if stripped_line.startswith('def ') and not stripped_line.endswith('):'):
                            in_multiline_def = True  # Detect function header spanning multiple lines
                            total_lines += 1
                            continue  # Count this def as one line, regardless of its span

                        # Handle multi-line dictionary or list assignments
                        if in_multiline_collection:
                            if stripped_line.endswith('}') or stripped_line.endswith(']'):
                                in_multiline_collection = False
                            continue  # Ignore lines inside the multi-line collection
                        if stripped_line.endswith('{') or stripped_line.endswith('['):
                            in_multiline_collection = True
                            total_lines += 1  # Count this collection assignment as one line
                            continue

                        # Handle multi-line expressions (such as mathematical or logical expressions)
                        if in_multiline_expression:
                            if stripped_line.endswith((')', '}', ']')) or stripped_line.endswith(';'):
                                in_multiline_expression = False
                            continue  # Ignore other lines in the multi-line expression
                        if stripped_line.endswith(('=', '+', '-', '/', '*', '(', 'and', 'or')):
                            in_multiline_expression = True
                            total_lines += 1  # Count this expression as one line
                            continue

                        # Handle single-line comments
                        if stripped_line.startswith('#'):
                            continue
                        if stripped_line.startswith('@'):  # Decorators
                            continue

                        # Count the line if it is not empty and not part of a multi-line def or comment
                        if stripped_line:
                            total_lines += 1

            # print(filename, total_lines)

    return total_lines


def main(project_path, dic):
    # Define the path to the test directory within the project
    test_path = os.path.join(project_path)  # Adjust this to your test folder location
    project_name = project_path.split("/")[-1]

    # Add project path to the system path to access the source code and tests
    sys.path.insert(0, "/".join(project_path.split("/")[:-1]))

    # Run all tests to get the full coverage
    os.chdir(project_path)
    test_command = [
        "coverage",
        "run",
        "-m",
        "pytest",
    ]
    result = subprocess.run(test_command, text=True, capture_output=True)

    cov = coverage.Coverage(source=[project_path])
    cov.load()

    # Get the coverage data
    data = cov.get_data()

    # Get all files that were covered during the full test run
    covered_files = data.measured_files()

    # Dictionary to store full coverage per file
    full_coverage = {}

    # Get all lines covered by the full test suite for each file
    total_test_lines = 0
    for filename in covered_files:
        if filename.endswith("test.py"):
            continue
        relative_filename = os.path.relpath(filename, project_path)
        full_coverage[relative_filename] = set(data.lines(filename))
        total_test_lines += len(set(data.lines(filename)))
    # Get the test functions automatically from the test directory
    test_functions = get_test_functions(test_path)

    # Dictionary to store unique coverage contribution for each test
    unique_coverage = {}

    # print(test_functions)
    all_test_lines = {}
    for test_func in test_functions:
        test_func = f"{project_name}.{project_name}_test."+test_func
        # print(test_func)
        test_coverage = run_coverage(test_func, project_path)
        all_test_lines[test_func] = test_coverage
    # print(all_test_lines)
    
    for test_func in all_test_lines.keys():
        test_coverage = all_test_lines[test_func]
        # Dictionary to hold unique contribution for each file
        unique_contribution = {}

        # Compare the coverage for each file
        for filename, lines in test_coverage.items():
            if filename.endswith("test.py"):
                continue
            # Unique contribution is the lines covered by this test but not by any other tests
            newlines = set()
            for other_test_func in all_test_lines.keys():
                if test_func != other_test_func:
                    newlines.update(all_test_lines[other_test_func][filename])
            if filename in full_coverage:
                unique_contribution[filename] = lines - newlines
            else:
                unique_contribution[filename] = lines
            # print(filename, unique_contribution[filename])
        
        unique_coverage[test_func] = unique_contribution
        # break

    # Print unique contributions for each test function and file
    total_unique_lines = 0
    for test_func, contribution in unique_coverage.items():
        # print(f"Unique lines covered by {test_func}:")
        for filename, lines in contribution.items():
            # print(f"  {filename}: {sorted(list(lines))}")
            total_unique_lines += len(list(lines))
    print("\n===================\n")
    print(project_name)
    print(f"Total unique lines of code: {total_unique_lines}")
    print(f"Total covered lines of code: {total_test_lines}")

    # total_lines = count_total_lines_of_code(project_path)
    total_lines = dic[project_name]
    print(f"Total non-empty lines of code in the project: {total_lines}")
    
    print(f"Unique contribution rate is {total_unique_lines/total_lines}")
    print(f"Coverage rate is {total_test_lines/total_lines}")

    cov.erase()

if __name__ == "__main__":
    dic = {"blackjack": 192,
            "bridge": 526,
            "doudizhu": 634,
            "fuzzywuzzy": 329,
            "gin_rummy": 944,
            "keras_preprocessing": 352,
            "leducholdem": 364,
            "limitholdem": 584,
            "mahjong": 377,
            "nolimitholdem": 733,
            "slugify": 109,
            "stock": 141,
            "stock2": 237,
            "stock3": 183,
            "stock4": 218,
            "structly": 242,
            "svm": 135,
            "thefuzz": 38,
            "tree": 125,
            "uno": 351,
            }
    # Provide the path to the project containing source code and tests
    root_dir = "./pytest/Gemini/ProjectTest/Python/original_fix"
    root_path = Path(root_dir)
    for dirpath in root_path.iterdir():
        if dirpath.is_dir():
            # print("This is dirpath:", dirpath)
            project_path = str(dirpath)
            name = project_path.split('/')[-1]
            print(name)
            main(project_path, dic)
        # break
 